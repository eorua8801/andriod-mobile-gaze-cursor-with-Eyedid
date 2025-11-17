package camp.visual.android.sdk.sample.data.repository;

import android.content.Context;
import android.util.DisplayMetrics;
import android.util.Log;

import com.eorua.gazecursor.BuildConfig;

import camp.visual.eyedid.gazetracker.GazeTracker;
import camp.visual.eyedid.gazetracker.callback.CalibrationCallback;
import camp.visual.eyedid.gazetracker.callback.InitializationCallback;
import camp.visual.eyedid.gazetracker.callback.StatusCallback;
import camp.visual.eyedid.gazetracker.callback.TrackingCallback;
import camp.visual.eyedid.gazetracker.constant.AccuracyCriteria;
import camp.visual.eyedid.gazetracker.constant.CalibrationModeType;
import camp.visual.eyedid.gazetracker.constant.GazeTrackerOptions;
import camp.visual.eyedid.gazetracker.constant.StatusErrorType;
import camp.visual.eyedid.gazetracker.device.CameraPosition;

public class EyedidTrackingRepository implements EyeTrackingRepository {
    private static final String TAG = "EyedidTracking";
    // 🔒 BuildConfig에서 라이센스 키 가져오기
    private static final String LICENSE_KEY = BuildConfig.EYEDID_LICENSE_KEY;

    private GazeTracker gazeTracker;
    private int currentFPS = 30; // 기본 FPS
    private boolean performanceMonitoringEnabled = true;
    private Context context; // 🆕 Context 저장 (AttentionRegion 설정용)
    private StatusCallback externalStatusCallback; // 🆕 외부 StatusCallback 저장
    
    // 🆕 AccuracyCriteria 설정 저장 (캘리브레이션 시 사용)
    private AccuracyCriteria accuracyCriteria = AccuracyCriteria.HIGH;
    
    // 🆕 상태 관리 플래그
    private boolean isInitialized = false;
    private boolean isCleanedUp = false;

    @Override
    public void initialize(Context context, InitializationCallback callback) {
        if (isCleanedUp) {
            Log.e(TAG, "이미 정리된 리포지토리를 재초기화할 수 없음");
            return;
        }
        
        this.context = context;
        
        // 🔧 개선: GazeTrackerOptions 향상된 설정
        GazeTrackerOptions options = new GazeTrackerOptions.Builder()
                .setUseBlink(true)          // 깜박임 감지 활성화
                .setUseUserStatus(true)     // 사용자 상태 추적 활성화
                .setUseGazeFilter(true)     // 시선 필터링 활성화
                .setMaxConcurrency(4)       // 최대 동시 처리 스레드
                .build();

        GazeTracker.initGazeTracker(context, LICENSE_KEY, (tracker, error) -> {
            if (tracker != null) {
                gazeTracker = tracker;
                isInitialized = true;
                
                // 🆕 내장 StatusCallback 설정 (FPS 설정 타이밍 개선)
                setupInternalStatusCallback();
                
                Log.d(TAG, "시선 추적 SDK 초기화 성공 (HIGH 정확도 준비됨)");
            } else {
                Log.e(TAG, "시선 추적 SDK 초기화 실패: " + error);
                isInitialized = false;
            }
            callback.onInitialized(tracker, error);
        }, options);
    }

    @Override
    public void startTracking() {
        if (isTrackerReady()) {
            gazeTracker.startTracking();
            Log.d(TAG, "시선 추적 시작 요청");
        } else {
            Log.w(TAG, "시선 추적 시작 불가능 - Tracker 준비 안됨");
        }
    }

    @Override
    public void stopTracking() {
        if (gazeTracker != null) {
            gazeTracker.stopTracking();
            Log.d(TAG, "시선 추적 중지 요청");
        }
    }

