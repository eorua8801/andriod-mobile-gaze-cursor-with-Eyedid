package camp.visual.android.sdk.sample.ui.settings;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.eorua.gazecursor.R;
import camp.visual.android.sdk.sample.data.settings.SettingsRepository;
import camp.visual.android.sdk.sample.data.settings.SharedPrefsSettingsRepository;
import camp.visual.android.sdk.sample.domain.model.OneEuroFilterPreset;
import camp.visual.android.sdk.sample.domain.model.UserSettings;
import camp.visual.android.sdk.sample.service.tracking.GazeTrackingService;
import camp.visual.android.sdk.sample.ui.main.MainActivity;

public class SettingsActivity extends AppCompatActivity {

    private SettingsRepository settingsRepository;
    private UserSettings currentSettings;

    // 기본 설정
    private Switch backgroundLearningSwitch;
    private Switch autoOnePointCalibrationSwitch;

    // 커서 위치 조정
    private SeekBar cursorOffsetXBar;
    private TextView cursorOffsetXText;
    private SeekBar cursorOffsetYBar;
    private TextView cursorOffsetYText;

    // 🆕 커서 위치 조정 버튼들
    private Button btnResetCursorOffset;
    private Button btnPrecisionCalibration;

    // 커서 움직임 설정
    private RadioGroup performanceRadioGroup;
    private RadioButton radioStability;
    private RadioButton radioBalanced;
    private RadioButton radioResponsive;

