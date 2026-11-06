package com.cptodix.rimalux.desktop

object BuildInfo {
    const val VERSION: String = "0.1.0"

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
