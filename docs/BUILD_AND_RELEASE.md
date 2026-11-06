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

```yaml
Linux:   ubuntu-latest  -> packageDeb, packageRpm
Windows: windows-latest -> packageMsi, packageExe
```

Steps:

1. Checkout.
2. Set up JDK 21 (Temurin).
3. Set up Gradle.
4. Install Linux packaging tools / WiX.
5. `./gradlew test`.
6. `./gradlew :desktopApp:packageDistributionForCurrentOS -PappVersion=$VERSION`.
7. Run Windows `--test-backend` (best-effort; skip if no interactive desktop).
8. Upload artifacts and checksums.
9. On tag, attach artifacts to the GitHub release.

## Artifacts

- `rimalux-desktop_<version>_amd64.deb`
- `rimalux-desktop-<version>.x86_64.rpm`
- `Rimalux-<version>.msi`
- `Rimalux-<version>.exe`
- Portable Linux `.tar.gz` and Windows `.zip` from `createDistributable`
- `SHA256SUMS.txt`

## Signing

- Windows: optional `signtool` with a code-signing certificate. Unsigned MSI/EXE trigger
  SmartScreen warnings.
- Linux: optional GPG signing for repository packages.
- Document the signing process; do not commit private keys.

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