    // 클릭 속도 설정
    private RadioGroup clickTimingRadioGroup;
    private RadioButton radioClickNormal;
    private RadioButton radioClickSlow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle("설정");
        }

        settingsRepository = new SharedPrefsSettingsRepository(this);
        currentSettings = settingsRepository.getUserSettings();

        initViews();
        loadSettings();
        setupListeners();
    }

    private void initViews() {
        // 기본 설정
        backgroundLearningSwitch = findViewById(R.id.switch_background_learning);
        autoOnePointCalibrationSwitch = findViewById(R.id.switch_auto_one_point_calibration);

        // 커서 위치 조정
        cursorOffsetXBar = findViewById(R.id.seekbar_cursor_offset_x);
        cursorOffsetXText = findViewById(R.id.text_cursor_offset_x);
        cursorOffsetYBar = findViewById(R.id.seekbar_cursor_offset_y);
        cursorOffsetYText = findViewById(R.id.text_cursor_offset_y);

        // 🆕 커서 위치 조정 버튼들 (안전하게 체크)
        btnResetCursorOffset = findViewById(R.id.btn_reset_cursor_offset);
        if (btnResetCursorOffset == null) {
            Log.w("SettingsActivity", "btn_reset_cursor_offset을 찾을 수 없습니다. XML에 추가가 필요합니다.");
        }

        btnPrecisionCalibration = findViewById(R.id.btn_precision_calibration);
        if (btnPrecisionCalibration == null) {
            Log.w("SettingsActivity", "btn_precision_calibration을 찾을 수 없습니다. XML에 추가가 필요합니다.");
        }

        // 커서 움직임 설정
        performanceRadioGroup = findViewById(R.id.radio_group_performance);
        radioStability = findViewById(R.id.radio_performance_stability);
        radioBalanced = findViewById(R.id.radio_performance_balanced);
        radioResponsive = findViewById(R.id.radio_performance_responsive);

        // 클릭 속도 설정
        clickTimingRadioGroup = findViewById(R.id.radio_group_click_timing);
        radioClickNormal = findViewById(R.id.radio_click_normal);
        radioClickSlow = findViewById(R.id.radio_click_slow);

        // 범위 설정
        cursorOffsetXBar.setMax(100);
        cursorOffsetYBar.setMax(100);
    }

    private void loadSettings() {
        // 기본 설정
        backgroundLearningSwitch.setChecked(currentSettings.isBackgroundLearningEnabled());
        autoOnePointCalibrationSwitch.setChecked(currentSettings.isAutoOnePointCalibrationEnabled());

        // 커서 오프셋
        cursorOffsetXBar.setProgress((int)(currentSettings.getCursorOffsetX() + 50));
        cursorOffsetYBar.setProgress((int)(currentSettings.getCursorOffsetY() + 50));
        updateCursorOffsetTexts();

        // 커서 움직임 설정
        OneEuroFilterPreset preset = currentSettings.getOneEuroFilterPreset();
        switch (preset) {
            case STABILITY:
                radioStability.setChecked(true);
                break;
            case RESPONSIVE:
                radioResponsive.setChecked(true);
                break;
            case BALANCED_STABILITY:
            case BALANCED:
            default:
                radioBalanced.setChecked(true);  // 기본값: 적당히
                break;
        }

        // 클릭 속도 설정
        UserSettings.ClickTiming clickTiming = currentSettings.getClickTiming();
        switch (clickTiming) {
            case NORMAL:
                radioClickNormal.setChecked(true);
                break;
            case SLOW:
                radioClickSlow.setChecked(true);
                break;
        }
    }

    private void setupListeners() {
        // 기본 설정 리스너
        autoOnePointCalibrationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveSettings();
        });

        backgroundLearningSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveSettings();
            if (isChecked) {
                Toast.makeText(this, "자동 학습 켜짐", Toast.LENGTH_SHORT).show();
            }
        });

        // 🆕 커서 오프셋 초기화 버튼 리스너
        if (btnResetCursorOffset != null) {
            btnResetCursorOffset.setOnClickListener(v -> {
                showResetCursorOffsetDialog();
            });
        }

        // 🆕 정밀 보정 버튼 리스너
        if (btnPrecisionCalibration != null) {
            btnPrecisionCalibration.setOnClickListener(v -> {
                startPrecisionCalibration();
            });
        }

        // 커서 움직임 설정 리스너
        performanceRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            saveSettings();
        });

        // 클릭 속도 리스너
        clickTimingRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            saveSettings();
        });

        // 커서 오프셋 리스너
        setupCursorOffsetListeners();
    }

    // 🆕 커서 오프셋 초기화 확인 다이얼로그
    private void showResetCursorOffsetDialog() {
        new AlertDialog.Builder(this)
                .setTitle("커서 위치 초기화")
                .setMessage("커서 위치 조정값을 모두 0으로 되돌리시겠습니까?")
                .setPositiveButton("초기화", (dialog, which) -> {
                    resetCursorOffset();
                })
                .setNegativeButton("취소", null)
                .show();
    }

    // 🆕 커서 오프셋 초기화 실행
    private void resetCursorOffset() {
        cursorOffsetXBar.setProgress(50); // 0 = 50 - 50
        cursorOffsetYBar.setProgress(50); // 0 = 50 - 50
        updateCursorOffsetTexts();
        saveSettings();
        Toast.makeText(this, "커서 위치가 초기화되었습니다", Toast.LENGTH_SHORT).show();
    }

    // 🆕 정밀 보정 시작
    private void startPrecisionCalibration() {
        new AlertDialog.Builder(this)
                .setTitle("정밀 보정")
                .setMessage("메인 화면의 시선 보정을 실행하시겠습니까?\n\n" +
                        "⚠️ 기존 위치 조정값이 초기화되고 새로운 보정을 실행합니다.\n\n" +
                        "설정을 저장한 후 메인 화면으로 이동합니다.")
                .setPositiveButton("보정 시작", (dialog, which) -> {
                    saveSettings();

                    // MainActivity로 이동하여 캘리브레이션 실행
                    try {
                        MainActivity mainActivity = MainActivity.getInstance();
                        if (mainActivity != null) {
                            // 설정 화면 종료
                            finish();
                            // MainActivity에서 캘리브레이션 실행
                            mainActivity.triggerCalibrationFromService();
                        } else {
                            Toast.makeText(this, "메인 화면으로 돌아가서 시선 보정을 실행하세요", Toast.LENGTH_LONG).show();
                            finish();
                        }
                    } catch (Exception e) {
                        Log.e("SettingsActivity", "정밀 보정 실행 오류: " + e.getMessage(), e);
                        Toast.makeText(this, "메인 화면으로 돌아가서 시선 보정을 실행하세요", Toast.LENGTH_LONG).show();
                        finish();
                    }
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void setupCursorOffsetListeners() {
        cursorOffsetXBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateCursorOffsetTexts();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                saveSettings();
                showOffsetGuidance();
            }
        });

        cursorOffsetYBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateCursorOffsetTexts();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                saveSettings();
                showOffsetGuidance();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        currentSettings = settingsRepository.getUserSettings();
        loadSettings();

        Log.d("SettingsActivity", "설정 새로고침 - 현재 커서 오프셋: X=" +
                currentSettings.getCursorOffsetX() + ", Y=" + currentSettings.getCursorOffsetY());
        Log.d("SettingsActivity", "클릭 타이밍: " + currentSettings.getClickTiming().getDisplayName());
        Log.d("SettingsActivity", "성능 프리셋: " + currentSettings.getOneEuroFilterPreset().getDisplayName());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showOffsetGuidance() {
        float offsetX = cursorOffsetXBar.getProgress() - 50;
        float offsetY = cursorOffsetYBar.getProgress() - 50;

        if (Math.abs(offsetX) > 20 || Math.abs(offsetY) > 20) {
            Toast.makeText(this, "너무 큰 값", Toast.LENGTH_SHORT).show();
        }
    }

    private OneEuroFilterPreset getPerformancePreset() {
        int checkedId = performanceRadioGroup.getCheckedRadioButtonId();
        if (checkedId == R.id.radio_performance_stability) {
            return OneEuroFilterPreset.STABILITY;
        } else if (checkedId == R.id.radio_performance_responsive) {
            return OneEuroFilterPreset.RESPONSIVE;
        } else {
            return OneEuroFilterPreset.BALANCED_STABILITY;
        }
    }

    private UserSettings.ClickTiming getClickTiming() {
        int checkedId = clickTimingRadioGroup.getCheckedRadioButtonId();
        if (checkedId == R.id.radio_click_slow) {
            return UserSettings.ClickTiming.SLOW;
        } else {
            return UserSettings.ClickTiming.NORMAL;
        }
    }

    private void updateCursorOffsetTexts() {
        float offsetX = cursorOffsetXBar.getProgress() - 50;
        float offsetY = cursorOffsetYBar.getProgress() - 50;

        cursorOffsetXText.setText(String.format("%.0f", offsetX));
        cursorOffsetYText.setText(String.format("%.0f", offsetY));
    }

    private void saveSettings() {
        UserSettings.Builder builder = new UserSettings.Builder()
                .autoOnePointCalibrationEnabled(autoOnePointCalibrationSwitch.isChecked())
                .backgroundLearningEnabled(backgroundLearningSwitch.isChecked())
                .cursorOffsetX(cursorOffsetXBar.getProgress() - 50)
                .cursorOffsetY(cursorOffsetYBar.getProgress() - 50)
                .oneEuroFilterPreset(getPerformancePreset())
                .clickTiming(getClickTiming());

        UserSettings newSettings = builder.build();
        settingsRepository.saveUserSettings(newSettings);
        currentSettings = newSettings;

        if (GazeTrackingService.getInstance() != null) {
            GazeTrackingService.getInstance().refreshSettings();
        }
    }
}