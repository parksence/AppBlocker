# 🔒 AppBlocker — 전면 앱 잠금

**AppBlocker**는 _차단 목록에 넣은 앱이 화면 맨 앞으로 올라올 때_ 잠금 UI를 띄우는 **개인용 안드로이드 앱**입니다. 사용 기록 폴링이 기본 감시 축이고, 원하면 시스템 설정에서 접근성 서비스를 켜서 전면 전환 이벤트로 보강할 수 있습니다.

설치형 앱만 골라 막고 싶고, 권한은 설정에서 직접 켜는 흐름이면 이 프로젝트가 맞습니다.

`applicationId`: `com.hjpark.appblocker` · Kotlin · Jetpack Compose · Room · minSdk **26** · targetSdk **36**

## 권장 첫 실행

1. 앱 실행 → **사용량 접근**·**다른 앱 위에 표시**를 안내 화면에서 순서대로 허용합니다.  
2. 메인 목록에서 막을 앱의 **스위치를 켭니다** → 차단이 1개라도 있으면 **감시(포그라운드 서비스)가 자동으로 시작**됩니다(Android 13+는 알림 권한 요청 후).  
3. **모든 앱의 차단을 끄면** 감시는 알림의 **「중지」**와 같이 **자동으로 종료**됩니다.  
4. (선택) **설정 → 접근성**에서 AppBlocker 접근성을 켜면 감지가 더 안정적인 기기가 많습니다.

메인 화면에는 별도의 **「차단 시작」** 버튼이 없습니다. 스위치가 곧 감시 on/off입니다.

## 빠른 시작 (TL;DR)

```bash
git clone <이 저장소 URL>
cd AppBlocker
./gradlew installDebug
```

USB 디버깅으로 단말이 잡혀 있으면 디버그 APK가 설치됩니다. Android Studio에서 열어 Run 해도 동일합니다.

## Highlights

- **[온보딩 게이트](app/src/main/java/com/hjpark/appblocker/SetupPermissionsScreen.kt)** — 사용량·오버레이가 모두 켜질 때까지 메인 목록으로 보내지 않음. 복귀 시 `ON_RESUME`으로 자동 재확인.  
- **[스위치 ↔ 감시 자동 연동](app/src/main/java/com/hjpark/appblocker/AppListViewModel.kt)** — Room의 차단 목록 `Flow`를 구독해, **비어 있지 않게 되는 순간** `MainActivity`가 서비스 시작을 요청하고, **다시 비면** [`AppBlockerService.stop`](app/src/main/java/com/hjpark/appblocker/AppBlockerService.kt)으로 종료.  
- **[앱 목록 + 검색 + 시각적 상태](app/src/main/java/com/hjpark/appblocker/AppListScreen.kt)** — 차단 ON: 아이콘 컬러·제목 강조. OFF: 아이콘 그레이스케일(`ColorFilter`)·행 `alpha`로 비활성 느낌. `LazyColumn`에 `key`·`contentType`으로 행 단위 갱신에 유리.  
- **[강력 차단 플래그(확장용)](app/src/main/java/com/hjpark/appblocker/prefs/BlockingPreferences.kt)** — `strict_power_block`이 켜지면 이미 차단된 앱은 스위치로 끌 수 없고 자물쇠 아이콘 표시(유료 기능 대비). 기본값은 꺼짐.  
- **[차단 상태 영속화](app/src/main/java/com/hjpark/appblocker/data/)** — Room이 **활성 차단 목록** 역할. 서비스·접근성은 매 평가 시 DB에서 최신 세트를 읽음([`AppDao`](app/src/main/java/com/hjpark/appblocker/data/AppDao.kt) 주석 참고).  
- **[포그라운드 감시](app/src/main/java/com/hjpark/appblocker/AppBlockerService.kt)** — `specialUse` FGS + 알림(앱 열기·**중지**). 사용 기록 폴링으로 전면 후보 주기적 평가.  
- **[이중 감지 경로](app/src/main/java/com/hjpark/appblocker/AppBlockerAccessibilityService.kt)** — 접근성 이벤트 + 폴링이 같은 평가 파이프라인(`ForegroundBlockCoordinator`)을 공유.  
- **[잠금 표면](app/src/main/java/com/hjpark/appblocker/LockOverlayManager.kt)** — 오버레이 허용 시 오버레이, 아니면 `LockActivity` 풀스크린.  
- **[오탐 완화](app/src/main/java/com/hjpark/appblocker/LockSuppressor.kt)** — 홈으로 나간 직후 등에 잠금 스킵. 세션·스로틀로 연속 잠금 완화.

## 지금까지 만든 것

### 온보딩 · 권한