    @Override
    public void startCalibration(CalibrationModeType type) {
        // 🔧 개선: 상태 확인 강화
        if (!isTrackerReady()) {
            Log.w(TAG, "캘리브레이션 시작 불가능 - Tracker 준비 안됨");
            return;
        }
        
        if (!gazeTracker.isTracking()) {
            Log.w(TAG, "캘리브레이션 시작 불가능 - 시선 추적이 시작되지 않음");
            return;
        }
        
        if (gazeTracker.isCalibrating()) {
            Log.w(TAG, "캘리브레이션 시작 불가능 - 이미 캘리브레이션 진행 중");
            return;
        }
        
        // 🆕 AccuracyCriteria.HIGH 적용하여 캘리브레이션 시작
        if (context != null) {
            DisplayMetrics dm = context.getResources().getDisplayMetrics();
            boolean success = gazeTracker.startCalibration(
                type, 
                accuracyCriteria,
                0, 0, dm.widthPixels, dm.heightPixels, // 전체 화면 영역
                false // 새로운 캘리브레이션
            );
            Log.d(TAG, "캘리브레이션 시작 (AccuracyCriteria.HIGH): " + (success ? "성공" : "실패"));
        } else {
            boolean success = gazeTracker.startCalibration(type, accuracyCriteria);
            Log.d(TAG, "캘리브레이션 시작 (AccuracyCriteria.HIGH): " + (success ? "성공" : "실패"));
        }
    }

    @Override
    public void setTrackingCallback(TrackingCallback callback) {
        if (gazeTracker != null) {
            gazeTracker.setTrackingCallback(callback);
        }
    }

    @Override
    public void setCalibrationCallback(CalibrationCallback callback) {
        if (gazeTracker != null) {
            gazeTracker.setCalibrationCallback(callback);
        }
    }

    @Override
    public void setStatusCallback(StatusCallback callback) {
        externalStatusCallback = callback; // 🆕 외부 콜백 저장
        // 내장 StatusCallback이 이미 설정되어 있으므로 여기서는 저장만
        Log.d(TAG, "외부 StatusCallback 설정");
    }

    @Override
    public GazeTracker getTracker() {
        return gazeTracker;
    }

    // 🆕 동적 FPS 조정 기능 (GazeTracker 인스턴스에서 직접 호출)
    public void setTrackingFPS(int fps) {
        if (gazeTracker != null && fps >= 1 && fps <= 30) {
            int oldFPS = currentFPS;
            currentFPS = fps;
            gazeTracker.setTrackingFPS(fps); // GazeTracker 인스턴스 메서드 사용
            Log.d(TAG, "추적 FPS 변경: " + oldFPS + " -> " + fps);
        } else if (gazeTracker == null) {
            // 초기화 전에는 값만 저장
            currentFPS = fps;
            Log.d(TAG, "FPS 설정 저장 (초기화 후 적용): " + fps);
        }
    }

    public int getCurrentFPS() {
        return currentFPS;
    }

    // 🆕 성능 기반 FPS 자동 조정
    public void adjustFPSBasedOnPerformance(int batteryLevel, float cpuUsage, long availableMemoryMB) {
        if (!performanceMonitoringEnabled) return;

        int optimalFPS = calculateOptimalFPS(batteryLevel, cpuUsage, availableMemoryMB);

        if (optimalFPS != currentFPS) {
            setTrackingFPS(optimalFPS);
            Log.d(TAG, "성능 기반 FPS 자동 조정: " + currentFPS + " -> " + optimalFPS +
                    " (배터리: " + batteryLevel + "%, CPU: " + cpuUsage + "%, 메모리: " + availableMemoryMB + "MB)");
        }
    }

    private int calculateOptimalFPS(int batteryLevel, float cpuUsage, long availableMemoryMB) {
        // 배터리 수준에 따른 기본 FPS 결정
        int baseFPS;
        if (batteryLevel < 15) {
            baseFPS = 10; // 극저전력 모드
        } else if (batteryLevel < 30) {
            baseFPS = 15; // 절전 모드
        } else if (batteryLevel < 50) {
            baseFPS = 20; // 효율 모드
        } else {
            baseFPS = 30; // 표준 모드
        }

        // CPU 사용률에 따른 조정
        if (cpuUsage > 80) {
            baseFPS = Math.max(10, baseFPS - 10);
        } else if (cpuUsage > 60) {
            baseFPS = Math.max(15, baseFPS - 5);
        }

        // 메모리 사용량에 따른 조정 (이미 MB 단위)
        if (availableMemoryMB < 100) {
            baseFPS = Math.max(10, baseFPS - 5);
        } else if (availableMemoryMB < 200) {
            baseFPS = Math.max(15, baseFPS - 3);
        }

        return Math.max(10, Math.min(30, baseFPS));
    }

