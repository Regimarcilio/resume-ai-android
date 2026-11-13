# AGENTS.md — Operating contract for this repository

> This file is the **default** for whoever (human or agent) changes this project.
> It encodes the rules that were learned the hard way here — every item below
> corresponds to a bug that actually shipped and had to be diagnosed again.

---

## 1. Non-negotiable: verify what you changed, visually

**A change is not done when it compiles.** It is done when the screen has been
**rendered and looked at** on an emulator.

Compiling and "did not crash" are not acceptance criteria. A shipped build had
a consent screen whose checkboxes were hardcoded (`checked = true`), a 4th
option cut off-screen, and three whole features unreachable — all of it
compiled perfectly and "did not crash".

### Required loop for any UI change

```bash
# 1. Build
./gradlew :app:assembleDebug

# 2. Install on the emulator (see §3 for environment)
adb install -r dist/resume-ai-1.0.0-debug.apk

# 3. Drive the flow and capture the screen
adb shell am start -n com.resumeapp.debug/com.resumeapp.MainActivity
adb exec-out screencap -p > /tmp/shot.png     # LOOK at it

# 4. Assert on the rendered state, not on "it didn't crash"
adb shell uiautomator dump /sdcard/ui.xml
adb shell cat /sdcard/ui.xml | tr '<' '\n' | grep -oE 'text="[^"]{3,60}"'

# 5. Check for crashes
adb logcat -d | grep -cE "FATAL EXCEPTION"     # must be 0
```

Rules:

- **Look at the screenshot.** A dump of text nodes is not a visual review.
- **Walk every screen you touched**, plus the screens that depend on it.
- **Check the layout, not just the content.** Title wrapping one letter per
  line, a button under the navigation bar, a cut-off list — none of these
  show up in a crash log.
- If a screen was already broken before your change, **fix it or say so**. Do
  not report "verified" for a screen you did not open.

---

## 2. APK delivery (default)

Every APK produced by this project is copied **inside the repository**, so it
is always findable next to the sources.

| Build | Command | Output | Directory |
|---|---|---|---|
| Debug | `./gradlew apk` | `resume-ai-<version>-debug.apk` | `dist/` |
| Production | `./gradlew apkRelease` | `resume-ai-<version>-release.apk` | `release/` |

Both directories are git-ignored (build artifacts).

- The **production** build is signed with the keystore referenced by
  `keystore.properties` (git-ignored). Without that file the build still
  succeeds but produces an **unsigned** APK — check the task output for the
  `[UNSIGNED]` warning.
- **Never commit** `keystore.properties`, `*.jks` or `*.keystore`.
- Losing the keystore means you can never update an already-installed app:
  the Android requires the same signing key.

---

## 3. Local environment

The Android SDK is at `~/Android/Sdk`. A JDK is **not** on the `PATH` by
default; a portable Temurin 21 is used from `/tmp/opencode`:

```bash
export JAVA_HOME=/tmp/opencode/jdk-21.0.13+9
export ANDROID_HOME=$HOME/Android/Sdk
export PATH=$JAVA_HOME/bin:$ANDROID_SDK/platform-tools:$PATH
echo "sdk.dir=$HOME/Android/Sdk" > local.properties   # git-ignored
```

| Tool | Version |
|---|---|
| JDK | 21 (Temurin) — same as CI |
| Gradle | 8.7 (wrapper) |
| Android SDK | platform 34, build-tools 34.0.0 |
| Device | API 26+ |

Emulator (used for the visual verification of §1):

```bash
~/Android/Sdk/emulator/emulator -avd test34 -no-window -no-audio \
  -no-boot-anim -gpu swiftshader_indirect -no-snapshot -memory 2048
```

> The host may be CPU-starved (2 cores, high load): the emulator itself can
> ANR even though the app is fine. Always confirm a suspected app crash
> against `logcat` before assuming it is the app's fault.

---

## 4. Build & CI

| Command | Effect |
|---|---|
| `./gradlew apk` | Debug APK → `dist/` |
| `./gradlew apkRelease` | Production APK → `release/` |
| `./gradlew clean` | Remove build outputs |
| `./gradlew bumpVersion -PversionName=1.1.0 -PversionCode=2` | Bump the version |

CI (`.github/workflows/ci.yml`) runs on every push to `develop`/`main`:
JDK 21 → `:app:assembleDebug` → copies to `dist/` → uploads the `debug-apk`
artifact. Releases are published as GitHub Releases with the APK attached.

---

## 5. Architecture rules

```
app  ──►  features/*  ──►  core:domain  ──►  core:ai
 │                              ▲
 └──────────────────────────────┘
        core:data  ──►  core:domain
```

