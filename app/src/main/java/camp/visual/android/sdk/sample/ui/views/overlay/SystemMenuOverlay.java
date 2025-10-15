package camp.visual.android.sdk.sample.ui.views.overlay;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.provider.Settings;
import android.util.Log;

import camp.visual.android.sdk.sample.service.accessibility.MyAccessibilityService;

public class SystemMenuOverlay extends EdgeMenuOverlay {
    
    private static final String TAG = "SystemMenuOverlay";
    private Context context;
    private AudioManager audioManager;
    
    public SystemMenuOverlay(Context context) {
        super(context, Corner.RIGHT_TOP);
        this.context = context;
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }
    
    @Override
    protected void initMenuButtons() {
        // 🔊 음량 키움
        addMenuButton("🔊", "음량+", () -> {
            Log.d(TAG, "음량 키움 실행");
            performVolumeUp();
        });
        
        // 🔉 음량 줄임
        addMenuButton("🔉", "음량-", () -> {
            Log.d(TAG, "음량 줄임 실행");
            performVolumeDown();
        });
        
        // 📳 진동 모드
        addMenuButton("📳", "진동", () -> {
            Log.d(TAG, "진동 모드 전환 실행");
            performToggleVibrationMode();
        });
        
        // 🔧 기능 추가 예정
        addMenuButton("➕", "예정", () -> {
            Log.d(TAG, "기능 추가 예정 - 아직 구현되지 않음");
            // 아무 동작도 하지 않음
        });
    }
    
    private void performVolumeUp() {
        try {
            if (audioManager != null) {
                // 현재 스트림 타입에 따라 음량을 증가시킴
                audioManager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_RAISE,
                    AudioManager.FLAG_SHOW_UI
                );
                Log.d(TAG, "음량 키움 성공");
            } else {
                Log.w(TAG, "AudioManager가 초기화되지 않음");
            }
        } catch (Exception e) {
            Log.e(TAG, "음량 키움 실패: " + e.getMessage());
        }
    }
    
    private void performVolumeDown() {
        try {
            if (audioManager != null) {
                // 현재 스트림 타입에 따라 음량을 감소시킴
                audioManager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_LOWER,
                    AudioManager.FLAG_SHOW_UI
                );
                Log.d(TAG, "음량 줄임 성공");
            } else {
                Log.w(TAG, "AudioManager가 초기화되지 않음");
            }
        } catch (Exception e) {
            Log.e(TAG, "음량 줄임 실패: " + e.getMessage());
        }
    }
    
    private void performToggleVibrationMode() {
        try {
            if (audioManager != null) {
                int currentMode = audioManager.getRingerMode();
                
                // 현재 모드에 따라 진동 모드로 전환
                if (currentMode == AudioManager.RINGER_MODE_NORMAL) {
                    // 일반 모드 → 진동 모드
                    audioManager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
                    Log.d(TAG, "진동 모드로 전환됨");
                } else if (currentMode == AudioManager.RINGER_MODE_VIBRATE) {
                    // 진동 모드 → 무음 모드
                    audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
                    Log.d(TAG, "무음 모드로 전환됨");
                } else {
                    // 무음 모드 → 일반 모드
                    audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                    Log.d(TAG, "일반 모드로 전환됨");
                }
            } else {
                Log.w(TAG, "AudioManager가 초기화되지 않음");
            }
        } catch (Exception e) {
            Log.e(TAG, "진동 모드 전환 실패: " + e.getMessage());
        }
    }
}
