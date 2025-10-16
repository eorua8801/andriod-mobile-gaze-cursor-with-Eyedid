package camp.visual.android.sdk.sample.core.managers;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.content.pm.PackageManager;

import camp.visual.android.sdk.sample.core.constants.AppConstants;
import camp.visual.android.sdk.sample.core.utils.PerformanceLogger;
import camp.visual.android.sdk.sample.service.accessibility.MyAccessibilityService;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * 🔐 권한 관리 매니저
 * - 모든 권한을 단계별로 관리
 * - 사용자 친화적인 권한 요청 플로우
 * - 권한 상태 모니터링
 * - 메모리 효율적인 콜백 관리
 */
public class PermissionManager {
    
    private final WeakReference<Activity> activityRef;
    private final WeakReference<Context> contextRef;
    private PermissionCallback callback;
    
    // 필요한 권한들 (API 레벨에 따라 동적으로 결정)
    private static String[] getRequiredPermissions() {
        List<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.CAMERA);
        permissions.add(Manifest.permission.VIBRATE);
        permissions.add(Manifest.permission.FOREGROUND_SERVICE);
        
        // Android 14 (API 34) 이상에서만 FOREGROUND_SERVICE_CAMERA 권한 필요
        if (Build.VERSION.SDK_INT >= 34) { // API 34 = Android 14
            permissions.add(Manifest.permission.FOREGROUND_SERVICE_CAMERA);
        }
        
