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

## CI matrix

The repository ships two workflows:

- `.github/workflows/build.yml` — manual dispatch only. Tests on Ubuntu, packages DEB/RPM on
  Linux, packages EXE/MSI on Windows, uploads artifacts.
- `.github/workflows/release.yml` — manual dispatch only (requires a release tag input). Builds
  Linux and Windows installers and attaches them to the GitHub release.

Neither workflow runs on push or pull request; start them from the Actions tab with
**Run workflow**.

Steps in the build workflow:

1. Checkout.
2. Set up JDK 21 (Temurin).
3. Set up Gradle.
4. `./gradlew test`.
5. Linux: install `rpm`, build `packageDeb` and `packageRpm` with
   `RIMALUX_PACKAGING_JDK=$JAVA_HOME`.
6. Windows: install WiX (best-effort), build `packageExe` and `packageMsi`.
7. Upload artifacts.

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
