# 🚀 플레이스토어 배포 체크리스트

## ✅ 완료된 작업
- [x] AppConstants.java 라이센스 키 BuildConfig로 변경
- [x] build.gradle.kts에 프로덕션 라이센스 키 설정 추가
- [x] ProGuard 규칙 추가 (EyeDID SDK, 접근성 서비스 등)
- [x] 코드 난독화 및 리소스 축소 활성화

---

## 📋 필수 작업 리스트

### 1. 🔑 프로덕션 라이센스 키 설정

#### Step 1: 프로덕션 라이센스 키 발급
비주얼 캠프(Visual Camp)에 문의하여 **프로덕션 라이센스 키** 발급:
- 개발용 키: `dev_gcgccetiewv85wcwdgzyuyhhy1k020w69mg92dnn`
- 프로덕션 키: **발급 필요**

#### Step 2: local.properties 파일에 키 추가
프로젝트 루트의 `local.properties` 파일에 추가:
```properties
EYEDID_PRODUCTION_KEY=발급받은_프로덕션_라이센스_키
```

> ⚠️ **중요**: `local.properties` 파일은 Git에 커밋되지 않으므로 안전합니다!

#### Step 3: 라이센스 키 테스트
```bash
# Release 빌드로 테스트
./gradlew assembleRelease

# APK 설치 및 작동 확인
adb install -r app/build/outputs/apk/release/app-release.apk
```

---

### 2. 📱 앱 버전 관리

`app/build.gradle.kts` 파일에서:
```kotlin
defaultConfig {
    versionCode = 1  // 매 업데이트마다 1씩 증가
    versionName = "1.0"  // 사용자에게 보이는 버전
}
```

**버전 관리 규칙:**
- `versionCode`: 정수, 매 릴리즈마다 1씩 증가 (필수!)
- `versionName`: "Major.Minor.Patch" 형식 (예: 1.0.0)

---

### 3. 🔐 APK 서명 설정

#### Step 1: Keystore 생성 (처음 한 번만)
```bash
keytool -genkey -v -keystore gazecursor-release.keystore -alias gazecursor -keyalg RSA -keysize 2048 -validity 10000
```

**입력할 정보:**
- 비밀번호: (안전하게 보관!)
- 이름, 조직, 도시, 국가 등

#### Step 2: Keystore 정보를 local.properties에 추가
```properties
KEYSTORE_FILE=../gazecursor-release.keystore
KEYSTORE_PASSWORD=키스토어_비밀번호
KEY_ALIAS=gazecursor
KEY_PASSWORD=키_비밀번호
```

#### Step 3: build.gradle.kts에 서명 설정 추가
`app/build.gradle.kts` 파일의 `android` 블록 안에 추가:
```kotlin
android {
    // ... 기존 코드 ...
    
    // Keystore 정보 로드
    val keystoreFile = localProperties.getProperty("KEYSTORE_FILE")
    val keystorePassword = localProperties.getProperty("KEYSTORE_PASSWORD")
    val keyAlias = localProperties.getProperty("KEY_ALIAS")
    val keyPassword = localProperties.getProperty("KEY_PASSWORD")
    
    signingConfigs {
        create("release") {
            storeFile = keystoreFile?.let { file(it) }
            storePassword = keystorePassword
            keyAlias = keyAlias
            keyPassword = keyPassword
        }
    }
    
    buildTypes {
        release {
            // ... 기존 코드 ...
            signingConfig = signingConfigs.getByName("release")  // 이 줄의 주석 해제
        }
    }
}
```

---

### 4. 📝 플레이스토어 메타데이터 준비

#### 앱 설명 (한국어)
```
🎯 시선만으로 스마트폰을 조작하는 접근성 솔루션

손을 자유롭게 사용하기 어려운 분들을 위한 혁신적인 시선 추적 커서 앱입니다.

주요 기능:
• 시선만으로 커서 제어
• 1초 응시로 자동 클릭
• 화면 모서리 응시로 스크롤/메뉴
• 안경 착용자 전용 보정
• 배터리 최적화 지원

기존 Camera Switches 대비 70% 빠른 반응속도!
```

#### 필요한 스크린샷
1. 메인 화면 (권한 설정 완료 후)
2. 시선 커서 사용 중 화면 (실제 커서 표시)
3. 캘리브레이션 화면
4. 설정 화면
5. 엣지 메뉴 사용 예시

📸 스크린샷 규격:
- 세로: 1920x1080 이상
- 가로: 1080x1920 (필요 시)
- PNG 또는 JPEG 형식

#### 앱 아이콘
- 현재 아이콘 확인: `app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp`
- 플레이스토어용 고해상도 아이콘 (512x512) 준비 필요