    public void setPerformanceMonitoringEnabled(boolean enabled) {
        performanceMonitoringEnabled = enabled;
        Log.d(TAG, "성능 모니터링 " + (enabled ? "활성화" : "비활성화"));
    }

    public boolean isPerformanceMonitoringEnabled() {
        return performanceMonitoringEnabled;
    }

    // 🆕 AccuracyCriteria 설정 메서드
    public void setAccuracyCriteria(AccuracyCriteria criteria) {
        accuracyCriteria = criteria;
        Log.d(TAG, "AccuracyCriteria 설정: " + criteria.toString());
    }

    public AccuracyCriteria getAccuracyCriteria() {
        return accuracyCriteria;
    }
    
    // 🔴 CRITICAL: 리소스 정리 및 GazeTracker 해제
    @Override
    public void cleanup() {
        if (isCleanedUp) {
            Log.w(TAG, "이미 정리된 상태");
            return;
        }
        
        Log.d(TAG, "GazeTracker 리소스 정리 시작");
        
        try {
            if (gazeTracker != null) {
                // 1. 추적 중지
                if (gazeTracker.isTracking()) {
                    gazeTracker.stopTracking();
                    Log.d(TAG, "시선 추적 중지");
                }
                
                // 2. 캘리브레이션 중지 (진행 중인 경우)
                if (gazeTracker.isCalibrating()) {
                    gazeTracker.stopCalibration();
                    Log.d(TAG, "캘리브레이션 중지");
                }
                
                // 3. AttentionRegion 제거
                gazeTracker.removeAttentionRegion();
                
                // 4. 모든 콜백 제거
                gazeTracker.removeCallbacks();
                Log.d(TAG, "모든 콜백 제거");
                
                // 5. 🔴 CRITICAL: GazeTracker 리소스 해제
                GazeTracker.releaseGazeTracker(gazeTracker);
                gazeTracker = null;
                Log.d(TAG, "GazeTracker 리소스 해제 완료");
            }
            
            // 6. 내부 상태 리셋
            isInitialized = false;
            isCleanedUp = true;
            externalStatusCallback = null;
            context = null;
            
            Log.d(TAG, "GazeTracker 리소스 정리 완료");
            
        } catch (Exception e) {
            Log.e(TAG, "GazeTracker 정리 중 오류", e);
        }
    }
    
    // 🆕 내장 StatusCallback 설정 (FPS 설정 타이밍 개선)
    private void setupInternalStatusCallback() {
        if (gazeTracker == null) return;
        
        gazeTracker.setStatusCallback(new StatusCallback() {
            @Override
            public void onStarted() {
                Log.d(TAG, "시선 추적 시작 완료");
                
                // 🔧 개선: 추적 시작 완료 후 설정 적용
                setTrackingFPS(currentFPS);
                setupCameraPosition();
                setupAttentionRegion();
                
                // 외부 콜백에도 전달
                if (externalStatusCallback != null) {
                    externalStatusCallback.onStarted();
                }
            }

            @Override
            public void onStopped(StatusErrorType error) {
                String errorMsg = "";
                switch (error) {
                    case ERROR_NONE:
                        errorMsg = "정상 종료";
                        break;
                    case ERROR_CAMERA_START:
                        errorMsg = "카메라 시작 오류";
                        break;
                    case ERROR_CAMERA_INTERRUPT:
                        errorMsg = "카메라 중단됨";
                        break;
                    default:
                        errorMsg = "알 수 없는 오류";
                        break;
                }
                Log.w(TAG, "시선 추적 중지: " + errorMsg);
                
                // 외부 콜백에도 전달
                if (externalStatusCallback != null) {
                    externalStatusCallback.onStopped(error);
                }
            }
        });
    }
    
    // 🆕 Tracker 준비 상태 확인
    private boolean isTrackerReady() {
        return gazeTracker != null && isInitialized && !isCleanedUp;
    }
    
