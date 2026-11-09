package com.cptodix.rimalux.desktop

object BuildInfo {
    /** Bumped by the release workflow via the `appVersion` Gradle property. */
    val VERSION: String by lazy {
        runCatching {
            BuildInfo::class.java
                .getResourceAsStream("/app-version.txt")
                ?.bufferedReader()
                ?.use { it.readText().trim() }
                ?.takeIf { it.isNotBlank() }
        }.getOrNull() ?: "0.1.0"
    }

    val engineCommit: String by lazy {
        runCatching {
            BuildInfo::class.java
                .getResourceAsStream("/engine-commit.txt")
                ?.bufferedReader()
                ?.use { it.readText().trim() }
                ?.takeIf { it.isNotBlank() }
        }.getOrNull() ?: "unknown"
    }
}
