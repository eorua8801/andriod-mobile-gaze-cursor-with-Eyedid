# 🔧 EyeID SDK 기반 시선 추적 시스템 - 기술 문서

[![Android](https://img.shields.io/badge/Android-10%2B-green.svg)](https://developer.android.com/about/versions/10)
[![EyeID SDK](https://img.shields.io/badge/EyeID%20SDK-Latest-blue.svg)](https://docs.eyedid.ai/)

> **완전한 기술 구현 가이드 - 비주얼캠프 개발팀을 위한 상세 문서**

---

## 📋 목차

1. [시스템 아키텍처](#1-시스템-아키텍처)
2. [EyeID SDK 연동](#2-eyeid-sdk-연동)
3. [핵심 컴포넌트 상세](#3-핵심-컴포넌트-상세)
4. [알고리즘 구현](#4-알고리즘-구현)
5. [성능 최적화](#5-성능-최적화)
6. [확장 개발 가이드](#6-확장-개발-가이드)
7. [문제 해결](#7-문제-해결)

---

## 1. 시스템 아키텍처

### 1.1 전체 아키텍처 개요

```
┌─────────────────────────────────────────────────┐
│                 UI Layer                        │
│  ┌─────────────────────────────────────────────┤
│  │ MainActivity.java (설정 관리)                │
│  │ SettingsActivity.java (상세 설정)           │
│  │ CalibrationViewer.java (캘리브레이션 UI)    │
│  │ OverlayCursorView.java (시선 커서 표시)     │
│  └─────────────────────────────────────────────┤
├─────────────────────────────────────────────────┤
│              Domain Layer                       │
│  ┌─────────────┬─────────────┬─────────────┐    │
│  │   Filter    │ Interaction │Performance  │    │
│  │   Manager   │   Engine    │  Monitor    │    │
│  └─────────────┴─────────────┴─────────────┘    │
├─────────────────────────────────────────────────┤
│                Data Layer                       │
│  ┌─────────────────────┬─────────────────────┐  │
│  │  EyeTracking        │     Settings        │  │
│  │  Repository         │   Repository        │  │
│  └─────────────────────┴─────────────────────┘  │
├─────────────────────────────────────────────────┤
│               Service Layer                     │
│  ┌─────────────────────┬─────────────────────┐  │
│  │ GazeTrackingService │MyAccessibilityService│ │
│  │    (핵심 엔진)        │   (시스템 제어)       │  │
│  └─────────────────────┴─────────────────────┘  │
└─────────────────────────────────────────────────┘
```

### 1.2 서비스 우선 아키텍처

**설계 철학**: 서비스가 주도하고, UI는 보조 역할

```java
// 서비스가 UI 없이 독립적으로 작동
public class GazeTrackingService extends Service {
    private EyeTrackingManager eyeTrackingManager;
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 초기화 및 캘리브레이션 관리
        initializeTracking();
        startContinuousTracking();
        return START_STICKY;
    }
    
    // 커서 움직임이 UI와 독립적으로 작동
    private void updateGazeCursor(GazePoint point) {
        // WindowManager를 통해 직접 커서 업데이트
        overlayManager.updateCursorPosition(point);
    }
}

// UI는 서비스의 상태만 표시
public class MainActivity extends AppCompatActivity {
    private GazeTrackingService trackingService;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 서비스 시작
        startService(new Intent(this, GazeTrackingService.class));
        // UI만 업데이트
        displayServiceStatus();
    }
}
```

---

## 2. EyeID SDK 연동

### 2.1 EyeID SDK 초기화 및 설정

```java
public class EyeIDInitializer {
    private static final String TAG = "EyeIDInit";
    
    // EyeID SDK 라이선스 설정
    public static void initializeEyeID(Context context) {
        try {
            // SDK 초기화 - 라이선스는 보안 설정을 통해 관리
            EyeIDSDK.initialize(context);
            
            // 카메라 권한 확인
            if (ContextCompat.checkSelfPermission(context,
                    Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                EyeIDSDK.startTracking();
            }
        } catch (Exception e) {
            Log.e(TAG, "EyeID initialization failed", e);
        }
    }
    
    // 콜백 핸들러
    public static EyeIDSDK.GazeListener createGazeListener() {
        return gazeData -> {
            // 시선 데이터 처리
            processGazeData(gazeData);
        };
    }
}
```

### 2.2 시선 데이터 수신 및 처리

```java
public class GazeDataProcessor {
    private static final String TAG = "GazeProcessor";
    private EyeTrackingRepository repository;
    
    // 시선 데이터 콜백 처리
    public void onGazeUpdate(GazeData gazeData) {
        try {
            // 데이터 정제
            GazePoint cleanedPoint = filterGazePoint(gazeData);
            
            // 이상치 감지
            if (isOutlier(cleanedPoint)) {
                Log.w(TAG, "Outlier detected: " + cleanedPoint);
                return;
            }
            
            // 데이터 저장
            repository.saveGazePoint(cleanedPoint);
            
        } catch (Exception e) {
            Log.e(TAG, "Gaze processing error", e);
        }
    }
    
    // 필터링 로직
    private GazePoint filterGazePoint(GazeData rawData) {
        // 칼만 필터 적용
        return kalmanFilter.filter(new GazePoint(
            rawData.getX(),
            rawData.getY(),
            rawData.getTimestamp()
        ));
    }
    
    // 이상치 감지
    private boolean isOutlier(GazePoint point) {
        return Math.abs(point.getVelocity()) > OUTLIER_THRESHOLD;
    }
}
```

### 2.3 EyeID SDK 에러 핸들링

```java
public class EyeIDErrorHandler {
    private static final String TAG = "EyeIDError";
    
    public static void handleSDKError(Exception e) {
        if (e instanceof CameraPermissionException) {
            Log.e(TAG, "Camera permission denied");
            // 권한 요청 로직
        } else if (e instanceof EyeTrackingException) {
            Log.e(TAG, "Eye tracking failed", e);
            // 재시도 로직
        } else if (e instanceof LicenseException) {
            Log.e(TAG, "License verification failed", e);
            // 라이선스 처리
        }
    }
}
```

---

## 3. 핵심 컴포넌트 상세

### 3.1 FilterManager (필터링 관리자)

**목표**: 노이즈 제거 및 데이터 정확도 향상

```java
public class FilterManager {
    private final KalmanFilter kalmanFilter;
    private final MedianFilter medianFilter;
    private final VelocityFilter velocityFilter;
    
    private static final int WINDOW_SIZE = 5;
    private static final double VELOCITY_THRESHOLD = 50.0;
    
    public FilterManager() {
        this.kalmanFilter = new KalmanFilter();
        this.medianFilter = new MedianFilter(WINDOW_SIZE);
        this.velocityFilter = new VelocityFilter(VELOCITY_THRESHOLD);
    }
    
    // 3단계 필터링
    public GazePoint applyFilters(GazePoint rawPoint) {
        // 1단계: 중앙값 필터 (노이즈 감소)
        GazePoint medianFiltered = medianFilter.apply(rawPoint);
        
        // 2단계: 칼만 필터 (예측 및 부드러움)
        GazePoint kalmanFiltered = kalmanFilter.filter(medianFiltered);
        
        // 3단계: 속도 필터 (이상치 감지)
        GazePoint velocityFiltered = velocityFilter.apply(kalmanFiltered);
        
        return velocityFiltered;
    }
}

// 칼만 필터 구현
public class KalmanFilter {
    private double x, y;
    private double vx, vy;
    private double[][] P; // 공분산 행렬
    
    public GazePoint filter(GazePoint rawPoint) {
        // 예측 단계
        predict();
        
        // 갱신 단계
        update(rawPoint);
        
        return new GazePoint(x, y, rawPoint.getTimestamp());
    }
    
    private void predict() {
        // 칼만 필터 예측
        x += vx;
        y += vy;
        // 공분산 업데이트
        updateCovariance();
    }
    
    private void update(GazePoint measurement) {
        // 칼만 이득 계산
        double K = calculateKalmanGain();
        // 상태 업데이트
        x += K * (measurement.getX() - x);
        y += K * (measurement.getY() - y);
    }
    
    private double calculateKalmanGain() {
        // 칼만 이득 계산식
        return 0.5; // 간단한 예시
    }
    
    private void updateCovariance() {
        // 공분산 행렬 업데이트
    }
}

// 중앙값 필터
public class MedianFilter {
    private final int windowSize;
    private final Queue<Double> xWindow = new LinkedList<>();
    private final Queue<Double> yWindow = new LinkedList<>();
    
    public MedianFilter(int windowSize) {
        this.windowSize = windowSize;
    }
    
    public GazePoint apply(GazePoint point) {
        xWindow.offer(point.getX());
        yWindow.offer(point.getY());
        
        if (xWindow.size() > windowSize) {
            xWindow.poll();
            yWindow.poll();
        }
        
        return new GazePoint(
            calculateMedian(new ArrayList<>(xWindow)),
            calculateMedian(new ArrayList<>(yWindow)),
            point.getTimestamp()
        );
    }
    
    private double calculateMedian(List<Double> values) {
        Collections.sort(values);
        return values.get(values.size() / 2);
    }
}

// 속도 기반 필터
public class VelocityFilter {
    private final double velocityThreshold;
    private GazePoint lastPoint;
    
    public VelocityFilter(double threshold) {
        this.velocityThreshold = threshold;
    }
    
    public GazePoint apply(GazePoint point) {
        if (lastPoint == null) {
            lastPoint = point;
            return point;
        }
        
        double velocity = calculateVelocity(lastPoint, point);
        if (velocity > velocityThreshold) {
            return lastPoint; // 이상치 무시
        }
        
        lastPoint = point;
        return point;
    }
    
    private double calculateVelocity(GazePoint p1, GazePoint p2) {
        double dx = p2.getX() - p1.getX();
        double dy = p2.getY() - p1.getY();
        double dt = (p2.getTimestamp() - p1.getTimestamp()) / 1000.0;
        
        if (dt == 0) return 0;
        return Math.sqrt(dx * dx + dy * dy) / dt;
    }
}
```

### 3.2 InteractionEngine (상호작용 엔진)

**목표**: 시선 기반 UI 상호작용 처리

```java
public class InteractionEngine {
    private static final String TAG = "InteractionEngine";
    private static final long GAZE_DWELL_TIME = 500; // ms
    
    private final FilterManager filterManager;
    private final PerformanceMonitor performanceMonitor;
    private final GazeInteractionListener listener;
    
    private GazePoint currentGaze;
    private long gazeStartTime;
    private InteractiveElement focusedElement;
    
    public InteractionEngine(GazeInteractionListener listener) {
        this.filterManager = new FilterManager();
        this.performanceMonitor = new PerformanceMonitor();
        this.listener = listener;
    }
    
    // 시선 점 업데이트
    public void updateGaze(GazePoint rawPoint) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 필터링
            GazePoint filteredPoint = filterManager.applyFilters(rawPoint);
            currentGaze = filteredPoint;
            
            // UI 요소 탐지
            InteractiveElement element = detectElement(filteredPoint);
            
            // 상호작용 처리
            if (element != null && !element.equals(focusedElement)) {
                onElementFocusChanged(focusedElement, element);
                focusedElement = element;
                gazeStartTime = System.currentTimeMillis();
            } else if (element != null) {
                checkDwellTime();
            }
            
            // 성능 모니터링
            long processingTime = System.currentTimeMillis() - startTime;
            performanceMonitor.recordGazeUpdateTime(processingTime);
            
        } catch (Exception e) {
            Log.e(TAG, "Gaze update error", e);
        }
    }
    
    // UI 요소 탐지
    private InteractiveElement detectElement(GazePoint point) {
        // 화면상의 모든 대화형 요소 확인
        // (실제 구현에서는 접근성 API 사용)
        return null; // 간단한 예시
    }
    
    // Dwell time 체크
    private void checkDwellTime() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - gazeStartTime >= GAZE_DWELL_TIME) {
            if (focusedElement != null) {
                listener.onDwellTimeReached(focusedElement);
            }
        }
    }
    
    // 포커스 변경 콜백
    private void onElementFocusChanged(InteractiveElement oldElement, 
                                       InteractiveElement newElement) {
        if (oldElement != null) {
            listener.onElementFocusLost(oldElement);
        }
        if (newElement != null) {
            listener.onElementFocused(newElement);
        }
    }
}

// 리스너 인터페이스
public interface GazeInteractionListener {
    void onElementFocused(InteractiveElement element);
    void onElementFocusLost(InteractiveElement element);
    void onDwellTimeReached(InteractiveElement element);
}

// 상호작용 요소 정의
public class InteractiveElement {
    private int id;
    private String label;
    private Rect bounds;
    private int type; // BUTTON, TEXT_FIELD, etc.
    
    public InteractiveElement(int id, String label, Rect bounds, int type) {
        this.id = id;
        this.label = label;
        this.bounds = bounds;
        this.type = type;
    }
    
    public boolean contains(GazePoint point) {
        return bounds.contains((int)point.getX(), (int)point.getY());
    }
    
    // Getter methods
    public int getId() { return id; }
    public String getLabel() { return label; }
    public Rect getBounds() { return bounds; }
    public int getType() { return type; }
}
```

### 3.3 PerformanceMonitor (성능 모니터)

**목표**: 실시간 성능 모니터링 및 최적화

```java
public class PerformanceMonitor {
    private static final String TAG = "PerformanceMonitor";
    private static final int SAMPLE_WINDOW = 100;
    
    private final Queue<Long> gazeUpdateTimes = new LinkedList<>();
    private final Queue<Long> filterTimes = new LinkedList<>();
    private final Queue<Long> renderTimes = new LinkedList<>();
    
    // 성능 지표
    private double averageGazeUpdateTime;
    private double averageFilterTime;
    private double averageRenderTime;
    private double fps;
    
    public void recordGazeUpdateTime(long timeMs) {
        gazeUpdateTimes.offer(timeMs);
        if (gazeUpdateTimes.size() > SAMPLE_WINDOW) {
            gazeUpdateTimes.poll();
        }
        updateAverages();
    }
    
    public void recordFilterTime(long timeMs) {
        filterTimes.offer(timeMs);
        if (filterTimes.size() > SAMPLE_WINDOW) {
            filterTimes.poll();
        }
        updateAverages();
    }
    
    public void recordRenderTime(long timeMs) {
        renderTimes.offer(timeMs);
        if (renderTimes.size() > SAMPLE_WINDOW) {
            renderTimes.poll();
        }
        updateAverages();
    }
    
    private void updateAverages() {
        averageGazeUpdateTime = calculateAverage(gazeUpdateTimes);
        averageFilterTime = calculateAverage(filterTimes);
        averageRenderTime = calculateAverage(renderTimes);
        fps = calculateFPS();
        
        // 성능 경고
        if (averageGazeUpdateTime > 100) {
            Log.w(TAG, "High gaze update time: " + averageGazeUpdateTime + "ms");
        }
    }
    
    private double calculateAverage(Queue<Long> times) {
        if (times.isEmpty()) return 0;
        return times.stream().mapToLong(Long::longValue).average().orElse(0);
    }
    
    private double calculateFPS() {
        if (averageRenderTime == 0) return 0;
        return 1000.0 / averageRenderTime;
    }
    
    // 성능 리포트
    public void printPerformanceReport() {
        Log.i(TAG, String.format(
            "Performance Report\n" +
            "- Gaze Update: %.2f ms\n" +
            "- Filter: %.2f ms\n" +
            "- Render: %.2f ms\n" +
            "- FPS: %.2f",
            averageGazeUpdateTime,
            averageFilterTime,
            averageRenderTime,
            fps
        ));
    }
    
    // Getter methods
    public double getAverageGazeUpdateTime() { return averageGazeUpdateTime; }
    public double getAverageFilterTime() { return averageFilterTime; }
    public double getAverageRenderTime() { return averageRenderTime; }
    public double getFPS() { return fps; }
}
```

---

## 4. 알고리즘 구현

### 4.1 칼만 필터 상세 구현

```java
public class KalmanFilterAdvanced {
    // 상태 벡터: [x, y, vx, vy]
    private double[][] x; // 상태
    private double[][] P; // 공분산
    
    // 시스템 파라미터
    private double[][] F; // 상태 전이 행렬
    private double[][] Q; // 프로세스 노이즈
    private double[][] R; // 측정 노이즈
    private double[][] H; // 측정 행렬
    
    private double dt; // 시간 간격
    
    public KalmanFilterAdvanced(double dt) {
        this.dt = dt;
        initializeMatrices();
    }
    
    private void initializeMatrices() {
        // 상태 초기화
        x = new double[][] {{0}, {0}, {0}, {0}};
        
        // 공분산 초기화
        P = new double[][] {
            {1000, 0, 0, 0},
            {0, 1000, 0, 0},
            {0, 0, 100, 0},
            {0, 0, 0, 100}
        };
        
        // 상태 전이 행렬 (등속 모델)
        F = new double[][] {
            {1, 0, dt, 0},
            {0, 1, 0, dt},
            {0, 0, 1, 0},
            {0, 0, 0, 1}
        };
        
        // 프로세스 노이즈 (작은 값)
        Q = new double[][] {
            {0.01, 0, 0, 0},
            {0, 0.01, 0, 0},
            {0, 0, 0.01, 0},
            {0, 0, 0, 0.01}
        };
        
        // 측정 노이즈 (센서 정확도)
        R = new double[][] {
            {25, 0},
            {0, 25}
        };
        
        // 측정 행렬 (위치만 측정)
        H = new double[][] {
            {1, 0, 0, 0},
            {0, 1, 0, 0}
        };
    }
    
    public GazePoint filter(GazePoint measurement) {
        // 1. 예측 단계
        predictState();
        predictCovariance();
        
        // 2. 갱신 단계
        updateState(measurement);
        updateCovariance();
        
        return new GazePoint(x[0][0], x[1][0], measurement.getTimestamp());
    }
    
    private void predictState() {
        // x = F * x
        x = matrixMultiply(F, x);
    }
    
    private void predictCovariance() {
        // P = F * P * F' + Q
        double[][] FP = matrixMultiply(F, P);
        double[][] FPFt = matrixMultiply(FP, transpose(F));
        P = matrixAdd(FPFt, Q);
    }
    
    private void updateState(GazePoint measurement) {
        // 측정값
        double[][] z = new double[][] {{measurement.getX()}, {measurement.getY()}};
        
        // 혁신 (innovation): y = z - H * x
        double[][] Hx = matrixMultiply(H, x);
        double[][] y = matrixSubtract(z, Hx);
        
        // 혁신 공분산: S = H * P * H' + R
        double[][] HP = matrixMultiply(H, P);
        double[][] HPHt = matrixMultiply(HP, transpose(H));
        double[][] S = matrixAdd(HPHt, R);
        
        // 칼만 이득: K = P * H' * S^-1
        double[][] Ht = transpose(H);
        double[][] PHt = matrixMultiply(P, Ht);
        double[][] Sinv = inverseMatrix(S);
        double[][] K = matrixMultiply(PHt, Sinv);
        
        // 상태 갱신: x = x + K * y
        double[][] Ky = matrixMultiply(K, y);
        x = matrixAdd(x, Ky);
    }
    
    private void updateCovariance() {
        // P = (I - K * H) * P
        double[][] I = identityMatrix(4);
        double[][] KH = matrixMultiply(K, H);
        double[][] IKH = matrixSubtract(I, KH);
        P = matrixMultiply(IKH, P);
    }
    
    // 행렬 연산 헬퍼 메서드
    private double[][] matrixMultiply(double[][] a, double[][] b) {
        int n = a.length;
        int m = b[0].length;
        int k = a[0].length;
        
        double[][] result = new double[n][m];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                for (int p = 0; p < k; p++) {
                    result[i][j] += a[i][p] * b[p][j];
                }
            }
        }
        return result;
    }
    
    private double[][] matrixAdd(double[][] a, double[][] b) {
        int n = a.length;
        int m = a[0].length;
        double[][] result = new double[n][m];
        
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                result[i][j] = a[i][j] + b[i][j];
            }
        }
        return result;
    }
    
    private double[][] matrixSubtract(double[][] a, double[][] b) {
        int n = a.length;
        int m = a[0].length;
        double[][] result = new double[n][m];
        
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                result[i][j] = a[i][j] - b[i][j];
            }
        }
        return result;
    }
    
    private double[][] transpose(double[][] a) {
        int n = a.length;
        int m = a[0].length;
        double[][] result = new double[m][n];
        
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                result[i][j] = a[j][i];
            }
        }
        return result;
    }
    
    private double[][] inverseMatrix(double[][] a) {
        // 2x2 역행렬 계산
        double det = a[0][0] * a[1][1] - a[0][1] * a[1][0];
        return new double[][] {
            {a[1][1] / det, -a[0][1] / det},
            {-a[1][0] / det, a[0][0] / det}
        };
    }
    
    private double[][] identityMatrix(int n) {
        double[][] I = new double[n][n];
        for (int i = 0; i < n; i++) {
            I[i][i] = 1;
        }
        return I;
    }
}
```

### 4.2 시선 가속도 분석

```java
public class GazeAccelerationAnalyzer {
    private final Queue<GazePoint> pointBuffer = new LinkedList<>();
    private static final int BUFFER_SIZE = 5;
    
    public void analyzeAcceleration(GazePoint point) {
        pointBuffer.offer(point);
        if (pointBuffer.size() > BUFFER_SIZE) {
            pointBuffer.poll();
        }
        
        if (pointBuffer.size() >= 3) {
            calculateAcceleration();
        }
    }
    
    private void calculateAcceleration() {
        List<GazePoint> points = new ArrayList<>(pointBuffer);
        
        if (points.size() < 3) return;
        
        // 속도 계산 (velocity)
        double v1x = (points.get(1).getX() - points.get(0).getX()) / 
                     getDeltaTime(points.get(0), points.get(1));
        double v1y = (points.get(1).getY() - points.get(0).getY()) / 
                     getDeltaTime(points.get(0), points.get(1));
        
        double v2x = (points.get(2).getX() - points.get(1).getX()) / 
                     getDeltaTime(points.get(1), points.get(2));
        double v2y = (points.get(2).getY() - points.get(1).getY()) / 
                     getDeltaTime(points.get(1), points.get(2));
        
        // 가속도 계산 (acceleration)
        double ax = (v2x - v1x) / getDeltaTime(points.get(1), points.get(2));
        double ay = (v2y - v1y) / getDeltaTime(points.get(1), points.get(2));
        
        double acceleration = Math.sqrt(ax * ax + ay * ay);
    }
    
    private double getDeltaTime(GazePoint p1, GazePoint p2) {
        return (p2.getTimestamp() - p1.getTimestamp()) / 1000.0;
    }
}
```

---

## 5. 성능 최적화

### 5.1 메모리 최적화

```java
public class MemoryOptimization {
    // 1. 객체 풀링 (Object Pooling)
    public class GazePointPool {
        private final Queue<GazePoint> pool = new LinkedList<>();
        private static final int POOL_SIZE = 50;
        
        public GazePointPool() {
            for (int i = 0; i < POOL_SIZE; i++) {
                pool.offer(new GazePoint());
            }
        }
        
        public GazePoint acquire() {
            GazePoint point = pool.poll();
            return point != null ? point : new GazePoint();
        }
        
        public void release(GazePoint point) {
            point.reset();
            if (pool.size() < POOL_SIZE) {
                pool.offer(point);
            }
        }
    }
    
    // 2. 약한 참조 (Weak References)
    private final Map<String, WeakReference<Bitmap>> bitmapCache = 
        new WeakHashMap<>();
    
    // 3. 메모리 할당 최소화
    public class GazeDataCompressor {
        // 16비트 정수로 좌표 표현 (32비트에서 50% 감소)
        public class CompressedGazePoint {
            short x, y;
            int timestamp;
        }
    }
}
```

### 5.2 렌더링 성능 최적화

```java
public class RenderingOptimization {
    // 1. 더블 버퍼링 (Double Buffering)
    private Bitmap frontBuffer, backBuffer;
    private Canvas backCanvas;
    
    public void startRendering() {
        // 백버퍼에 그리기
        backCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        drawGazeCursor(backCanvas);
        
        // 버퍼 스왑
        swapBuffers();
    }
    
    private void swapBuffers() {
        Bitmap temp = frontBuffer;
        frontBuffer = backBuffer;
        backBuffer = temp;
    }
    
    // 2. 뷰 계층 최소화
    // 최적화 전: 많은 중첩된 ViewGroup
    // 최적화 후: 커스텀 View에서 직접 그리기
    public class OptimizedGazeCursorView extends View {
        private Paint paint;
        private float cursorX, cursorY;
        
        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            canvas.drawCircle(cursorX, cursorY, 20, paint);
        }
    }
    
    // 3. 하드웨어 가속 활용
    public void enableHardwareAcceleration(View view) {
        view.setLayerType(View.LAYER_TYPE_HARDWARE, null);
    }
    
    // 4. 프레임 속도 동기화
    public class FrameRateSynchronizer {
        private static final int TARGET_FPS = 60;
        private static final long FRAME_TIME = 1000 / TARGET_FPS; // 약 16ms
        
        private long lastFrameTime = 0;
        
        public void synchronizeFrame() {
            long currentTime = System.currentTimeMillis();
            long deltaTime = currentTime - lastFrameTime;
            
            if (deltaTime < FRAME_TIME) {
                try {
                    Thread.sleep(FRAME_TIME - deltaTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            
            lastFrameTime = System.currentTimeMillis();
        }
    }
}
```

### 5.3 배터리 최적화

```java
public class BatteryOptimization {
    private WakeLock wakeLock;
    private SensorManager sensorManager;
    
    // 1. Wake Lock 효율적 관리
    public void acquireWakeLock(Context context) {
        PowerManager powerManager = (PowerManager) 
            context.getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "GazeTracking:WakeLock"
        );
        wakeLock.acquire();
    }
    
    public void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
    }
    
    // 2. 센서 샘플링 최적화
    public void optimizeSensorSampling(Context context) {
        sensorManager = (SensorManager) 
            context.getSystemService(Context.SENSOR_SERVICE);
        
        // 자이로스코프: 낮은 레이트 사용
        Sensor gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        sensorManager.registerListener(
            listener,
            gyroscope,
            SensorManager.SENSOR_DELAY_GAME // 20ms, 50Hz
        );
    }
    
    // 3. 백그라운드 작업 최적화
    public void scheduleBackgroundTasks(Context context) {
        // WorkManager로 배터리 효율적 스케줄링
        PeriodicWorkRequest dataCleanup =
            new PeriodicWorkRequest.Builder(
                DataCleanupWorker.class,
                6, TimeUnit.HOURS
            ).build();
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "data_cleanup",
            ExistingPeriodicWorkPolicy.KEEP,
            dataCleanup
        );
    }
}
```

---

## 6. 확장 개발 가이드

### 6.1 새로운 필터 추가하기

```java
// 1. 필터 인터페이스 정의
public interface GazeFilter {
    GazePoint apply(GazePoint input);
    void reset();
}

// 2. 새로운 필터 구현
public class BilateralFilter implements GazeFilter {
    private static final double SIGMA_SPACE = 20;
    private static final double SIGMA_RANGE = 10;
    private final Queue<GazePoint> window = new LinkedList<>();
    
    @Override
    public GazePoint apply(GazePoint input) {
        window.offer(input);
        if (window.size() > 5) {
            window.poll();
        }
        
        if (window.size() < 5) {
            return input;
        }
        
        List<GazePoint> points = new ArrayList<>(window);
        GazePoint center = points.get(2);
        
        double sumWeights = 0;
        double sumX = 0, sumY = 0;
        
        for (GazePoint point : points) {
            double spaceDist = calculateDistance(center, point);
            double rangeDist = calculateDistance(center, point);
            
            double weight = Math.exp(-(spaceDist * spaceDist) / (2 * SIGMA_SPACE * SIGMA_SPACE)) *
                           Math.exp(-(rangeDist * rangeDist) / (2 * SIGMA_RANGE * SIGMA_RANGE));
            
            sumWeights += weight;
            sumX += weight * point.getX();
            sumY += weight * point.getY();
        }
        
        return new GazePoint(sumX / sumWeights, sumY / sumWeights, input.getTimestamp());
    }
    
    @Override
    public void reset() {
        window.clear();
    }
    
    private double calculateDistance(GazePoint p1, GazePoint p2) {
        double dx = p1.getX() - p2.getX();
        double dy = p1.getY() - p2.getY();
        return Math.sqrt(dx * dx + dy * dy);
    }
}

// 3. FilterManager에 추가
public void addCustomFilter(GazeFilter filter) {
    customFilters.add(filter);
}

public GazePoint applyAllFilters(GazePoint rawPoint) {
    GazePoint point = rawPoint;
    
    // 기본 필터
    point = medianFilter.apply(point);
    point = kalmanFilter.filter(point);
    
    // 커스텀 필터
    for (GazeFilter filter : customFilters) {
        point = filter.apply(point);
    }
    
    return point;
}
```

### 6.2 새로운 상호작용 모드 추가하기

```java
// 1. 상호작용 모드 인터페이스
public interface InteractionMode {
    void onGazeUpdate(GazePoint point);
    void onDwellTimeReached(InteractiveElement element);
    void onElementFocused(InteractiveElement element);
    void onElementFocusLost(InteractiveElement element);
}

// 2. 새로운 모드 구현: Gaze Gesture 모드
public class GazeGestureMode implements InteractionMode {
    private static final int SWIPE_THRESHOLD = 100;
    private static final long SWIPE_DURATION = 500;
    
    private GazePoint startPoint;
    private long startTime;
    private List<GazePoint> gestureTrail = new ArrayList<>();
    
    @Override
    public void onGazeUpdate(GazePoint point) {
        if (startPoint == null) {
            startPoint = point;
            startTime = System.currentTimeMillis();
        }
        
        gestureTrail.add(point);
        
        if (System.currentTimeMillis() - startTime > SWIPE_DURATION) {
            detectGesture();
            reset();
        }
    }
    
    private void detectGesture() {
        if (gestureTrail.size() < 3) return;
        
        GazePoint first = gestureTrail.get(0);
        GazePoint last = gestureTrail.get(gestureTrail.size() - 1);
        
        double dx = last.getX() - first.getX();
        double dy = last.getY() - first.getY();
        double distance = Math.sqrt(dx * dx + dy * dy);
        
        if (distance > SWIPE_THRESHOLD) {
            String direction = detectDirection(dx, dy);
            // 제스처 처리
        }
    }
    
    private String detectDirection(double dx, double dy) {
        if (Math.abs(dx) > Math.abs(dy)) {
            return dx > 0 ? "right" : "left";
        } else {
            return dy > 0 ? "down" : "up";
        }
    }
    
    private void reset() {
        startPoint = null;
        gestureTrail.clear();
    }
    
    @Override
    public void onDwellTimeReached(InteractiveElement element) {}
    
    @Override
    public void onElementFocused(InteractiveElement element) {}
    
    @Override
    public void onElementFocusLost(InteractiveElement element) {}
}

// 3. InteractionEngine에 모드 전환 기능 추가
public class InteractionEngineExtended {
    private InteractionMode currentMode;
    
    public void setInteractionMode(InteractionMode mode) {
        this.currentMode = mode;
    }
    
    public void updateGaze(GazePoint point) {
        if (currentMode != null) {
            currentMode.onGazeUpdate(point);
        }
    }
}
```

### 6.3 데이터 분석 및 시각화

```java
public class GazeDataAnalysis {
    // 1. 히트맵 생성
    public class HeatmapGenerator {
        private int[] heatmapData;
        private int screenWidth, screenHeight;
        private static final int CELL_SIZE = 10;
        
        public HeatmapGenerator(int width, int height) {
            this.screenWidth = width;
            this.screenHeight = height;
            int cellsX = width / CELL_SIZE;
            int cellsY = height / CELL_SIZE;
            heatmapData = new int[cellsX * cellsY];
        }
        
        public void addGazePoint(GazePoint point) {
            int cellX = (int) point.getX() / CELL_SIZE;
            int cellY = (int) point.getY() / CELL_SIZE;
            
            if (cellX >= 0 && cellX < screenWidth / CELL_SIZE &&
                cellY >= 0 && cellY < screenHeight / CELL_SIZE) {
                int index = cellY * (screenWidth / CELL_SIZE) + cellX;
                heatmapData[index]++;
            }
        }
        
        public int[] getHeatmapData() {
            return heatmapData;
        }
    }
    
    // 2. 통계 분석
    public class GazeStatistics {
        private List<GazePoint> points = new ArrayList<>();
        
        public void addPoint(GazePoint point) {
            points.add(point);
        }
        
        public double getAverageX() {
            return points.stream()
                .mapToDouble(GazePoint::getX)
                .average()
                .orElse(0);
        }
        
        public double getAverageY() {
            return points.stream()
                .mapToDouble(GazePoint::getY)
                .average()
                .orElse(0);
        }
        
        public double getStandardDeviationX() {
            double mean = getAverageX();
            double variance = points.stream()
                .mapToDouble(p -> Math.pow(p.getX() - mean, 2))
                .average()
                .orElse(0);
            return Math.sqrt(variance);
        }
    }
}
```

---

## 7. 문제 해결

### 7.1 일반적인 문제 및 해결책

#### 1. 높은 시선 지터 (High Gaze Jitter)

**증상**: 시선 커서가 흔들림
**원인**: 카메라 해상도 부족, 조명 불량, 필터 설정 부정확

```java
public class JitterSolution {
    // 칼만 필터 파라미터 조정
    private void tuneKalmanFilter() {
        // Q (프로세스 노이즈) 감소 -> 더 부드러운 추적
        kalmanFilter.setProcessNoise(0.001);
        
        // R (측정 노이즈) 증가 -> 센서 입력 덜 신뢰
        kalmanFilter.setMeasurementNoise(50);
    }
    
    // 또는 더 강력한 중앙값 필터 사용
    private void increaseMedianFilterWindow() {
        medianFilter = new MedianFilter(10); // 기본: 5
    }
}
```

#### 2. 느린 응답 시간 (Slow Response)

**증상**: 시선이 움직여도 커서가 늦게 따라옴
**원인**: 과도한 필터링, 높은 지연시간

```java
public class ResponseTimeSolution {
    private void optimizeLatency() {
        // 1. 필터 개수 최소화
        // 필요한 필터만 적용
        
        // 2. 칼만 필터 Q 증가 (더 신뢰)
        kalmanFilter.setProcessNoise(0.1);
        
        // 3. 렌더링 최적화
        enableHardwareAcceleration();
        
        // 4. 프레임 드롭 방지
        setHighPriority();
    }
}
```

#### 3. 배터리 과다 소비 (Excessive Battery Drain)

**증상**: 앱 사용 후 배터리가 빠르게 감소
**원인**: 높은 샘플링 레이트, 연속 화면 켜짐

```java
public class BatteryOptimizationSolution {
    private void reducePowerConsumption() {
        // 1. 샘플링 레이트 조정
        sensorManager.unregisterListener(listener);
        sensorManager.registerListener(
            listener,
            sensor,
            SensorManager.SENSOR_DELAY_NORMAL // 낮은 레이트
        );
        
        // 2. 화면 꺼짐 시 추적 중지
        onScreenOff();
        
        // 3. 배경 작업 최소화
        stopBackgroundAnalysis();
    }
}
```

#### 4. 메모리 누수 (Memory Leak)

**증상**: 앱 사용 시간 경과에 따라 메모리 사용량 증가

```java
public class MemoryLeakSolution {
    // 1. 리스너 등록 해제
    @Override
    protected void onDestroy() {
        sensorManager.unregisterListener(listener);
        super.onDestroy();
    }
    
    // 2. Bitmap 해제
    private void releaseBitmaps() {
        if (bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
            bitmap = null;
        }
    }
    
    // 3. 약한 참조 사용
    private Map<String, WeakReference<Object>> cache = new WeakHashMap<>();
}
```

### 7.2 성능 모니터링 및 디버깅

```java
public class PerformanceDebugger {
    private static final String TAG = "PerformanceDebug";
    
    // ANR (Application Not Responding) 감지
    public void detectANR() {
        Handler handler = new Handler();
        handler.post(() -> {
            long startTime = System.currentTimeMillis();
            
            // 메인 스레드 작업
            performHeavyComputation();
            
            long duration = System.currentTimeMillis() - startTime;
            if (duration > 5000) {
                Log.e(TAG, "Potential ANR detected: " + duration + "ms");
            }
        });
    }
    
    // 프레임 드롭 감시
    public class FrameDropMonitor {
        private long lastFrameTime = 0;
        private static final long FRAME_TIME_THRESHOLD = 33; // 30 FPS
        
        public void onFrameRendered() {
            long currentTime = System.currentTimeMillis();
            if (lastFrameTime != 0) {
                long frameTime = currentTime - lastFrameTime;
                if (frameTime > FRAME_TIME_THRESHOLD) {
                    Log.w(TAG, "Frame drop detected: " + frameTime + "ms");
                }
            }
            lastFrameTime = currentTime;
        }
    }
    
    // 메모리 사용량 로깅
    public void logMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long maxMemory = runtime.maxMemory();
        
        Log.i(TAG, String.format(
            "Memory Usage: %d / %d MB",
            usedMemory / 1024 / 1024,
            maxMemory / 1024 / 1024
        ));
    }
}
```

### 7.3 테스트 및 검증

```java
public class TestingFramework {
    // 1. 단위 테스트
    @Test
    public void testKalmanFilter() {
        KalmanFilter filter = new KalmanFilter();
        GazePoint input = new GazePoint(100, 100, System.currentTimeMillis());
        GazePoint output = filter.filter(input);
        
        assertEquals(100, output.getX(), 1.0); // 오차 범위 1px
        assertEquals(100, output.getY(), 1.0);
    }
    
    // 2. 성능 벤치마크
    @Test
    public void benchmarkFilterPerformance() {
        FilterManager filterManager = new FilterManager();
        GazePoint point = new GazePoint(0, 0, 0);
        
        long startTime = System.nanoTime();
        for (int i = 0; i < 10000; i++) {
            filterManager.applyFilters(point);
        }
        long duration = System.nanoTime() - startTime;
        
        double avgTime = duration / 10000.0 / 1000000.0; // ms
        assertTrue("Filter takes too long: " + avgTime + "ms", avgTime < 1.0);
    }
    
    // 3. UI 테스트
    @Test
    public void testGazeCursorMovement() {
        onView(withId(R.id.gaze_cursor))
            .check(matches(isDisplayed()));
        
        // 시선 이동 시뮬레이션
        simulateGazePoint(100, 100);
        
        // 커서 위치 확인
        onView(withId(R.id.gaze_cursor))
            .check(matches(isAtPosition(100, 100)));
    }
}
```

---

## 추가 리소스

### 참고 문서 및 링크

- **EyeID SDK 공식 문서**: https://docs.eyedid.ai/
- **Android 개발자 가이드**: https://developer.android.com/
- **OpenGL ES 최적화**: https://developer.android.com/games/optimize
- **칼만 필터 이론**: https://en.wikipedia.org/wiki/Kalman_filter
- **시선 추적 알고리즘**: https://ieeexplore.ieee.org/
- **모션 감지**: http://www.lifl.fr/~casiez/1euro/

---

**이 문서는 EyeID SDK 기반 시선 추적 시스템의 완전한 기술 구현 가이드입니다.**  
**비주얼캠프 개발팀의 추가 개발 및 확장에 활용하시기 바랍니다.**

---