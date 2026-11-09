import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":core"))

    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation(compose.components.resources)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.swing)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.jna.platform)
    implementation(libs.dbus.java.core)
    implementation(libs.dbus.java.native.unixsocket)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}

compose.resources {
    packageOfResClass = "com.cptodix.rimalux.desktop.resources"
}

tasks.processResources {
    from(rootProject.layout.projectDirectory.file("core/ENGINE_COMMIT")) {
        rename { "engine-commit.txt" }
    }
}

tasks.test {
    useJUnit()
    systemProperty("java.awt.headless", "true")
}

val packagingJdk = providers.environmentVariable("RIMALUX_PACKAGING_JDK").orNull
val appVersion = providers.gradleProperty("appVersion").orElse("0.1.0")

val generateAppVersion by tasks.registering {
    val version = appVersion
    val outputDir = layout.buildDirectory.dir("generated/appVersion")
    inputs.property("appVersion", version)
    outputs.dir(outputDir)
    doLast {
        val file = outputDir.get().file("app-version.txt").asFile
        file.parentFile.mkdirs()
        file.writeText(version.get())
    }
}

sourceSets["main"].resources.srcDir(generateAppVersion)

compose.desktop {
    application {
        mainClass = "com.cptodix.rimalux.desktop.MainKt"

        if (!packagingJdk.isNullOrBlank()) {
            javaHome = packagingJdk
        }

        jvmArgs += listOf("-Dfile.encoding=UTF-8")

        nativeDistributions {
            targetFormats(TargetFormat.Deb, TargetFormat.Rpm, TargetFormat.Msi, TargetFormat.Exe)
            packageName = "Rimalux"
            packageVersion = appVersion.get()
            description = "Procedural OLED wallpaper studio"
            vendor = "Cptodix Studio"
            copyright = "© 2026 Cptodix Studio"
            modules(
                "java.desktop",
                "java.logging",
                "java.naming",
                "java.net.http",
                "java.prefs",
                "java.xml",
                "jdk.crypto.ec",
                "jdk.net",
                "jdk.security.auth",
                "jdk.unsupported"
            )

            linux {
                packageName = "rimalux"
                debMaintainer = "cptodix@users.noreply.github.com"
                appCategory = "Graphics"
                menuGroup = "Graphics"
                shortcut = true
                iconFile.set(project.file("packaging/rimalux.png"))
            }

            windows {
                menuGroup = "Rimalux"
                menu = true
                shortcut = true
                perUserInstall = true
                dirChooser = true
                upgradeUuid = "7E4C2B6A-6D2F-4B3D-9F8E-2A1C5D7E9B10"
            }
        }
    }
}
