# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

**始终使用简体中文** 与用户交流，包括解释、代码注释、commit 信息等。

## Build & Run

```bash
# Build the project (macOS/Linux)
./gradlew assembleDebug

# Build the project (Windows)
gradlew.bat assembleDebug

# Run unit tests (JVM, no device needed)
./gradlew test

# Run a single unit test class
./gradlew test --tests "com.deepnews.app.ExampleUnitTest"

# Run instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Clean build
./gradlew clean
```

## 远程仓库

- **阿里云效 Codeup**: https://codeup.aliyun.com/69c4a166405bafb07e124740/news.git
- **GitHub 备份**: https://github.com/prayyu/DeepNews.git

## Architecture

This is a single-module Android app using **Java** (not Kotlin). The project uses Gradle with Kotlin DSL and a version catalog.

- **Package**: `com.deepnews.app`
- **API levels**: minSdk 26 (Android 8.0), targetSdk/compileSdk 36
- **Java version**: 11
- **Theme**: Material3 DayNight (defined in `res/values/themes.xml`)

### Source layout

| Directory | Purpose |
|---|---|
| `app/src/main/java/` | App source code |
| `app/src/test/java/` | Unit tests (JVM-hosted, JUnit 4) |
| `app/src/androidTest/java/` | Instrumented tests (device-hosted, Espresso + JUnit 4) |
| `app/src/main/res/` | Resources (layouts, strings, drawables, etc.) |

### Dependencies (version catalog at `gradle/libs.versions.toml`)

Libraries are declared in `gradle/libs.versions.toml` and referenced in `app/build.gradle.kts` via `libs.<alias>` (e.g., `libs.appcompat`, `libs.material`). To add a dependency, declare it in the TOML file first, then add it to `app/build.gradle.kts`.

### Key patterns

- The single activity `MainActivity` uses `EdgeToEdge` for edge-to-edge display with system bar insets applied via `ViewCompat.setOnApplyWindowInsetsListener`.
- Layouts use `ConstraintLayout` (`activity_main.xml`).
- Testing uses JUnit 4 (not 5) — instrumented tests are annotated with `@RunWith(AndroidJUnit4.class)`.
