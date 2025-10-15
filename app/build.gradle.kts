plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.eorua.gazecursor"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.eorua.gazecursor"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // 🔒 보안: 라이센스 키와 API URL 설정
        buildConfigField("String", "EYEDID_LICENSE_KEY", "\"dev_gcgccetiewv85wcwdgzyuyhhy1k020w69mg92dnn\"")
        buildConfigField("String", "API_BASE_URL", "\"https://api.eyedid.ai/v1/\"")
    }

    buildTypes {
        debug {
            // 🔧 디버그 모드 설정
            buildConfigField("String", "EYEDID_LICENSE_KEY", "\"dev_gcgccetiewv85wcwdgzyuyhhy1k020w69mg92dnn\"")
            buildConfigField("String", "API_BASE_URL", "\"https://dev-api.eyedid.ai/v1/\"")
        }
        
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            
            // 🔒 릴리즈 모드: 실제 라이센스 키로 교체 필요
            buildConfigField("String", "EYEDID_LICENSE_KEY", "\"YOUR_PRODUCTION_LICENSE_KEY_HERE\"")
            buildConfigField("String", "API_BASE_URL", "\"https://api.eyedid.ai/v1/\"")
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
