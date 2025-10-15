package camp.visual.android.sdk.sample.ui.views.overlay;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import camp.visual.android.sdk.sample.service.accessibility.MyAccessibilityService;
import camp.visual.android.sdk.sample.service.tracking.GazeTrackingService;

public class NavigationMenuOverlay extends EdgeMenuOverlay {
    
    private static final String TAG = "NavigationMenuOverlay";
    private Context context;
    private Handler mainHandler;
    
    public NavigationMenuOverlay(Context context) {
        super(context, Corner.LEFT_TOP);
        this.context = context;
        this.mainHandler = new Handler(Looper.getMainLooper());
    }
    
    @Override
    protected void initMenuButtons() {
        // 🔙 뒤로가기
        addMenuButton("◀", "뒤로", () -> {
            Log.d(TAG, "뒤로가기 실행");
            performBackAction();
        });
        
        // 🏠 홈
        addMenuButton("⌂", "홈", () -> {
            Log.d(TAG, "홈 버튼 실행");
            performHomeAction();
        });
        
        // ⧉ 최근 앱
        addMenuButton("⧉", "최근", () -> {
            Log.d(TAG, "최근 앱 실행");
            performRecentAppsAction();
        });
        
        // 🎯 정밀 보정
        addMenuButton("🎯", "보정", () -> {
            Log.d(TAG, "정밀 보정 실행");
            performPrecisionCalibration();
        });
    }
    
    private void performBackAction() {
        try {
            if (MyAccessibilityService.getInstance() != null) {
                // AccessibilityService를 통한 뒤로가기
                MyAccessibilityService.getInstance().performGlobalAction(
                    android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK
                );
                Log.d(TAG, "뒤로가기 성공");
            } else {
                Log.w(TAG, "AccessibilityService가 활성화되지 않음");
            }
        } catch (Exception e) {
            Log.e(TAG, "뒤로가기 실행 실패: " + e.getMessage());
        }
    }
    
    private void performHomeAction() {
        try {
            if (MyAccessibilityService.getInstance() != null) {
                // AccessibilityService를 통한 홈 버튼
                MyAccessibilityService.getInstance().performGlobalAction(
                    android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME
                );
                Log.d(TAG, "홈 버튼 성공");
            } else {
                Log.w(TAG, "AccessibilityService가 활성화되지 않음");
            }
        } catch (Exception e) {
            Log.e(TAG, "홈 버튼 실행 실패: " + e.getMessage());
        }
    }
    
    private void performRecentAppsAction() {
        try {
            if (MyAccessibilityService.getInstance() != null) {
                // AccessibilityService를 통한 최근 앱
                MyAccessibilityService.getInstance().performGlobalAction(
                    android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_RECENTS
                );
                Log.d(TAG, "최근 앱 성공");
            } else {
                Log.w(TAG, "AccessibilityService가 활성화되지 않음");
            }
        } catch (Exception e) {
            Log.e(TAG, "최근 앱 실행 실패: " + e.getMessage());
        }
    }
    
    private void performPrecisionCalibration() {
        // 메인 스레드에서 실행
        mainHandler.post(() -> {
            try {
                Log.d(TAG, "정밀 보정 바로 시작");
                
                // 서비스를 통한 직접 캘리브레이션 트리거 (다이얼로그 없이)
                GazeTrackingService service = GazeTrackingService.getInstance();
                if (service != null) {
                    // 토스트로 사용자에게 알림
                    Toast.makeText(context, "시선 보정을 시작합니다", Toast.LENGTH_SHORT).show();
                    
                    // 캘리브레이션 바로 시작
                    service.triggerCalibration();
                    
                    Log.d(TAG, "정밀 보정 트리거 성공");
                } else {
                    Log.w(TAG, "GazeTrackingService 인스턴스를 찾을 수 없음");
                    Toast.makeText(context, "시선 추적 서비스가 실행되지 않았습니다", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Log.e(TAG, "정밀 보정 실행 실패: " + e.getMessage());
                Toast.makeText(context, "보정 실행 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
