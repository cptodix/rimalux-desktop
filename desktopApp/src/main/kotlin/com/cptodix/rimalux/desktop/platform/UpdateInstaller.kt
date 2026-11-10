package com.cptodix.rimalux.desktop.platform

import com.cptodix.rimalux.desktop.BuildInfo
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.time.Duration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface UpdateInstallResult {
    /** The app must exit now; the downloaded build takes over. */
    data object Restarting : UpdateInstallResult

    /** Automatic install is not possible; the release page has the download. */
    data class Manual(val releaseUrl: String) : UpdateInstallResult
}

enum class UpdatePlatform { WINDOWS, LINUX_APPIMAGE, MANUAL }

/**
 * Downloads the latest release and swaps it in.
 *
 * Linux builds run as an AppImage; the new file replaces the running one and is relaunched.
 * Windows builds use the jpackage EXE installer, started after this process exits so it can
 * replace the installed files. Anything else (dev runs, distro packages) falls back to the
 * release page.
 */
class UpdateInstaller(
    private val http: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(15))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build(),
    private val osName: String = System.getProperty("os.name").orEmpty().lowercase(),
    private val appImagePath: String? = System.getenv("APPIMAGE")
) {
    suspend fun install(info: UpdateInfo): Result<UpdateInstallResult> = withContext(Dispatchers.IO) {
        runCatching {
            when (platform()) {
                UpdatePlatform.WINDOWS -> installWindows(info)
                UpdatePlatform.LINUX_APPIMAGE -> installAppImage(info)
                UpdatePlatform.MANUAL -> UpdateInstallResult.Manual(info.releaseUrl)
            }
        }
    }

    internal fun platform(): UpdatePlatform = when {
        osName.contains("win") -> UpdatePlatform.WINDOWS
        !appImagePath.isNullOrBlank() -> UpdatePlatform.LINUX_APPIMAGE
        else -> UpdatePlatform.MANUAL
    }

    private fun installAppImage(info: UpdateInfo): UpdateInstallResult {
        val asset = selectAsset(info.assets, UpdatePlatform.LINUX_APPIMAGE)
            ?: return UpdateInstallResult.Manual(info.releaseUrl)
        val target = Path.of(requireNotNull(appImagePath)).toAbsolutePath()
        val directory = target.parent ?: return UpdateInstallResult.Manual(info.releaseUrl)
        val temp = Files.createTempFile(directory, ".rimalux-update-", ".AppImage")
        try {
            download(asset.downloadUrl, temp)
            temp.toFile().setExecutable(true, false)
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING)
        } catch (error: Throwable) {
            runCatching { Files.deleteIfExists(temp) }
            throw error
        }
        ProcessBuilder(target.toString()).start()
        return UpdateInstallResult.Restarting
    }

    private fun installWindows(info: UpdateInfo): UpdateInstallResult {
        val asset = selectAsset(info.assets, UpdatePlatform.WINDOWS)
            ?: return UpdateInstallResult.Manual(info.releaseUrl)
        val installer = Files.createTempFile("rimalux-setup-", ".exe")
        try {
            download(asset.downloadUrl, installer)
        } catch (error: Throwable) {
            runCatching { Files.deleteIfExists(installer) }
            throw error
        }
        val quoted = installer.toAbsolutePath().toString().replace("'", "''")
        val script = "Wait-Process -Id ${ProcessHandle.current().pid()} -ErrorAction SilentlyContinue; " +
            "Start-Process -FilePath '$quoted' -ArgumentList '/quiet'"
        ProcessBuilder(
            "powershell",
            "-NoProfile",
            "-WindowStyle",
            "Hidden",
            "-Command",
            script
        ).start()
        return UpdateInstallResult.Restarting
    }

    private fun download(url: String, destination: Path) {
        val request = HttpRequest.newBuilder(URI.create(url))
            .timeout(Duration.ofMinutes(10))
            .header("User-Agent", "Rimalux-Desktop/${BuildInfo.VERSION}")
            .GET()
            .build()
        http.send(
            request,
            HttpResponse.BodyHandlers.ofFile(
                destination,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING
            )
        )
    }

    companion object {
        /** Picks the release asset that matches the running platform. */
        fun selectAsset(assets: List<UpdateAsset>, platform: UpdatePlatform): UpdateAsset? =
            when (platform) {
                UpdatePlatform.WINDOWS ->
                    assets.firstOrNull { it.name.endsWith(".exe", ignoreCase = true) }

                UpdatePlatform.LINUX_APPIMAGE ->
                    assets.firstOrNull { it.name.endsWith(".AppImage", ignoreCase = true) }

                UpdatePlatform.MANUAL -> null
            }
    }
}