        return permissions.toArray(new String[0]);
    }
    
    public PermissionManager(Activity activity) {
        this.activityRef = new WeakReference<>(activity);
        this.contextRef = new WeakReference<>(activity.getApplicationContext());
    }
    
    /**
     * 🔍 모든 권한 상태 확인
     */
    public PermissionStatus checkAllPermissions() {
        Context context = contextRef.get();
        if (context == null) {
            return PermissionStatus.createError("Context is null");
        }
        
        try {
            // 1. 기본 권한들 확인 (API 레벨에 따라 동적)
            String[] requiredPermissions = getRequiredPermissions();
            List<String> missingBasicPermissions = new ArrayList<>();
            for (String permission : requiredPermissions) {
                if (ContextCompat.checkSelfPermission(context, permission) 
                    != PackageManager.PERMISSION_GRANTED) {
                    missingBasicPermissions.add(permission);
                }
            }
            
            // 2. 오버레이 권한 확인
            boolean hasOverlayPermission = Settings.canDrawOverlays(context);
            
            // 3. 접근성 서비스 확인
            boolean hasAccessibilityPermission = isAccessibilityServiceEnabled(context);
            
            return new PermissionStatus(
                missingBasicPermissions,
                hasOverlayPermission,
                hasAccessibilityPermission
            );
            
        } catch (Exception e) {
            PerformanceLogger.logError(AppConstants.Logging.TAG_MAIN, 
                "Error checking permissions", e);
            return PermissionStatus.createError("Permission check failed: " + e.getMessage());
        }
    }
    
    /**
     * 🚀 권한 요청 플로우 시작
     */
    public void startPermissionFlow(PermissionCallback callback) {
        this.callback = callback;
        Activity activity = activityRef.get();
        
        if (activity == null) {
            if (callback != null) {
                callback.onPermissionFlowCompleted(false, "Activity is null");
            }
            return;
        }
        
        PermissionStatus status = checkAllPermissions();
        if (status.hasError()) {
            if (callback != null) {
                callback.onPermissionFlowCompleted(false, status.getErrorMessage());
            }
            return;
        }
        
        // 권한 요청 순서: 기본 권한 → 오버레이 → 접근성
        if (!status.getMissingBasicPermissions().isEmpty()) {
            requestBasicPermissions(status.getMissingBasicPermissions());
        } else if (!status.hasOverlayPermission()) {
            requestOverlayPermission();
        } else if (!status.hasAccessibilityPermission()) {
            requestAccessibilityPermission();
        } else {
            // 모든 권한 완료
            PerformanceLogger.logImportant(AppConstants.Logging.TAG_MAIN, 
                "모든 권한이 승인되었습니다. GazeTracker 초기화를 시작합니다.");
            if (callback != null) {
                callback.onPermissionFlowCompleted(true, "All permissions granted");
            }
        }
    }
    
    /**
     * 📱 기본 권한 요청
     */
    private void requestBasicPermissions(List<String> permissions) {
        Activity activity = activityRef.get();
        if (activity == null) return;
        
        String[] permissionArray = permissions.toArray(new String[0]);
        
        PerformanceLogger.SecurityLogger.logPermissionDenied(
            "Requesting basic permissions: " + permissions.toString());
        
        if (callback != null) {
            callback.onPermissionRequested(PermissionType.BASIC, permissions);
        }
        
        ActivityCompat.requestPermissions(activity, permissionArray, 
            AppConstants.PermissionRequestCodes.CAMERA);
    }
    
    /**
     * 🖼️ 오버레이 권한 요청
     */
    private void requestOverlayPermission() {
        Activity activity = activityRef.get();
        if (activity == null) return;
        
        PerformanceLogger.SecurityLogger.logPermissionDenied("SYSTEM_ALERT_WINDOW");
        
        if (callback != null) {
            callback.onPermissionRequested(PermissionType.OVERLAY, 
                List.of(Manifest.permission.SYSTEM_ALERT_WINDOW));
        }
        
        // 🆕 사용자에게 오버레이 권한에 대한 설명 제공
        showOverlayPermissionDialog(activity);
    }
    
    /**
     * 🆕 오버레이 권한 설명 다이얼로그
     */
    private void showOverlayPermissionDialog(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            new android.app.AlertDialog.Builder(activity)
                .setTitle("🖼️ 화면 위에 그리기 권한 필요")
                .setMessage("시선 커서와 캘리브레이션 화면을 표시하려면 화면 위에 그리기 권한이 필요합니다.\n\n" +
                           "이 권한으로 다음이 표시됩니다:\n" +
                           "• 👁️ 시선 위치를 나타내는 커서\n" +
                           "• 🎯 캘리브레이션(보정) 화면\n\n" +

                           "아래 '설정으로 이동' 버튼을 누른 후 '시선 추적 커서' 앱의 권한을 활성화 하고 다시 이 앱으로 돌아와 주세요.")
                .setPositiveButton("⚙️ 설정으로 이동", (dialog, which) -> {
                    openOverlaySettings(activity);
                })
                .setNegativeButton("나중에", (dialog, which) -> {
                    if (callback != null) {
                        callback.onPermissionDenied(PermissionType.OVERLAY, 
                            List.of(Manifest.permission.SYSTEM_ALERT_WINDOW));
                    }
                })
                .setCancelable(false)
                .show();
        } else {
            // API 레벨이 낮은 경우 직접 설정으로 이동
            openOverlaySettings(activity);
        }
    }
    
    /**
     * 🆕 오버레이 설정 열기
     */
    private void openOverlaySettings(Activity activity) {
        try {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + activity.getPackageName()));
            activity.startActivityForResult(intent, AppConstants.PermissionRequestCodes.OVERLAY);
            
        } catch (Exception e) {
            PerformanceLogger.logError(AppConstants.Logging.TAG_MAIN, 
                "Failed to open overlay settings", e);
            
            if (callback != null) {
                callback.onPermissionError(PermissionType.OVERLAY, 
                    "Cannot open overlay permission settings");
            }
        }
    }
    
    /**
     * ♿ 접근성 서비스 권한 요청
     */
    private void requestAccessibilityPermission() {
        Activity activity = activityRef.get();
        if (activity == null) return;
        
        PerformanceLogger.SecurityLogger.logPermissionDenied("ACCESSIBILITY_SERVICE");
        
        if (callback != null) {
            callback.onPermissionRequested(PermissionType.ACCESSIBILITY, 
                List.of("ACCESSIBILITY_SERVICE"));
        }
        
        // 🆕 사용자에게 접근성 권한에 대한 상세한 설명 제공
        showAccessibilityPermissionDialog(activity);
    }
    
    /**
     * 🆕 접근성 권한 설명 다이얼로그
     */
    private void showAccessibilityPermissionDialog(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            new android.app.AlertDialog.Builder(activity)
                .setTitle("🛡️ 접근성 서비스 권한 필요")
                .setMessage("시선 추적으로 화면을 조작하려면 접근성 서비스 권한이 필요합니다.\n\n" +
                           "이 권한으로 다음이 가능해집니다:\n" +
                           "• 👁️ 시선으로 클릭하기\n" +
                           "• 📱 시선으로 스크롤하기\n" +
                           "• 🖱️ 시선으로 스와이프하기\n\n" +
                           "❗ 중요: 이 앱은 시선 추적 목적으로만 접근성 서비스를 사용하며, 개인정보를 수집하지 않습니다.\n\n" +
                           "'설정으로 이동' 버튼을 누른 후 '설치된 앱'에서 '시선 추적 커서' 앱의 접근성 서비스를 활성화 한 다음 다시 이 앱으로 돌아와 주세요.")
                .setPositiveButton("⚙️ 설정으로 이동", (dialog, which) -> {
                    openAccessibilitySettings(activity);
                })
                .setNegativeButton("나중에", (dialog, which) -> {
                    if (callback != null) {
                        callback.onPermissionDenied(PermissionType.ACCESSIBILITY, 
                            List.of("ACCESSIBILITY_SERVICE"));
                    }
                })
                .setCancelable(false)
                .show();
        } else {
            // API 레벨이 낮은 경우 직접 설정으로 이동
            openAccessibilitySettings(activity);
        }
    }
    
    /**
     * 🆕 접근성 설정 열기
     */
    private void openAccessibilitySettings(Activity activity) {
        try {
            // 직접 서비스 설정으로 이동 시도
            ComponentName componentName = new ComponentName(activity.getPackageName(),
                MyAccessibilityService.class.getName());
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Bundle bundle = new Bundle();
                String showArgs = componentName.flattenToString();
                bundle.putString(":settings:fragment_args_key", showArgs);
                intent.putExtra(":settings:show_fragment_args", bundle);
            }
            
            activity.startActivityForResult(intent, AppConstants.PermissionRequestCodes.ACCESSIBILITY);
            
        } catch (Exception e) {
            PerformanceLogger.logError(AppConstants.Logging.TAG_MAIN, 
                "Failed to open accessibility settings", e);
            
            // 대안: 일반 접근성 설정
            try {
                Intent fallbackIntent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                activity.startActivity(fallbackIntent);
            } catch (Exception fallbackError) {
                PerformanceLogger.logError(AppConstants.Logging.TAG_MAIN, 
                    "Failed to open accessibility settings (fallback)", fallbackError);
                
                if (callback != null) {
                    callback.onPermissionError(PermissionType.ACCESSIBILITY, 
                        "Cannot open accessibility settings");
                }
            }
        }
    }
    
    /**
     * 📱 기본 권한 요청 결과 처리
     */
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode != AppConstants.PermissionRequestCodes.CAMERA) {
            return;
        }
        
        List<String> deniedPermissions = new ArrayList<>();
        for (int i = 0; i < permissions.length; i++) {
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                deniedPermissions.add(permissions[i]);
                PerformanceLogger.SecurityLogger.logPermissionDenied(permissions[i]);
            } else {
                PerformanceLogger.SecurityLogger.logPermissionGranted(permissions[i]);
            }
        }
        
        if (deniedPermissions.isEmpty()) {
            // 기본 권한 모두 승인됨, 다음 단계로
            PerformanceLogger.logImportant(AppConstants.Logging.TAG_MAIN, 
                "기본 권한 승인 완료. 다음 권한 단계로 진행합니다.");
            continuePermissionFlow();
        } else {
            // 일부 권한 거부됨, 하지만 계속 진행
            PerformanceLogger.logWarning(AppConstants.Logging.TAG_MAIN, 
                "일부 기본 권한이 거부되었지만 계속 진행합니다: " + deniedPermissions.toString());
            if (callback != null) {
                callback.onPermissionDenied(PermissionType.BASIC, deniedPermissions);
            }
            // 필수가 아닌 권한이 거부되어도 다음 단계로 진행
            continuePermissionFlow();
        }
    }
    
    /**
     * 🖼️ 오버레이 권한 결과 처리
     */
    public void onOverlayPermissionResult() {
        Context context = contextRef.get();
        if (context == null) return;
        
        if (Settings.canDrawOverlays(context)) {
            PerformanceLogger.SecurityLogger.logPermissionGranted("SYSTEM_ALERT_WINDOW");
            continuePermissionFlow();
        } else {
            PerformanceLogger.SecurityLogger.logPermissionDenied("SYSTEM_ALERT_WINDOW");
            if (callback != null) {
                callback.onPermissionDenied(PermissionType.OVERLAY, 
                    List.of(Manifest.permission.SYSTEM_ALERT_WINDOW));
            }
        }
    }
    
    /**
     * ♿ 접근성 권한 결과 처리
     */
    public void onAccessibilityPermissionResult() {
        Context context = contextRef.get();
        if (context == null) return;
        
        if (isAccessibilityServiceEnabled(context)) {
            PerformanceLogger.SecurityLogger.logPermissionGranted("ACCESSIBILITY_SERVICE");
            continuePermissionFlow();
        } else {
            PerformanceLogger.SecurityLogger.logPermissionDenied("ACCESSIBILITY_SERVICE");
            if (callback != null) {
                callback.onPermissionDenied(PermissionType.ACCESSIBILITY, 
                    List.of("ACCESSIBILITY_SERVICE"));
            }
        }
    }
    
    /**
     * 🔄 권한 플로우 계속 진행
     */
    private void continuePermissionFlow() {
        PermissionStatus status = checkAllPermissions();
        
        PerformanceLogger.logImportant(AppConstants.Logging.TAG_MAIN, 
            "권한 플로우 진행 중 - " + status.toString());
        
        if (!status.hasOverlayPermission()) {
            requestOverlayPermission();
        } else if (!status.hasAccessibilityPermission()) {
            requestAccessibilityPermission();
        } else {
            // 모든 권한 완료
            PerformanceLogger.logImportant(AppConstants.Logging.TAG_MAIN, 
                "권한 플로우 완료! GazeTracker 초기화를 시작합니다.");
            if (callback != null) {
                callback.onPermissionFlowCompleted(true, "All permissions granted");
            }
        }
    }
    
    /**
     * ♿ 접근성 서비스 활성화 확인
     */
    private boolean isAccessibilityServiceEnabled(Context context) {
        try {
            int accessibilityEnabled = Settings.Secure.getInt(
                context.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_ENABLED, 0);

            if (accessibilityEnabled != 1) {
                return false;
            }

            String enabledServices = Settings.Secure.getString(
                context.getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);

            if (enabledServices == null || enabledServices.isEmpty()) {
                return false;
            }

            final String packageName = context.getPackageName();
            final String fullServiceName = MyAccessibilityService.class.getName();

            return enabledServices.contains(packageName + "/" + fullServiceName) ||
                   enabledServices.contains(fullServiceName);

        } catch (Exception e) {
            PerformanceLogger.logError(AppConstants.Logging.TAG_MAIN, 
                "Error checking accessibility service", e);
            return false;
        }
    }
    
    /**
     * 🧹 리소스 정리
     */
    public void cleanup() {
        callback = null;
        PerformanceLogger.logImportant(AppConstants.Logging.TAG_MAIN, 
            "PermissionManager cleanup completed");
    }
    
    // 콜백 인터페이스
    public interface PermissionCallback {
        void onPermissionRequested(PermissionType type, List<String> permissions);
        void onPermissionGranted(PermissionType type, List<String> permissions);
        void onPermissionDenied(PermissionType type, List<String> permissions);
        void onPermissionError(PermissionType type, String error);
        void onPermissionFlowCompleted(boolean success, String message);
    }
    
    // 권한 타입
    public enum PermissionType {
        BASIC("기본 권한"),
        OVERLAY("오버레이 권한"),
        ACCESSIBILITY("접근성 서비스");
        
        private final String displayName;
        
        PermissionType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    // 권한 상태 클래스
    public static class PermissionStatus {
        private final List<String> missingBasicPermissions;
        private final boolean hasOverlayPermission;
        private final boolean hasAccessibilityPermission;
        private final String errorMessage;
        
        public PermissionStatus(List<String> missingBasicPermissions, 
                              boolean hasOverlayPermission, 
                              boolean hasAccessibilityPermission) {
            this.missingBasicPermissions = missingBasicPermissions != null ? 
                new ArrayList<>(missingBasicPermissions) : new ArrayList<>();
            this.hasOverlayPermission = hasOverlayPermission;
            this.hasAccessibilityPermission = hasAccessibilityPermission;
            this.errorMessage = null;
        }
        
        private PermissionStatus(String errorMessage) {
            this.missingBasicPermissions = new ArrayList<>();
            this.hasOverlayPermission = false;
            this.hasAccessibilityPermission = false;
            this.errorMessage = errorMessage;
        }
        
        public static PermissionStatus createError(String errorMessage) {
            return new PermissionStatus(errorMessage);
        }
        
        public List<String> getMissingBasicPermissions() {
            return new ArrayList<>(missingBasicPermissions);
        }
        
        public boolean hasOverlayPermission() {
            return hasOverlayPermission;
        }
        
        public boolean hasAccessibilityPermission() {
            return hasAccessibilityPermission;
        }
        
        public boolean hasAllPermissions() {
            return missingBasicPermissions.isEmpty() && 
                   hasOverlayPermission && 
                   hasAccessibilityPermission && 
                   !hasError();
        }
        
        public boolean hasError() {
            return errorMessage != null;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
        
        @Override
        public String toString() {
            if (hasError()) {
                return "PermissionStatus{error='" + errorMessage + "'}";
            }
            
            return String.format("PermissionStatus{missingBasic=%d, overlay=%s, accessibility=%s}", 
                missingBasicPermissions.size(), hasOverlayPermission, hasAccessibilityPermission);
        }
    }
}
