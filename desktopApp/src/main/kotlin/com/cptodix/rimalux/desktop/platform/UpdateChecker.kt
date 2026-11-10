package com.cptodix.rimalux.desktop.platform

import com.cptodix.rimalux.desktop.BuildInfo
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

data class UpdateAsset(
    val name: String,
    val downloadUrl: String
)

data class UpdateInfo(
    val currentVersion: String,
    val latestVersion: String,
    val releaseUrl: String,
    val updateAvailable: Boolean,
    val assets: List<UpdateAsset> = emptyList()
)

/**
 * Checks the GitHub releases API for a newer desktop release. Network failures are returned as a
 * failed [Result] so the UI can stay offline-first.
 */
class UpdateChecker(
    private val http: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build(),
    private val json: Json = Json { ignoreUnknownKeys = true; isLenient = true }
) {
    suspend fun check(
        currentVersion: String = BuildInfo.VERSION,
        repository: String = DEFAULT_REPOSITORY
    ): Result<UpdateInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val request = HttpRequest.newBuilder(
                URI.create("https://api.github.com/repos/$repository/releases/latest")
            )
                .timeout(Duration.ofSeconds(20))
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "Rimalux-Desktop/$currentVersion")
                .GET()
                .build()
            val response = http.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() == 404) {
                return@runCatching UpdateInfo(
                    currentVersion = currentVersion,
                    latestVersion = currentVersion,
                    releaseUrl = "https://github.com/$repository/releases",
                    updateAvailable = false
                )
            }
            if (response.statusCode() !in 200..299) {
                error("Update service returned HTTP ${response.statusCode()}")
            }
            val release = json.decodeFromString(GitHubRelease.serializer(), response.body())
            val latest = release.tagName.trim().removePrefix("v").removePrefix("V")
            if (latest.isBlank()) error("Update service returned no release tag")
            UpdateInfo(
                currentVersion = currentVersion,
                latestVersion = latest,
                releaseUrl = release.htmlUrl,
                updateAvailable = isNewer(latest, currentVersion),
                assets = release.assets.mapNotNull { asset ->
                    val url = asset.downloadUrl.takeIf { it.isNotBlank() }
                    val name = asset.name.takeIf { it.isNotBlank() }
                    if (url != null && name != null) UpdateAsset(name, url) else null
                }
            )
        }
    }

    companion object {
        const val DEFAULT_REPOSITORY = "cptodix/rimalux-desktop"

        /** Compares dotted numeric versions; pre-release suffixes are ignored. */
        fun isNewer(candidate: String, current: String): Boolean {
            val left = parts(candidate)
            val right = parts(current)
            val size = maxOf(left.size, right.size)
            for (index in 0 until size) {
                val a = left.getOrElse(index) { 0 }
                val b = right.getOrElse(index) { 0 }
                if (a != b) return a > b
            }
            return false
        }

        private fun parts(version: String): List<Int> =
            version.trim()
                .removePrefix("v")
                .removePrefix("V")
                .substringBefore('-')
                .substringBefore('+')
                .split('.')
                .map { it.toIntOrNull() ?: 0 }
    }
}

@Serializable
private data class GitHubRelease(
    @SerialName("tag_name") val tagName: String = "",
    @SerialName("html_url") val htmlUrl: String = "",
    val name: String = "",
    val prerelease: Boolean = false,
    val draft: Boolean = false,
    val assets: List<GitHubAsset> = emptyList()
)

@Serializable
private data class GitHubAsset(
    val name: String = "",
    @SerialName("browser_download_url") val downloadUrl: String = ""
)