- `core:domain` must never depend on the Android framework — that is what
  keeps the business rules unit-testable.
- `core:domain` → `core:ai` is allowed; the reverse is **not** (cycle).
- `core:data` exposes its Android/framework dependencies as `implementation`,
  so **consumers must declare what they use directly**. A missing
  `implementation(...)` in a module shows up as a confusing
  `error.NonExistentClass` at KSP time, not as a missing-dependency error.

### Layering of responsibilities

| Module | Owns |
|---|---|
| `core:domain` | models, use cases, repository contracts |
| `core:data` | Room, DataStore, repository implementations, offline sync |
| `core:ai` | gateway client, semantic cache, guardrail, traces (pure JVM) |
| `features/*` | screens + ViewModels of one feature |
| `app` | composition root: Hilt graph, navigation, theme, launcher icon |

---

## 6. Hilt / KSP rules (every one of these caused a real crash)

1. **`@HiltViewModel` must be *applied*, not just imported.**
   A ViewModel with the import but without the annotation compiles, is not
   registered in the Dagger graph, and blows up at runtime with
   `NoSuchMethodException: XViewModel.<init> []`.
   After adding a ViewModel, verify it appears in the app's generated code:
   ```bash
   grep -rl "XViewModel" app/build/generated/
   ```
2. **Hilt 2.48 uses `javax.inject`, not `jakarta.inject`.**
3. **Imports must exist in the artifact.** `InstallIn` lives in `hilt-core`
   (`dagger.hilt.InstallIn`), not in `hilt-android`
   (`dagger.hilt.android.InstallIn` does not exist). Same for
   `dagger.hilt.components.SingletonComponent` and
   `androidx.datastore.core.DataStore` (not
   `androidx.datastore.preferences.DataStore`).
   Verify with `javap` on the cached artifact before guessing.
4. Cross-module aggregation requires
   `hilt { enableAggregatingTask = true }` **in every module that applies
   the Hilt plugin**, and `hilt-android` as `api` in the library modules.
5. `tasks.named("assembleDebug")` fails at configuration time — the AGP
   registers variant tasks lazily. Use
   `tasks.matching { }.configureEach { }`.

---

## 7. Compose rules that caused crashes

1. **Never `return` from a `@Composable` lambda.** An early `return@Box`
   unbalances the composition groups →
   `IndexOutOfBoundsException` in `ComposerImpl.exitGroup`.
   Use `if / else`.
2. **Never nest lazy containers.** A `LazyColumn` inside a `LazyColumn`
   (or inside a `Column(verticalScroll)`) receives infinite height →
   `IllegalStateException: Vertically scrollable component was measured with
   an infinity maximum height constraints`. Use a plain `Column` for the
   inner list.
3. **Edge-to-edge requires explicit insets.** `enableEdgeToEdge()` draws
   under the system bars: apply `statusBarsPadding()` / `navigationBarsPadding()`
   (or handle them in a `Scaffold`) or content and buttons end up hidden.
4. **Do not reuse a parent `NavController` in a nested `NavHost`** →
   `ViewModelStore should be set before setGraph call`. A nested flow needs
   its own `rememberNavController()`.
5. **Routes of destinations inside a nested graph are short**
   (`templates`), not concatenated (`app/templates`). The concatenated form
   throws `IllegalArgumentException: Navigation destination ... cannot be
   found`.
6. A `weight(1f)` child next to fixed-width siblings can be squeezed to
   zero width, making the text wrap one character per line. Give the text
   column a `widthIn(min = ...)` and stack the actions vertically.

---

## 8. Language & naming

- The UI is **pt-BR**. Section titles come from
  `SectionType.displayTitle` (in `core:domain`) — never from
  `enum.name`, which yields English ("Experience").
- Comments and commit messages in this repository are in pt-BR or English,
   consistently within a file.

---

## 9. Git

- Branch: `develop` (develop-first). Never commit straight to `main`.
- Conventional Commits: `fix(scope): ...`, `feat(scope): ...`,
  `docs(scope): ...`, `chore(scope): ...`, `refactor(scope): ...`.
- Build outputs (`dist/`, `release/`) and signing secrets are never committed.

---

## 10. Definition of Done

A change is done when:

1. `./gradlew apk` succeeds **and** the APK lands in `dist/`.
2. The affected screens were **opened on the emulator** and the screenshots
   actually inspected.
3. `adb logcat` shows **zero** `FATAL EXCEPTION` for the flow that was
   exercised.
4. The CI is green.
5. Anything left as a known limitation is written down — in the README's
   *Known limitations* section and in the release notes — not silently
   dropped.
