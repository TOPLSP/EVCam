package com.kooo.evcam.camera;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.util.Log;

/**
 * 摄像头模式选择器
 * 根据设备API级别和Camera2支持程度自动选择最佳方案
 */
public class CameraModeSelector {
    private static final String TAG = "CameraModeSelector";
    
    public enum CameraMode {
        CAMERA2_FULL,      // Camera2完整模式（API 23+且FULL级别）
        CAMERA2_LIMITED,   // Camera2受限模式（API 21+ LIMITED级别）
        CAMERA2_LEGACY,    // Camera2兼容模式（API 21+ LEGACY级别）
        CAMERA1_ONLY       // Camera1唯一模式（API < 21 或 LEGACY设备）
    }
    
    private CameraMode mSelectedMode;
    private boolean mIsMultiCameraSupported;
    
    public CameraModeSelector(Context context) {
        selectMode(context);
    }
    
    private void selectMode(Context context) {
        int sdkVersion = Build.VERSION.SDK_INT;
        
        if (sdkVersion < Build.VERSION_CODES.LOLLIPOP) {
            // API < 21 只能使用Camera1
            mSelectedMode = CameraMode.CAMERA1_ONLY;
            mIsMultiCameraSupported = false;
            Log.d(TAG, "Selected mode: CAMERA1_ONLY (API " + sdkVersion + ")");
            return;
        }
        
        // API 21+ 检查Camera2支持程度
        CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        int fullCount = 0;
        int limitedCount = 0;
        int legacyCount = 0;
        int totalCount = 0;
        
        try {
            String[] cameraIds = manager.getCameraIdList();
            totalCount = cameraIds.length;
            
            for (String cameraId : cameraIds) {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
                Integer level = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
                
                if (level == null) {
                    legacyCount++;
                    continue;
                }
                
                switch (level) {
                    case CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL:
                        fullCount++;
                        break;
                    case CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED:
                        limitedCount++;
                        break;
                    case CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY:
                    default:
                        legacyCount++;
                        break;
                }
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, "Failed to access camera", e);
            mSelectedMode = CameraMode.CAMERA1_ONLY;
            mIsMultiCameraSupported = false;
            return;
        }
        
        Log.d(TAG, "Camera support - Full: " + fullCount + ", Limited: " + limitedCount + 
              ", Legacy: " + legacyCount + ", Total: " + totalCount);
        
        // 决策逻辑
        if (sdkVersion >= Build.VERSION_CODES.M && fullCount >= 2) {
            // API 23+ 且有至少2个FULL级别摄像头
            mSelectedMode = CameraMode.CAMERA2_FULL;
            mIsMultiCameraSupported = true;
        } else if (sdkVersion >= Build.VERSION_CODES.M && fullCount >= 1) {
            // API 23+ 但只有1个FULL级别
            mSelectedMode = CameraMode.CAMERA2_FULL;
            mIsMultiCameraSupported = false;
        } else if (limitedCount > 0 && sdkVersion >= Build.VERSION_CODES.LOLLIPOP) {
            // API 21+ 有LIMITED级别
            mSelectedMode = CameraMode.CAMERA2_LIMITED;
            mIsMultiCameraSupported = false; // LIMITED通常不支持多路并发
        } else if (sdkVersion >= Build.VERSION_CODES.LOLLIPOP) {
            // API 21+ 但只有LEGACY级别
            mSelectedMode = CameraMode.CAMERA2_LEGACY;
            mIsMultiCameraSupported = false;
        } else {
            mSelectedMode = CameraMode.CAMERA1_ONLY;
            mIsMultiCameraSupported = false;
        }
        
        Log.d(TAG, "Selected mode: " + mSelectedMode + ", Multi-camera: " + mIsMultiCameraSupported);
    }
    
    public CameraMode getMode() {
        return mSelectedMode;
    }
    
    public boolean isMultiCameraSupported() {
        return mIsMultiCameraSupported;
    }
    
    public boolean useCamera2() {
        return mSelectedMode != CameraMode.CAMERA1_ONLY;
    }
    
    public boolean isLegacyMode() {
        return mSelectedMode == CameraMode.CAMERA2_LEGACY || mSelectedMode == CameraMode.CAMERA1_ONLY;
    }
    
    /**
     * 获取建议的摄像头数量
     */
    public int getRecommendedCameraCount() {
        if (mIsMultiCameraSupported) {
            return 4; // 支持多路，建议4路
        } else {
            return 1; // 不支持多路，建议1路
        }
    }
    
    /**
     * 获取建议的最大分辨率
     */
    public String getRecommendedResolution() {
        switch (mSelectedMode) {
            case CAMERA2_FULL:
                return "1920x1080";
            case CAMERA2_LIMITED:
                return "1280x720";
            case CAMERA2_LEGACY:
            case CAMERA1_ONLY:
            default:
                return "1280x720"; // 保守配置
        }
    }
    
    /**
     * 获取建议的最大帧率
     */
    public int getRecommendedFps() {
        switch (mSelectedMode) {
            case CAMERA2_FULL:
                return 30;
            case CAMERA2_LIMITED:
                return 20;
            case CAMERA2_LEGACY:
            case CAMERA1_ONLY:
            default:
                return 15; // 保守配置
        }
    }
}