#### 개인정보 처리방침
- 플레이스토어 등록 시 필수!
- 카메라 사용 목적 명시
- 수집하는 데이터 명시
- 웹페이지 또는 텍스트 문서로 준비

---

### 5. 🧪 최종 테스트

#### 릴리즈 빌드 생성
```bash
./gradlew assembleRelease
```

#### 테스트 체크리스트
- [ ] 프로덕션 라이센스 키로 정상 작동
- [ ] 모든 권한 요청 정상 작동
- [ ] 캘리브레이션 정상 작동
- [ ] 시선 커서 정상 작동
- [ ] 클릭 감지 정상 작동
- [ ] 엣지 스크롤/메뉴 정상 작동
- [ ] 접근성 서비스 정상 작동
- [ ] 앱 종료 후 재시작 정상 작동
- [ ] 배터리 최적화 정상 작동
- [ ] 다양한 기기에서 테스트 (갤럭시 등)

#### 테스트 기기 목록
- [ ] Galaxy S 시리즈
- [ ] Galaxy A 시리즈
- [ ] Pixel 시리즈 (가능하면)
- [ ] Android 10, 11, 12, 13, 14 버전 테스트

---

### 6. 📦 AAB(Android App Bundle) 생성

플레이스토어는 AAB 형식을 권장합니다:
```bash
./gradlew bundleRelease
```

생성된 파일 위치:
```
app/build/outputs/bundle/release/app-release.aab
```

---

### 7. 🌐 플레이스토어 등록

#### Google Play Console 접속
https://play.google.com/console

#### 앱 생성 단계
1. "앱 만들기" 클릭
2. 앱 이름: "시선 추적 커서" (또는 원하는 이름)
3. 기본 언어: 한국어
4. 앱 유형: 앱
5. 무료/유료: 무료

#### 필수 입력 정보
1. **앱 콘텐츠**
   - 개인정보 처리방침
   - 대상 고객 및 콘텐츠
   - 앱 액세스 권한
   - 광고 포함 여부

2. **스토어 등록정보**
   - 앱 이름
   - 간단한 설명 (80자 이내)
   - 자세한 설명 (4000자 이내)
   - 스크린샷
   - 아이콘

3. **앱 콘텐츠 등급**
   - 접근성 도구이므로 "모든 연령" 가능

4. **앱 카테고리**
   - 카테고리: 도구
   - 태그: 접근성, 시선 추적, 보조 기술

---

### 8. ⚠️ 주의사항 및 권장사항

#### 접근성 앱으로서의 특별 요구사항
- [ ] 접근성 서비스 사용 목적을 명확히 설명
- [ ] 카메라 권한 사용 이유 상세 설명
- [ ] 오버레이 권한 사용 이유 설명
- [ ] 사용자 데이터 수집하지 않음을 명시

#### 라이센스 관련
- [ ] 프로덕션 라이센스 비용 확인
- [ ] 라이센스 갱신 주기 확인
- [ ] 사용량 제한 확인

#### 법적 검토
- [ ] 개인정보 처리방침 작성
- [ ] 이용약관 작성 (필요 시)
- [ ] 접근성 도구로서의 면책 조항

---

## 🔍 문제 해결

### ProGuard 오류 시
```bash
# ProGuard 로그 확인
./gradlew assembleRelease --stacktrace

# 문제가 되는 클래스를 proguard-rules.pro에 추가
-keep class 문제클래스명 { *; }
```

### 라이센스 키 오류 시
1. `local.properties` 파일 확인
2. 키에 따옴표나 공백이 없는지 확인
3. BuildConfig에서 제대로 로드되는지 로그 확인

### APK 서명 오류 시
1. Keystore 파일 경로 확인
2. 비밀번호 확인
3. Alias 이름 확인

---

## 📞 도움말

### EyeDID SDK 문의
- 웹사이트: https://www.visualcamp.com/
- 문서: https://docs.eyedid.ai/

### 플레이스토어 관련
- Google Play Console 고객센터
- https://support.google.com/googleplay/android-developer

---

## ✅ 최종 체크리스트

배포 전 모든 항목 확인:

- [ ] 1. 프로덕션 라이센스 키 설정 완료
- [ ] 2. 버전 코드/이름 업데이트
- [ ] 3. Keystore 생성 및 서명 설정
- [ ] 4. 플레이스토어 메타데이터 준비
- [ ] 5. 최종 테스트 완료
- [ ] 6. AAB 파일 생성
- [ ] 7. 플레이스토어 등록
- [ ] 8. 개인정보 처리방침 준비

---

**마지막 확인 날짜:** 2025-10-22
**작성자:** Claude (Anthropic)