    // 🆕 CameraPosition 설정 (공식 문서 추가 기능)
    private void setupCameraPosition() {
        if (!isTrackerReady()) return;
        
        try {
            // 🔧 수정: getCameraPosition()을 직접 호출해서 null 체크로 대체
            CameraPosition currentPosition = gazeTracker.getCameraPosition();
            if (currentPosition != null) {
                Log.d(TAG, "CameraPosition: " + currentPosition.modelName + 
                       " (" + currentPosition.screenOriginX + ", " + currentPosition.screenOriginY + ")");
            } else {
                Log.w(TAG, "CameraPosition 정보 없음 - 기본값 사용");
            }
        } catch (Exception e) {
            Log.e(TAG, "CameraPosition 설정 오류", e);
        }
    }
    
    // 🆕 AttentionRegion 설정 (성능 최적화)
    private void setupAttentionRegion() {
        if (!isTrackerReady() || context == null) return;
        
        try {
            DisplayMetrics dm = context.getResources().getDisplayMetrics();
            float margin = 50f; // 50px 여백
            
            gazeTracker.setAttentionRegion(
                margin,                           // left
                margin,                           // top  
                dm.widthPixels - margin,         // right
                dm.heightPixels - margin         // bottom
            );
            
            Log.d(TAG, "AttentionRegion 설정: 화면 중앙 영역 (50px 여백)");
            
        } catch (Exception e) {
            Log.e(TAG, "AttentionRegion 설정 오류", e);
        }
    }
    
    // 🆕 AttentionRegion 제거
    public void removeAttentionRegion() {
        if (isTrackerReady()) {
            gazeTracker.removeAttentionRegion();
            Log.d(TAG, "AttentionRegion 제거");
        }
    }
    
    // 🆕 현재 AttentionRegion 조회
    public String getAttentionRegionInfo() {
        if (!isTrackerReady()) return "비활성화";
        
        try {
            android.graphics.RectF region = gazeTracker.getAttentionRegion();
            if (region != null) {
                return String.format("AttentionRegion: (%.0f, %.0f, %.0f, %.0f)", 
                    region.left, region.top, region.right, region.bottom);
            } else {
                return "AttentionRegion 설정 없음";
            }
        } catch (Exception e) {
            return "AttentionRegion 조회 오류: " + e.getMessage();
        }
    }
    
    // 🆕 향상된 FPS 설정 (검증 추가)
    public void setTrackingFPSWithValidation(int fps) {
        if (fps < 1 || fps > 30) {
            Log.w(TAG, "잘못된 FPS 값: " + fps + " (1-30 범위 필요)");
            return;
        }
        
        if (isTrackerReady() && gazeTracker.isTracking()) {
            boolean success = gazeTracker.setTrackingFPS(fps);
            if (success) {
                int oldFPS = currentFPS;
                currentFPS = fps;
                Log.d(TAG, "추적 FPS 변경 성공: " + oldFPS + " -> " + fps);
            } else {
                Log.w(TAG, "FPS 설정 실패: " + fps);
            }
        } else {
            // 추적 시작 전에는 값만 저장
            currentFPS = fps;
            Log.d(TAG, "FPS 설정 저장 (추적 시작 후 적용): " + fps);
        }
    }
    
    // 🆕 현재 상태 정보 조회
    public String getTrackerStatusInfo() {
        if (!isTrackerReady()) {
            return "트래커 비활성화 상태";
        }
        
        StringBuilder info = new StringBuilder();
        info.append("=== GazeTracker 상태 ===\n");
        info.append("초기화: ").append(isInitialized ? "완료" : "미완료").append("\n");
        info.append("추적 상태: ").append(gazeTracker.isTracking() ? "시작됨" : "중지됨").append("\n");
        info.append("캘리브레이션: ").append(gazeTracker.isCalibrating() ? "진행 중" : "비활성화").append("\n");
        info.append("현재 FPS: ").append(currentFPS).append("\n");
        info.append("성능 모니터링: ").append(performanceMonitoringEnabled ? "활성화" : "비활성화").append("\n");
        info.append("정확도 기준: ").append(accuracyCriteria.toString()).append("\n");
        // 🔧 수정: getCameraPosition()을 직접 호출해서 null 체크로 대체
        try {
            CameraPosition position = gazeTracker.getCameraPosition();
            info.append("CameraPosition: ").append(position != null ? "사용 가능" : "기본값").append("\n");
        } catch (Exception e) {
            info.append("CameraPosition: 비활성화\n");
        }
        info.append(getAttentionRegionInfo());
        
        return info.toString();
    }
}