- [SetupPermissionsScreen](app/src/main/java/com/hjpark/appblocker/SetupPermissionsScreen.kt): 진행률 UI, 사용량/오버레이 카드, 설정으로 이동.  
- [PermissionHelper](app/src/main/java/com/hjpark/appblocker/PermissionHelper.kt): 사용량 접근 여부 및 가능하면 앱 전용 설정 화면으로 인텐트.  
- [OverlayPermissionHelper](app/src/main/java/com/hjpark/appblocker/OverlayPermissionHelper.kt): `SYSTEM_ALERT_WINDOW` 및 오버레이 설정 진입.  
- [MainActivity](app/src/main/java/com/hjpark/appblocker/MainActivity.kt): 권한 만족 시에만 `AppListScreen`. `AppListViewModel.autoStartBlockingRequests`를 수집해 알림 권한·`tryStartBlocking()` 처리.

### 메인 화면 · 데이터

- [AppListViewModel](app/src/main/java/com/hjpark/appblocker/AppListViewModel.kt): 설치 앱 로드, 차단 `Flow`와 결합, 검색 필터, 차단 목록 변화에 따른 서비스 시작/중지 트리거, `strict_power_block` 프리퍼런스 구독.  
- [AppRepository](app/src/main/java/com/hjpark/appblocker/data/AppRepository.kt) / [AppDao](app/src/main/java/com/hjpark/appblocker/data/AppDao.kt) / [AppDatabase](app/src/main/java/com/hjpark/appblocker/data/AppDatabase.kt): 차단 패키지 CRUD·observe.  
- [AppListScreen](app/src/main/java/com/hjpark/appblocker/AppListScreen.kt): TopAppBar, 검색 필드, 스위치 리스트(FAB·감시 안내 카드 없음).

### 차단 파이프라인

- [UsageStatsForegroundReader](app/src/main/java/com/hjpark/appblocker/UsageStatsForegroundReader.kt): 마지막 포그라운드 패키지 후보 읽기(폴링 보조).  
- [ForegroundBlockCoordinator](app/src/main/java/com/hjpark/appblocker/ForegroundBlockCoordinator.kt): `evaluateForegroundPackageCandidate` — 활성 차단 목록 매칭, `SystemPackageFilter`, 스로틀, `LockSuppressor` 후 잠금 표시.  
- [AppBlockerAccessibilityService](app/src/main/java/com/hjpark/appblocker/AppBlockerAccessibilityService.kt): 윈도우 이벤트로 동일 평가 호출.  
- [LockScreen](app/src/main/java/com/hjpark/appblocker/LockScreen.kt) / [LockActivity](app/src/main/java/com/hjpark/appblocker/LockActivity.kt): 차단 앱 아이콘·이름, 홈으로 이동.

### 알림 · 매니페스트

- 포그라운드 알림: 앱으로 이동, **중지** 액션 → `AppBlockerService`의 `ACTION_STOP`과 동일하게 동작하는 [`stop()`](app/src/main/java/com/hjpark/appblocker/AppBlockerService.kt)도 앱 코드에서 호출 가능.  
- [AndroidManifest.xml](app/src/main/AndroidManifest.xml): FGS `specialUse`, 접근성 서비스 선언, `LockActivity`, `queries`로 런처 앱 나열.

### UI 테마

- [ui/theme](app/src/main/java/com/hjpark/appblocker/ui/theme/): Material 3 컬러·타이포.

## 동작 요약 (짧게)

```
[차단 스위치 ON] ──► Room 목록 비어 있지 않음 ──► 포그라운드 감시 시작(알림 권한 등)
[마지막 스위치 OFF] ──► 목록 비움 ──► AppBlockerService.stop (알림 중지와 동일)

[사용자가 차단 앱 실행]
        │
        ▼
┌───────────────────┐     ┌─────────────────────────┐
│ UsageStats 폴링   │     │ 접근성 (설정에서 ON 시) │
│ AppBlockerService │     │ AppBlockerAccessibility │
└─────────┬─────────┘     └────────────┬────────────┘
          │                            │
          └────────────┬───────────────┘
                       ▼
          ┌────────────────────────────┐
          │ ForegroundBlockCoordinator │
          │  (Room 활성 차단 목록 조회)  │
          └────────────┬───────────────┘
                       ▼
          ┌────────────────────────────┐
          │ LockOverlay 또는 LockActivity │
          └────────────────────────────┘
```

## 핵심 서브시스템

- **Blocking session** — `ForegroundBlockCoordinator` 안의 공유 세션·스로틀 상태로 폴링/접근성이 한 경로를 씀.  
- **잠금 표면 선택** — `Settings.canDrawOverlays`에 따라 오버레이 vs 액티비티.  
- **후보 필터** — `SystemPackageFilter`: 시스템 UI·자기 패키지 등 전면 후보에서 제외.

## 소스에서 빌드 (개발)

```bash
cd AppBlocker
./gradlew :app:assembleDebug
# 또는
./gradlew installDebug
```

## 변경 기록

커밋할 때마다 **아래 표 맨 위에 한 줄**을 추가하면 됩니다.

| 날짜 | 요약 |
|------|------|
| 2026-03-30 | OpenClaw 스타일 README 및 이후 동기화. 스위치↔감시 자동 연동·`AppBlockerService.stop`·FAB 제거. 목록 시각 피드백·`BlockingPreferences`(강력 차단)·활성 차단 목록 문서화. (이전: 접근성 안내 카드 제거, 사용량 설정 package URI 등.) |
