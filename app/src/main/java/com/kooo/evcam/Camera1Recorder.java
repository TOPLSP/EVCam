package com.kooo.evcam.camera;

import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Android 5.0 Camera1 API 录制器（兼容模式）
 * 用于API 21-22设备，或Camera2 LEGACY模式的设备
 */
public class Camera1Recorder implements VideoRecorder {
    private static final String TAG = "Camera1Recorder";
    
    private Camera mCamera;
    private MediaRecorder mMediaRecorder;
    private int mCameraId;
    private boolean mIsRecording = false;
    private File mOutputFile;
    private RecordCallback mCallback;
    private SurfaceHolder mHolder;
    
    // 车机性能限制，使用保守配置
    private static final int MAX_WIDTH = 1280;
    private static final int MAX_HEIGHT = 720;
    private static final int MAX_FPS = 30;
    
    public Camera1Recorder(int cameraId) {
        this.mCameraId = cameraId;
    }
    
    @Override
    public void prepare(Surface surface, File outputFile, RecordCallback callback) throws IOException {
        this.mOutputFile = outputFile;
        this.mCallback = callback;
        
        // 如果surface是SurfaceHolder类型，保存它
        if (surface instanceof SurfaceHolder) {
            this.mHolder = (SurfaceHolder) surface;
        }
        
        // 打开摄像头
        try {
            mCamera = Camera.open(mCameraId);
            if (mCamera == null) {
                throw new IOException("Failed to open camera " + mCameraId);
            }
        } catch (RuntimeException e) {
            throw new IOException("Camera " + mCameraId + " is already in use or unavailable: " + e.getMessage());
        }
        
        // 配置参数
        Camera.Parameters params = mCamera.getParameters();
        
        // 设置预览分辨率
        Camera.Size previewSize = getOptimalSize(params.getSupportedPreviewSizes(), MAX_WIDTH, MAX_HEIGHT);
        if (previewSize != null) {
            params.setPreviewSize(previewSize.width, previewSize.height);
            Log.d(TAG, "Camera " + mCameraId + " preview size: " + previewSize.width + "x" + previewSize.height);
        }
        
        // 设置录制分辨率（如果支持）
        List<Camera.Size> videoSizes = params.getSupportedVideoSizes();
        if (videoSizes != null && !videoSizes.isEmpty()) {
            Camera.Size videoSize = getOptimalSize(videoSizes, MAX_WIDTH, MAX_HEIGHT);
            if (videoSize != null) {
                Log.d(TAG, "Camera " + mCameraId + " video size: " + videoSize.width + "x" + videoSize.height);
            }
        }
        
        // 设置帧率
        List<int[]> fpsRanges = params.getSupportedPreviewFpsRange();
        if (fpsRanges != null && !fpsRanges.isEmpty()) {
            int[] bestRange = fpsRanges.get(0);
            for (int[] range : fpsRanges) {
                if (range[1] <= MAX_FPS * 1000 && range[1] > bestRange[1]) {
                    bestRange = range;
                }
            }
            params.setPreviewFpsRange(bestRange[0], bestRange[1]);
            Log.d(TAG, "Camera " + mCameraId + " FPS range: " + bestRange[0] + "-" + bestRange[1]);
        }
        
        // 设置对焦模式
        List<String> focusModes = params.getSupportedFocusModes();
        if (focusModes != null) {
            if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            } else if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            }
        }
        
        // 设置场景模式
        List<String> sceneModes = params.getSupportedSceneModes();
        if (sceneModes != null && sceneModes.contains(Camera.Parameters.SCENE_MODE_AUTO)) {
            params.setSceneMode(Camera.Parameters.SCENE_MODE_AUTO);
        }
        
        mCamera.setParameters(params);
        
        // 设置预览显示
        if (mHolder != null) {
            mCamera.setPreviewDisplay(mHolder);
        }
        
        // 设置方向（车机通常是横屏，需要测试调整）
        try {
            mCamera.setDisplayOrientation(0); // 根据车机实际方向调整
        } catch (Exception e) {
            Log.w(TAG, "Failed to set display orientation", e);
        }
        
        mCamera.startPreview();
        Log.d(TAG, "Camera " + mCameraId + " prepared successfully");
    }
    
    @Override
    public void start() throws IOException {
        if (mIsRecording) {
            Log.w(TAG, "Already recording");
            return;
        }
        
        if (mCamera == null) {
            throw new IOException("Camera not prepared");
        }
        
        try {
            mMediaRecorder = new MediaRecorder();
            
            // 解锁摄像头供MediaRecorder使用
            mCamera.unlock();
            mMediaRecorder.setCamera(mCamera);
            
            // 配置录制源
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
            
            // 使用CamcorderProfile（最兼容的方式）
            CamcorderProfile profile = null;
            if (CamcorderProfile.hasProfile(mCameraId, CamcorderProfile.QUALITY_720P)) {
                profile = CamcorderProfile.get(mCameraId, CamcorderProfile.QUALITY_720P);
                Log.d(TAG, "Using QUALITY_720P profile");
            } else if (CamcorderProfile.hasProfile(mCameraId, CamcorderProfile.QUALITY_480P)) {
                profile = CamcorderProfile.get(mCameraId, CamcorderProfile.QUALITY_480P);
                Log.d(TAG, "Using QUALITY_480P profile");
            } else {
                profile = CamcorderProfile.get(mCameraId, CamcorderProfile.QUALITY_LOW);
                Log.d(TAG, "Using QUALITY_LOW profile");
            }
            
            if (profile != null) {
                // 降低帧率以保证稳定性
                profile.videoFrameRate = Math.min(profile.videoFrameRate, MAX_FPS);
                mMediaRecorder.setProfile(profile);
            } else {
                // 手动配置
                mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
                mMediaRecorder.setVideoSize(1280, 720);
                mMediaRecorder.setVideoFrameRate(MAX_FPS);
                mMediaRecorder.setVideoEncodingBitRate(4000000); // 4Mbps
            }
            
            // 设置输出文件
            mMediaRecorder.setOutputFile(mOutputFile.getAbsolutePath());
            
            // 设置预览（可选，某些设备需要）
            if (mHolder != null) {
                mMediaRecorder.setPreviewDisplay(mHolder.getSurface());
            }
            
            // 准备和开始
            mMediaRecorder.prepare();
            mMediaRecorder.start();
            mIsRecording = true;
            
            Log.d(TAG, "Recording started: " + mOutputFile.getAbsolutePath());
            
            if (mCallback != null) {
                mCallback.onRecordStarted();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to start recording", e);
            releaseMediaRecorder();
            throw new IOException("Start recording failed: " + e.getMessage());
        }
    }
    
    @Override
    public void stop() {
        if (!mIsRecording) {
            return;
        }
        
        try {
            mMediaRecorder.stop();
            Log.d(TAG, "Recording stopped successfully");
        } catch (RuntimeException e) {
            // stop()失败时，文件可能损坏，删除它
            Log.e(TAG, "RuntimeException during stop", e);
            if (mOutputFile.exists()) {
                mOutputFile.delete();
            }
        } finally {
            releaseMediaRecorder();
            releaseCamera();
            mIsRecording = false;
        }
        
        if (mCallback != null && mOutputFile.exists()) {
            mCallback.onRecordStopped(mOutputFile);
        }
    }
    
    @Override
    public boolean isRecording() {
        return mIsRecording;
    }
    
    @Override
    public void release() {
        stop();
        releaseMediaRecorder();
        releaseCamera();
    }
    
    private void releaseMediaRecorder() {
        if (mMediaRecorder != null) {
            try {
                mMediaRecorder.reset();
                mMediaRecorder.release();
            } catch (Exception e) {
                Log.e(TAG, "Error releasing MediaRecorder", e);
            }
            mMediaRecorder = null;
        }
    }
    
    private void releaseCamera() {
        if (mCamera != null) {
            try {
                mCamera.lock();
                mCamera.stopPreview();
                mCamera.release();
            } catch (Exception e) {
                Log.e(TAG, "Error releasing camera", e);
            }
            mCamera = null;
        }
    }
    
    /**
     * 获取最佳尺寸（最接近目标宽高比和分辨率）
     */
    private Camera.Size getOptimalSize(List<Camera.Size> sizes, int targetWidth, int targetHeight) {
        if (sizes == null || sizes.isEmpty()) {
            return null;
        }
        
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) targetWidth / targetHeight;
        
        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;
        
        // 首先寻找精确匹配
        for (Camera.Size size : sizes) {
            if (size.width == targetWidth && size.height == targetHeight) {
                return size;
            }
        }
        
        // 然后寻找最接近的宽高比和分辨率
        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            
            if (Math.abs(size.height - targetHeight) < minDiff && size.width <= targetWidth) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }
        
        // 如果没找到，忽略宽高比，只找最接近的分辨率
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff && size.width <= targetWidth) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        
        // 如果还是没找到，使用第一个（通常是最大或最小）
        if (optimalSize == null) {
            optimalSize = sizes.get(0);
        }
        
        return optimalSize;
    }
}
