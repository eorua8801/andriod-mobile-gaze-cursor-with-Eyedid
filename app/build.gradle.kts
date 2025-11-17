import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.eorua.gazecursor"
    compileSdk = 35  // ✅ 34 → 35로 변경
    ndkVersion = "28.0.12674087"  // NDK r28+ for 16KB page size support
    
    // local.properties에서 프로덕션 라이센스 키 및 키스토어 정보 읽기
    val localProperties = Properties()
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { localProperties.load(it) }
    }
    val prodLicenseKey = localProperties.getProperty("EYEDID_PRODUCTION_KEY") 
        ?: "YOUR_PRODUCTION_LICENSE_KEY_HERE"
    
    // 키스토어 정보
    val keystoreFilePath = localProperties.getProperty("KEYSTORE_FILE")
    val keystorePwd = localProperties.getProperty("KEYSTORE_PASSWORD")
    val keyAliasName = localProperties.getProperty("KEY_ALIAS")
    val keyPwd = localProperties.getProperty("KEY_PASSWORD")

    defaultConfig {
        applicationId = "com.eorua.gazecursor"
        minSdk = 29
        targetSdk = 35  // ✅ 34 → 35로 변경
        versionCode = 8  // ✅ 16KB 페이지 크기 지원 업데이트 (NDK r28+ 포함)
        versionName = "1.0"  // 또는 "1.1"로 변경 가능

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 🔒 보안: 프로덕션 라이센스 키 사용 (local.properties에서 로드)
        buildConfigField("String", "EYEDID_LICENSE_KEY", "\"$prodLicenseKey\"")
        buildConfigField("String", "API_BASE_URL", "\"https://api.eyedid.ai/v1/\"")

        // 16KB 페이지 크기 지원: 네이티브 라이브러리 아키텍처 명시
        ndk {
            abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a"))
        }
    }

    // 서명 설정
    signingConfigs {
        create("release") {
            if (keystoreFilePath != null && keystorePwd != null && keyAliasName != null && keyPwd != null) {
                storeFile = file(keystoreFilePath)
                storePassword = keystorePwd
                keyAlias = keyAliasName
                keyPassword = keyPwd
            }
        }
    }
    
    buildTypes {
        debug {
            // 🔧 디버그 모드 설정 - 프로덕션 키 테스트용
            buildConfigField("String", "EYEDID_LICENSE_KEY", "\"$prodLicenseKey\"")
            buildConfigField("String", "API_BASE_URL", "\"https://api.eyedid.ai/v1/\"")
        }
        
        release {
            // 코드 난독화 및 최적화 활성화
            isMinifyEnabled = true
            isShrinkResources = true
            
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            
            // 🔒 프로덕션 라이센스 키 (local.properties에서 로드)
            buildConfigField("String", "EYEDID_LICENSE_KEY", "\"$prodLicenseKey\"")
            buildConfigField("String", "API_BASE_URL", "\"https://api.eyedid.ai/v1/\"")
            
            // 🔒 릴리즈 서명
            // 키스토어가 있으면 릴리즈 키 사용, 없으면 디버그 키 사용 (테스트용)
            signingConfig = if (keystoreFilePath != null) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")  // 테스트용 폴백
            }
        }
    }
    
    // 🔧 BuildConfig 기능 활성화
    buildFeatures {
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    // 🔧 패키징 옵션 (라이브러리 충돌 방지)
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        // 16KB 페이지 크기 지원: 네이티브 라이브러리 압축 해제 및 정렬
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.eyedid.gazetracker)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
