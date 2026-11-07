# RULES.md — Mandatory Agent Testing & Visual Verification Rules

These rules apply to every AI agent and contributor working in `rimalux-desktop`.
Read this file together with `AGENTS.md` before running any test, build, or debug session.

---

## 1. Screenshots and image context

- **Do not take repeated screenshots for visual testing.** Image context is limited and must not
  be wasted on iterative visual checks.
- Do not capture the desktop, windows, or app UI unless the user explicitly asks for a screenshot
  or explicitly approves one.
- If a screenshot is approved, take **one** purposeful screenshot, inspect it, and stop. Do not
  repeat captures after every small change.
- Never use screenshots as the primary regression mechanism. Prefer automated assertions,
  headless renders written to files, logs, and state inspection.

## 2. Foreground app runs

- **Never run the GUI app in the foreground without the user present and approving it.**
- Do not launch `./gradlew :desktopApp:run` or the packaged app on the user's desktop as an
  unattended step.
- If a GUI run is needed, ask the user first and wait for their answer.

## 3. Background and non-disruptive testing

Prefer tests that do not touch the user's desktop:

- `./gradlew test`
- headless render/CLI commands such as
  `./gradlew :desktopApp:run --args="--render-test /tmp/opencode/rimalux-render-test"`
- `./gradlew :desktopApp:run --args="--list-monitors"` and `--diagnostics`
- inspecting settings JSON, logs, generated PNGs, and test reports
- writing outputs to temporary directories (`/tmp/opencode/...`)
- unit tests with fake clocks/backends (for scheduler work)
- packaging into temporary extraction directories

Do not:

- change the user's live wallpaper,
- open windows on their desktop,
- move or focus their windows,
- install system packages,
- run foreground GUI apps,
- or otherwise interfere with what they are doing.

If a test would change the desktop or user state, **stop and ask**.

## 4. UI testing workflow

If UI behavior must be verified:

1. **Ask the user first and wait for their answer.**
2. If approved, start the app in the agreed debug/background mode.
3. Tell the user exactly what to do (which screen, which control, what result to look for).
4. Let the user interact with the app themselves.
5. Then either:
   - inspect the debug log / settings JSON / generated files, or
   - wait for the user's feedback.
6. Do not automate clicks, window management, or screenshots unless the user explicitly asks for it.

## 5. State-changing tests

These need explicit permission before running:

- applying a wallpaper (`Apply` / `--rotate-now` later),
- changing language or settings in the user's real config,
- writing to `~/Pictures/Rimalux`,
- installing/removing packages,
- enabling autostart or system services,
- changing the user's live desktop wallpaper.

For automated coverage, use temporary paths and fakes instead of the real user environment.

## 6. Preferred evidence

Use these instead of screenshots:

- `./gradlew test` results and HTML reports
- `build/test-results/...`
- `--render-test` PNG outputs plus measured black percentages
- `--diagnostics` output
- `settings.json` and cache file inspection
- application logs
- compile/package task output

## 7. Safe command examples

```bash
# Automated tests (preferred)
./gradlew test

# Headless render proof to a temp directory
./gradlew :desktopApp:run --args="--render-test /tmp/opencode/rimalux-render-test"

# Diagnostic output only
./gradlew :desktopApp:run --args="--diagnostics"

# Package into build outputs; do not install
RIMALUX_PACKAGING_JDK=/path/to/full-jdk-21 ./gradlew :desktopApp:packageRpm
```

## 8. Enforcement checklist

Before running anything, ask:

1. Does this need a visible window? If yes, ask the user and wait.
2. Does this change the user's desktop, wallpaper, settings, or files? If yes, ask first.
3. Can this be covered by a unit test, headless command, temp directory, or log inspection instead?
4. Am I about to take more than one screenshot? If yes, stop.
5. Am I about to run a GUI app in the foreground without the user present? If yes, stop.
