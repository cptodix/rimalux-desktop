# Third-Party Notices

Rimalux Desktop is licensed under the PolyForm Perimeter License 1.0.1 (see `LICENSE`).
That license covers the Rimalux source code only. The application bundles third-party
components under their own licenses, listed below.

This file is informational. The authoritative license text for each component ships with the
component or is available from its upstream project.

## Runtime components

| Component | License |
| --- | --- |
| Kotlin standard library (`org.jetbrains.kotlin`) | Apache License 2.0 |
| Kotlinx Coroutines (`org.jetbrains.kotlinx:kotlinx-coroutines-core`, `kotlinx-coroutines-swing`) | Apache License 2.0 |
| Kotlinx Serialization (`org.jetbrains.kotlinx:kotlinx-serialization-json`) | Apache License 2.0 |
| Jetpack Compose Multiplatform / Compose Desktop (`org.jetbrains.compose`) | Apache License 2.0 |
| Compose Material 3 and Material Icons Extended | Apache License 2.0 |
| Skiko (`org.jetbrains.skiko`) | Apache License 2.0 |
| Skia (bundled by Skiko) | BSD 3-Clause |
| JNA / JNA Platform (`net.java.dev.jna`) | LGPL 2.1 or later, or Apache License 2.0 (dual) |
| dbus-java (`com.github.hypfvieh:dbus-java-core`, `dbus-java-transport-native-unixsocket`) | MIT |
| SLF4J API (`org.slf4j`) | MIT |

## Build and test components

| Component | License |
| --- | --- |
| JUnit 4 (`junit:junit`) | Eclipse Public License 1.0 |
| Kotlinx Coroutines Test (`kotlinx-coroutines-test`) | Apache License 2.0 |

## Notes

- The bundled Java runtime created by `jlink`/`jpackage` is derived from the OpenJDK build
  used at packaging time and carries that build's license (GPL 2.0 with the Classpath
  Exception).
- Native libraries used by AWT/Swing and Skia on Linux and Windows keep their upstream
  licenses.
- If you redistribute Rimalux, keep this file together with the application, alongside
  `LICENSE`.
