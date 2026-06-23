# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Build debug variants
./gradlew assembleDevDebug    # Developer debug APK
./gradlew assembleProdDebug   # Production debug APK

# Build release variants
./gradlew assembleDevRelease  # Developer release APK
./gradlew assembleProdRelease # Production release APK

# Run unit tests
./gradlew test

# Run instrumentation tests
./gradlew connectedAndroidTest

# Lint
./gradlew lint
```

- JDK 21 required (Temurin recommended)
- Gradle 9.5 with AGP 9.2.1
- Two product flavors: `dev` (developer, shows DevTools, custom repos) and `prod` (production)
- Min SDK 26, Target/Compile SDK 36
- Room schemas generated to `app/schemas/`

## Architecture

**Pattern**: MVVM + Repository + Hilt DI, single-module app.

```
app/src/main/java/com/xingheyuzhuan/shiguangschedule/
├── MainActivity.kt          # Entry point, Navigation3 host
├── Navigation.kt            # All sealed interface Destination definitions
├── MyApplication.kt         # Hilt app, SyncManager + offline repo init
├── data/
│   ├── db/main/             # Room: Course, CourseTable, TimeSlot, ExamEntity, GradeEntity
│   ├── db/widget/           # Separate Room DB for widget data
│   ├── di/                  # Hilt modules (DatabaseModule, NetworkModule, DataStoreModule)
│   ├── model/               # Data classes (AppSettings, RepoInfo, ScheduleGridStyle, etc.)
│   ├── network/             # SCNU教务 scraper (OkHttp-based, custom TrustManager)
│   ├── repository/          # Repositories: AppSettings, CourseTable, School, Git, etc.
│   └── sync/                # SyncManager, WidgetDataSynchronizer
├── ui/
│   ├── campus/              # Campus services: sync, exams, grades, verification, mini program launcher
│   ├── schedule/            # Weekly schedule view (the main timetable)
│   ├── today/               # Today schedule view
│   ├── schoolselection/     # School/adapter selection for教务 import
│   ├── settings/            # All settings screens (style, time, courses, notifications, etc.)
│   ├── components/          # Shared Compose components
│   └── theme/               # Material3 theming, dynamic color
├── service/                 # CourseAlarmReceiver, CourseNotificationWorker, DndSchedulerWorker
├── widget/                  # 4 widget types: tiny, compact, double_days, list_vertical
└── tool/                    # CalendarAccountManager, GitUpdater, IcsExportTool, UpdateTool
```

### Key patterns

- **Navigation3**: All destinations defined as `@Serializable sealed interface Destination` in Navigation.kt. Navigation via `onNavigate: (Destination) -> Unit`. Four main screens (CourseSchedule, Campus, Settings, TodaySchedule) at the bottom nav level.
- **Hilt DI**: `@HiltAndroidApp` in MyApplication, `@AndroidEntryPoint` Activity/ViewModels, `@Module @InstallIn(SingletonComponent)` for DI modules.
- **Room**: Two separate databases — `MainAppDatabase` (courses, exams, grades) and `WidgetDatabase` (widget cache). DAOs accessed via Hilt.
- **Network**: SCNU教务 scraping uses a separate `@Named("scnu")` OkHttpClient with trust-all SSL (for self-signed campus certs). Cookie-based auth.
- **Data sync**: `SyncManager` (singleton) starts `WidgetDataSynchronizer` when the main DB is initialized. Also triggers `CourseNotificationWorker` and `DndSchedulerWorker` on widget data changes.
- **UI State**: ViewModels expose state via `StateFlow`, collected as Compose state.

### Resources

- `resources/` — Per-school JS adapters for教务 system scraping (正方, 超星, URP, etc.), each in its own folder with `adapters.yaml`
- `index/` — School index for offline repo
- `scripts/` — Python build scripts for school index data
- Offline repo is bundled in assets and copied to `filesDir/repo/` at app start

### Build variants

| Flavor | `applicationIdSuffix` | `HIDE_CUSTOM_REPOS` | `ENABLE_DEV_TOOLS_OPTION_IN_UI` |
|--------|----------------------|---------------------|----------------------------------|
| dev    | `.dev`               | `false`             | `true`                           |
| prod   | (none)               | `true`              | `false`                          |

### Widget types

- `tiny` (2×1) — next class info
- `compact` (4×1) — compact day schedule
- `double_days` (4×2) — today + tomorrow
- `list_vertical` (4×3) — full list view

All use native `AppWidgetProvider` with manual `RemoteViews` rendering (not Glance).
