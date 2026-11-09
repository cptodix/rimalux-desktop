# Build and Release

## Requirements

- JDK 17 or newer. The Gradle toolchain targets JVM 17.
- Internet access on the first build for Gradle dependencies.
- **Native packaging requires a full JDK with `jmods`.** The openSUSE runtime JDK in this
  environment does not include `jmods`, so `jlink` fails unless you point the build at a full
  JDK (for example Temurin 21):

  ```bash
  RIMALUX_PACKAGING_JDK=/path/to/jdk-21 ./gradlew :desktopApp:packageRpm
  ```

  The Gradle build reads `RIMALUX_PACKAGING_JDK` and uses it for `jlink`/`jpackage`.
- Linux packaging: `fakeroot`, `rpmbuild`, and (for Debian packages) `dpkg-deb`.
- Windows packaging: Windows runner with WiX Toolset 3.11 (the Compose plugin downloads it).

## Local development

```bash
./gradlew test
./gradlew :desktopApp:run
```

Run a headless render proof:

```bash
./gradlew :desktopApp:run --args="--render-test /tmp/rimalux-render-test"
```

List monitors and backend:

```bash
./gradlew :desktopApp:run --args="--list-monitors"
```

## Sync from Android

```bash
./gradlew syncAll
./gradlew test
```

The default Android checkout is `../rimalux`. Override with:

```bash
./gradlew syncAll -PandroidProjectPath=/path/to/rimalux
```

## Packaging

### Linux

```bash
# Verified on openSUSE Tumbleweed with Temurin 21:
RIMALUX_PACKAGING_JDK=/path/to/jdk-21 ./gradlew :desktopApp:packageRpm

# Debian/Ubuntu host with dpkg-deb:
RIMALUX_PACKAGING_JDK=/path/to/jdk-21 ./gradlew :desktopApp:packageDeb
```

Outputs:

```text
desktopApp/build/compose/binaries/main/deb/*.deb
desktopApp/build/compose/binaries/main/rpm/*.rpm
```

### Windows

Run on Windows:

```powershell
.\gradlew.bat :desktopApp:packageMsi
.\gradlew.bat :desktopApp:packageExe
```

Outputs:

```text
desktopApp\build\compose\binaries\main\msi\*.msi
desktopApp\build\compose\binaries\main\exe\*.exe
```

Cross-compilation is not supported by jpackage. Use the CI matrix.

## Versioning

- `packageVersion` comes from `-PappVersion` (default `0.1.0`).
- Release tags use `v<major>.<minor>.<patch>`.
- Keep the Windows `upgradeUuid` constant across releases.
- Update `CHANGELOG.md`.

## CI release workflow

The repository ships a single workflow, `.github/workflows/release.yml`, triggered manually
from the Actions tab (**Run workflow**). It never runs on push or pull request.

Inputs:

- `bump` — `patch` (default), `minor`, or `major`.
- `prerelease` — mark the GitHub release as a prerelease.

What it does:

1. **prepare** — checks out `main`, runs `./gradlew test`, reads `appVersion` from
   `gradle.properties`, increments it, commits the bump, creates the `v<version>` tag, and pushes
   both the commit and the tag.
2. **linux** — checks out the new tag, installs `rpm`, and builds `packageDeb` + `packageRpm`
   with `RIMALUX_PACKAGING_JDK=$JAVA_HOME`.
3. **windows** — checks out the new tag, installs WiX (best-effort), and builds `packageExe`
   and `packageMsi`.
4. **publish** — downloads both artifact sets and publishes the GitHub release at the new tag
   (marked latest).

Release asset names use:

```text
rimalux_desktop_<version>_<YYYYMMDD>.<ext>
```

for example `rimalux_desktop_0.1.1_20261109.rpm`, `.deb`, `.exe`, `.msi`. The installed
application/executable keeps the stable name `Rimalux`.

Version source of truth:

- `gradle.properties` → `appVersion=…` (bumped by the workflow).
- `desktopApp/build.gradle.kts` → `packageVersion = appVersion`.
- The build generates `app-version.txt` into the app resources, and `BuildInfo.VERSION` reads it,
  so the About card, diagnostics, update check, and `--version` all match the tag.

The workflow needs `contents: write` and a `main` branch that allows the bot push. On protected
branches, allow `github-actions[bot]` to push or run the workflow from an unprotected branch.

Windows MSI packaging is marked best-effort in CI because the WiX toolchain and interactive
desktop availability vary between runners. Treat a missing MSI as a CI warning, not a code
failure, until a Windows runner is verified.

## Diagnostics and support

```bash
# Print backend/monitor report without changing the wallpaper
rimalux --test-backend

# Write a support ZIP
rimalux --diagnostics-bundle /tmp/rimalux-diagnostics.zip
```

The bundle contains system info, redacted settings, monitor/backend state, and recent logs.
Custom wallpaper commands are redacted.

## Signing

- Windows: optional `signtool` with a code-signing certificate. Unsigned MSI/EXE trigger
  SmartScreen warnings.
- Linux: optional GPG signing for repository packages.
- Document the signing process; do not commit private keys.
- CI signing is not enabled yet; add repository secrets and a signing step before a stable
  public release.

## Artifacts

- `rimalux-desktop_<version>_amd64.deb`
- `rimalux-desktop-<version>.x86_64.rpm`
- `Rimalux-<version>.msi`
- `Rimalux-<version>.exe`
- Portable Linux `.tar.gz` and Windows `.zip` from `createDistributable`
- `SHA256SUMS.txt`

## Release checklist

1. `./gradlew test` passes.
2. `./gradlew :desktopApp:run` launches on Linux KDE.
3. Apply wallpaper, save PNG, restart, verify settings.
4. `./gradlew :desktopApp:packageDeb` and `packageRpm` succeed.
5. CI Windows MSI/EXE build succeeds.
6. Windows `--test-backend` report reviewed.
7. Human Windows checklist completed or explicitly deferred.
8. README/docs updated.
9. Tag `v<version>` and publish release notes.
