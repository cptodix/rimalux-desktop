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

Releases ship exactly two assets:

```text
rimalux_desktop_<version>_<YYYYMMDD>.AppImage   Linux (x86_64)
rimalux_desktop_<version>_<YYYYMMDD>.exe        Windows installer
```

### Linux (AppImage)

CI builds the jpackage app-image and wraps it with `appimagetool`:

```bash
export RIMALUX_PACKAGING_JDK=/path/to/jdk-21
./gradlew :desktopApp:createDistributable

# AppDir/usr <- desktopApp/build/compose/binaries/main/app/Rimalux/
# AppDir/rimalux.desktop, AppDir/rimalux.png, AppDir/AppRun
ARCH=x86_64 appimagetool --appimage-extract-and-run --no-appstream AppDir rimalux.AppImage
```

The AppImage bundles its own runtime, so it needs no system Java. DEB/RPM packaging still works
locally (`packageDeb`, `packageRpm`) but is not published.

### Windows (EXE installer)

```powershell
.\gradlew.bat :desktopApp:packageExe
```

Output: `desktopApp\build\compose\binaries\main\exe\*.exe`. It is a per-user installer
(`perUserInstall = true`) and upgrades in place via the stable `upgradeUuid`. MSI packaging still
works locally but is not published. Cross-compilation is not supported by jpackage.

### Install scripts

- `scripts/install.sh` — resolves the latest release (or `RIMALUX_VERSION`), downloads the
  AppImage to `~/.local/share/rimalux/rimalux.AppImage`, symlinks `~/.local/bin/rimalux`, writes
  `~/.local/share/applications/rimalux.desktop`, and installs the icon. `-uninstall` removes the
  app, launcher, menu entry, and icon, keeping settings and cached wallpapers.
- `scripts/install.ps1` — resolves the latest release (or `RIMALUX_VERSION`), downloads the EXE
  and runs it (`/quiet`, falling back to the interactive wizard). `-Uninstall` runs the registered
  uninstaller quietly.

### License and notices

Rimalux Desktop is licensed under the PolyForm Perimeter License 1.0.1 (`LICENSE`). The AppImage
bundles it at `usr/share/licenses/rimalux/LICENSE` together with
`usr/share/doc/rimalux/THIRD_PARTY_NOTICES.md`. The About card shows the license name and URL.

PolyForm Perimeter has no SPDX identifier, so packaging metadata uses the custom reference
`LicenseRef-PolyForm-Perimeter-1.0.1`.

### In-app updates

**Advanced Settings → Support → Check for Updates** reads the GitHub releases API and compares
the tag with `BuildInfo.VERSION`. When a newer release exists, **Install Update**:

- Linux AppImage: downloads the new AppImage next to the running one, replaces it atomically,
  relaunches it, and exits. Requires `APPIMAGE` (set by the AppImage runtime).
- Windows: downloads the EXE, starts a helper that waits for the app to exit, then runs the
  installer with `/quiet`.
- Dev runs and distro packages: shows the release URL instead of self-updating.

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
2. **linux** — checks out the new tag, builds the jpackage app-image, wraps it with
   `appimagetool`, and names it `rimalux_desktop_<version>_<YYYYMMDD>.AppImage`.
3. **windows** — checks out the new tag, installs WiX (best-effort), and builds the EXE
   installer as `rimalux_desktop_<version>_<YYYYMMDD>.exe`.
4. **publish** — downloads both artifacts and publishes the GitHub release at the new tag
   (marked latest).

Only those two assets are published. The installed application keeps the stable name `Rimalux`.

Version source of truth:

- `gradle.properties` → `appVersion=…` (bumped by the workflow).
- `desktopApp/build.gradle.kts` → `packageVersion = appVersion`.
- The build generates `app-version.txt` into the app resources, and `BuildInfo.VERSION` reads it,
  so the About card, diagnostics, update check, and `--version` all match the tag.

The workflow needs `contents: write` and a `main` branch that allows the bot push. On protected
branches, allow `github-actions[bot]` to push or run the workflow from an unprotected branch.

The Windows job needs WiX; the install step is best-effort, but the EXE build itself must
succeed for the release to publish.

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
