plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.kooo.evcam"
    compileSdk = 34

    // 签名配置 (使用 AOSP 公共测试签名)
    signingConfigs {
        create("release") {
            storeFile = file("../keystore/release.jks")
            storePassword = "android"
            keyAlias = "apkeasytool"
            keyPassword = "android"
        }
    }

    defaultConfig {
        applicationId = "com.kooo.evcam"
        minSdk = 21
        targetSdk = 34
        versionCode = 19
        versionName = "1.0.6"

        // 启用 multidex 支持（API 21需要）
        multiDexEnabled = true

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // 限制ABI，提高车机兼容性
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a")
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isDebuggable = true
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
        // 关键：启用 desugaring 支持 API 21 的 Java 8 特性
        isCoreLibraryDesugaringEnabled = true
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
    
    packagingOptions {
        resources {
            excludes += listOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt"
            )
        }
        pickFirsts += listOf(
            "lib/armeabi-v7a/libc++_shared.so",
            "lib/arm64-v8a/libc++_shared.so"
        )
    }
    
    lint {
        disable += listOf("InvalidPackage", "OldTargetApi")
        abortOnError = false
    }

    sourceSets {
        getByName("main") {
            assets.srcDir("../assets")
        }
    }
}

dependencies {
    // AndroidX 基础库
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.recyclerview)
    implementation(libs.cardview)
    implementation(libs.core.ktx)
    
    // Android 5.0 适配关键库
    implementation(libs.multidex)
    implementation(libs.workmanager)
    // 使用 toml 中的 lifecycle-runtime，删除硬编码的重复依赖
    // implementation(libs.lifecycle.runtime) // 如需具体版本控制可保留
    
    // Java 8+ 脱糖支持
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    
    // 关键：Conscrypt 提供现代 TLS 支持（Android 5.0 必需）
    implementation(libs.conscrypt.android)

    // 钉钉官方 Stream SDK（排除冲突依赖，防止强制使用 OkHttp 4.x）
    implementation("com.dingtalk.open:app-stream-client:1.3.12") {
        exclude(group = "com.squareup.okhttp3")
        exclude(group = "com.squareup.okio")
    }

    // 网络请求 - 3.12.13 是最后一个稳定支持 API 21 的版本
    implementation("com.squareup.okhttp3:okhttp:3.12.13")
    implementation("com.squareup.okhttp3:logging-interceptor:3.12.13")

    // JSON 解析
    implementation("com.google.code.gson:gson:2.10.1")

    // ZXing 二维码生成
    implementation("com.google.zxing:core:3.5.1")

    // Glide 图片加载库
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // 生命周期支持（车机保活）- 删除重复，只保留这组或 toml 中的那组
    implementation("androidx.lifecycle:lifecycle-service:2.6.2")
    implementation("androidx.lifecycle:lifecycle-process:2.6.2")
    
    // 本地广播支持
    implementation("androidx.localbroadcastmanager:localbroadcastmanager:1.1.0")

    // 测试库
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
