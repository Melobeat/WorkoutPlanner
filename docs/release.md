# Release Guide

## Build Commands

```bash
./gradlew assembleDebug        # Debug APK, no R8
./gradlew assembleRelease      # Release APK, R8 full mode active
./gradlew :app:lintDebug       # Lint checks
./gradlew :app:testDebugUnitTest  # Unit tests
./gradlew connectedAndroidTest # Instrumented tests (device/emulator required)
```

## R8 Full Mode

`android.enableR8.fullMode=true` in `gradle.properties`.

`@Serializable` classes survive via the Kotlin serialization plugin (`kotlin-serialization` applied at app level). `proguard-rules.pro` is essentially empty — do not add manual keeps for serialization.

## Version Bumping

Edit `app/build.gradle.kts`:
- `versionCode` — integer, increment by 1 each release
- `versionName` — semantic version string (e.g., `"1.2.0"`)

## Git Flow Release Process

1. Create release branch from `develop`: `git checkout -b release/1.2.0 develop`
2. Bump version in `build.gradle.kts`
3. Stabilize: fix bugs, no new features
4. Merge to `main`: `git checkout main && git merge --no-ff release/1.2.0`
5. Tag: `git tag -a v1.2.0 -m "Release 1.2.0"`
6. Push tag: `git push origin v1.2.0`
7. Merge back to `develop`: `git checkout develop && git merge --no-ff release/1.2.0`
8. Delete release branch

## Hotfix Process

1. Branch from `main`: `git checkout -b hotfix/fix-name main`
2. Fix, bump patch version
3. Merge to `main` + tag
4. Merge back to `develop`

## Play Store (if published)

- Generate signed AAB: `./gradlew bundleRelease`
- Signing config: check `build.gradle.kts` for `signingConfigs` block
- Upload AAB to Google Play Console
- Update release notes with version changelog
