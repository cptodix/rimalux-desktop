import java.io.File

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose) apply false
}

// ---------------------------------------------------------------------------
// Android asset sync
//
// The Android project remains the source of truth for the engine, strings,
// pattern previews, fonts, and launcher icon. These tasks copy those assets
// into the desktop repo. Do not edit the synced copies by hand.
// ---------------------------------------------------------------------------

val androidProjectPath = providers.gradleProperty("androidProjectPath")
    .orElse(providers.gradleProperty("androidProjectDir"))
    .orElse("../rimalux")

val androidRoot: Provider<File> = androidProjectPath.map { project.file(it) }

fun androidFile(relativePath: String): Provider<File> =
    androidRoot.map { File(it, relativePath) }

tasks.register<Sync>("syncEngine") {
    group = "rimalux"
    description = "Copy the pure Kotlin wallpaper engine from the Android project."
    from(androidFile("app/src/main/java/com/cptodix/rimalux/oled/wallpaper/engine")) {
        include("*.kt")
    }
    into(layout.projectDirectory.dir("core/src/main/kotlin/com/cptodix/rimalux/oled/wallpaper/engine"))
}

tasks.register<Copy>("syncStringsEn") {
    group = "rimalux"
    description = "Copy English Android strings."
    from(androidFile("app/src/main/res/values/strings.xml"))
    into(layout.projectDirectory.dir("desktopApp/src/main/composeResources/values"))
}

tasks.register<Copy>("syncStringsId") {
    group = "rimalux"
    description = "Copy Indonesian Android strings (values-in -> values-id)."
    from(androidFile("app/src/main/res/values-in/strings.xml"))
    into(layout.projectDirectory.dir("desktopApp/src/main/composeResources/values-id"))
}

tasks.register("syncStrings") {
    group = "rimalux"
    description = "Copy both Android string catalogs."
    dependsOn("syncStringsEn", "syncStringsId")
}

tasks.register<Copy>("syncPreviews") {
    group = "rimalux"
    description = "Copy the 80 static pattern preview images."
    from(androidFile("app/src/main/res/drawable-nodpi")) {
        include("pattern_preview_*.webp")
    }
    into(layout.projectDirectory.dir("desktopApp/src/main/composeResources/drawable"))
}

tasks.register<Copy>("syncFonts") {
    group = "rimalux"
    description = "Copy the bundled UI fonts."
    from(androidFile("app/src/main/res/font")) {
        include("plus_jakarta_sans.ttf", "jetbrains_mono.ttf")
    }
    into(layout.projectDirectory.dir("desktopApp/src/main/composeResources/font"))
}

tasks.register<Copy>("syncBrandIcon") {
    group = "rimalux"
    description = "Copy the launcher icon for the window and tray."
    from(androidFile("app/src/main/res/mipmap-xxxhdpi/ic_launcher.png")) {
        rename { "rimalux_icon.png" }
    }
    into(layout.projectDirectory.dir("desktopApp/src/main/composeResources/drawable"))
}

tasks.register<Copy>("syncBrandPackage") {
    group = "rimalux"
    description = "Copy the Linux package icon."
    from(androidFile("app/src/main/res/mipmap-xxxhdpi/ic_launcher.png")) {
        rename { "rimalux.png" }
    }
    into(layout.projectDirectory.dir("desktopApp/packaging"))
}

tasks.register("syncBrand") {
    group = "rimalux"
    description = "Copy brand icons for the app and package."
    dependsOn("syncBrandIcon", "syncBrandPackage")
}

tasks.register("writeEngineCommit") {
    group = "rimalux"
    description = "Record the Android commit used for the current engine sync."
    doLast {
        val root = androidRoot.get()
        if (!root.isDirectory) {
            logger.warn("Android project not found at ${root.absolutePath}; skipping engine commit.")
            return@doLast
        }
        val hash = runCatching {
            val process = ProcessBuilder("git", "-C", root.absolutePath, "rev-parse", "HEAD")
                .redirectErrorStream(true)
                .start()
            val text = process.inputStream.bufferedReader().use { it.readText() }.trim()
            process.waitFor()
            text.takeIf { process.exitValue() == 0 && it.length >= 7 }
        }.getOrNull()
        if (hash == null) {
            logger.warn("Could not read Android git commit; skipping engine commit.")
            return@doLast
        }
        val target = layout.projectDirectory.file("core/ENGINE_COMMIT").asFile
        target.parentFile.mkdirs()
        target.writeText(hash + "\n")
        logger.lifecycle("Engine commit: $hash")
    }
}

tasks.register("syncAll") {
    group = "rimalux"
    description = "Sync engine, strings, previews, fonts, brand icon, and engine commit."
    dependsOn(
        "syncEngine",
        "syncStrings",
        "syncPreviews",
        "syncFonts",
        "syncBrand",
        "writeEngineCommit"
    )
}
