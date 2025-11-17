package camp.visual.android.sdk.sample.core.constants;

import com.eorua.gazecursor.BuildConfig;

/**
 * 🔒 애플리케이션 전역 상수 관리
 * - 보안 강화: 민감한 정보 분리
 * - 중앙 집중식 상수 관리
 * - 빌드 설정별 다른 값 적용 가능
 */
public final class AppConstants {
    
    // 🔒 보안: 라이센스 키 (BuildConfig에서 가져옴)
    public static final String EYEDID_SDK_LICENSE = BuildConfig.EYEDID_LICENSE_KEY;
    
    // 📱 서비스 관련 상수
    public static final String NOTIFICATION_CHANNEL_ID = "GazeTrackingServiceChannel";
    public static final String NOTIFICATION_CHANNEL_NAME = "시선 추적 서비스 채널";
    public static final int FOREGROUND_SERVICE_ID = 1001;
    
    // 🎯 권한 요청 코드
    public static final class PermissionRequestCodes {
        public static final int CAMERA = 1000;
        public static final int OVERLAY = 1001;
        public static final int ACCESSIBILITY = 1002;
    }
    
    // ⚙️ 기본 설정값
    public static final class DefaultSettings {
        public static final int TARGET_FPS = 30;
        public static final long CLICK_TIMEOUT_MS = 3000L;
        public static final long EDGE_TRIGGER_MS = 3000L;
        public static final float EDGE_MARGIN_RATIO = 0.1f;
        public static final long SCROLL_COOLDOWN_MS = 1500L;
        public static final long PERFORMANCE_CHECK_INTERVAL_MS = 10000L;
    }
    
    // 🔧 필터 기본값
    public static final class FilterDefaults {
        public static final float ONE_EURO_FREQ = 30.0f;
        public static final float ONE_EURO_MIN_CUTOFF = 1.0f;
        public static final float ONE_EURO_BETA = 0.4f;
        public static final float ONE_EURO_D_CUTOFF = 1.0f;
    }
    
    // 📊 성능 임계값
    public static final class PerformanceThresholds {
        public static final int BATTERY_CRITICAL = 15; // 15% 이하
        public static final int BATTERY_LOW = 30; // 30% 이하
        public static final float CPU_CRITICAL = 80.0f; // 80% 이상
        public static final int MEMORY_CRITICAL_MB = 100; // 100MB 이하
        public static final int MAX_CONSECUTIVE_DROPS = 5; // 연속 프레임 드롭 허용 횟수
    }
    
    // 🎨 UI 상수
    public static final class UI {
        public static final long UI_UPDATE_INTERVAL_MS = 16L; // 60fps
        public static final long TOAST_DURATION_SHORT = 2000L;
        public static final long TOAST_DURATION_LONG = 4000L;
        public static final int CALIBRATION_ANIMATION_DURATION_MS = 1000;
    }
    
    // 🔍 로깅 설정
    public static final class Logging {
        public static final String TAG_MAIN = "EyeDID-Main";
        public static final String TAG_SERVICE = "EyeDID-Service";
        public static final String TAG_CALIBRATION = "EyeDID-Calibration";
        public static final String TAG_PERFORMANCE = "EyeDID-Performance";
        public static final String TAG_SECURITY = "EyeDID-Security";
        public static final long LOG_INTERVAL_MS = 1000L; // 로그 간격 제한
    }
    
    // 📁 파일 경로
    public static final class FilePaths {
        public static final String CALIBRATION_DATA = "calibration_data.dat";
        public static final String USER_SETTINGS = "user_settings.pref";
        public static final String PERFORMANCE_LOG = "performance.log";
    }
    
    // 🌐 네트워크 관련
    public static final class Network {
        public static final int CONNECTION_TIMEOUT_MS = 5000;
        public static final int READ_TIMEOUT_MS = 10000;
        public static final String API_BASE_URL = "https://api.eyedid.ai";
    }
    
    // Private constructor to prevent instantiation
    private AppConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
