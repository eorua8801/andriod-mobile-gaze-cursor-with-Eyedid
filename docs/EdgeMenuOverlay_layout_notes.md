# EdgeMenuOverlay 레이아웃 수정 노트

버튼 배치 문제 해결을 위한 수정 제안 사항입니다.

## 1. 메뉴 중심점 수정 (화면 1/4 → 중앙)

```java
private float[] getMenuCenter() {
    float centerX, centerY;

    if (corner == Corner.LEFT_TOP) {
        centerX = 0f;
        centerY = getHeight() / 2f; // 1/4f → 1/2f로 변경
    } else {
        centerX = getWidth();
        centerY = getHeight() / 2f; // 1/4f → 1/2f로 변경
    }

    return new float[]{centerX, centerY};
}
```

## 2. 배경 중심점도 동일하게 수정

```java
private float[] getBackgroundCenter() {
    if (corner == Corner.LEFT_TOP) {
        return new float[]{0f, getHeight() / 2f};
    } else {
        return new float[]{getWidth(), getHeight() / 2f};
    }
}
```

## 3. 버튼 배치 각도 조정 (2시~10시 범위)

```java
private AngleData calculatePreciseButtonPosition(int buttonIndex) {
    if (corner == Corner.LEFT_TOP) {
        switch (buttonIndex) {
            case 0: return new AngleData(-30f, 0.866f, -0.5f);    // 2시 방향
            case 1: return new AngleData(0f, 1f, 0f);             // 3시 방향
            case 2: return new AngleData(30f, 0.866f, 0.5f);      // 4시 방향
            case 3: return new AngleData(60f, 0.5f, 0.866f);      // 5시 방향
            default: return new AngleData(0f, 1f, 0f);
        }
    } else {
        switch (buttonIndex) {
            case 0: return new AngleData(-150f, -0.866f, -0.5f);  // 10시 방향
            case 1: return new AngleData(-180f, -1f, 0f);         // 9시 방향
            case 2: return new AngleData(-210f, -0.866f, 0.5f);   // 8시 방향
            case 3: return new AngleData(-240f, -0.5f, 0.866f);   // 7시 방향
            default: return new AngleData(180f, -1f, 0f);
        }
    }
}
```

## 추가 제안

- 메뉴 반지름을 `80f → 70f`로 줄여서 버튼들이 화면 안에 잘 들어오도록 조정
- 메뉴 위치를 화면 높이의 45% 지점으로 미세 조정 가능: `getHeight() * 0.45f`
