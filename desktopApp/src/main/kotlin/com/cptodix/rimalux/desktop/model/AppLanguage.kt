package com.cptodix.rimalux.desktop.model

import java.util.Locale

/**
 * Single-source registry of supported application languages.
 *
 * The Android project uses `values-in` for Indonesian; Compose Resources uses `values-id`.
 */
enum class AppLanguage(
    val languageTag: String,
    val nativeName: String
) {
    EN_US(
        languageTag = "en-US",
        nativeName = "English (US)"
    ),
    ID_ID(
        languageTag = "id-ID",
        nativeName = "Bahasa Indonesia"
    );

    fun toLocale(): Locale = Locale.forLanguageTag(languageTag)

    companion object {
        val DEFAULT: AppLanguage = EN_US

        fun fromTagOrNull(tag: String?): AppLanguage? {
            if (tag.isNullOrBlank()) return null
            val normalizedInput = normalizeCanonicalTag(Locale.forLanguageTag(tag.replace('_', '-')))
            return entries.firstOrNull { entry ->
                entry.languageTag.equals(tag, ignoreCase = true) ||
                    normalizeCanonicalTag(entry.toLocale()) == normalizedInput
            }
        }

        fun fromTagOrDefault(tag: String?): AppLanguage = fromTagOrNull(tag) ?: DEFAULT

        fun resolveInitialLanguage(
            systemLocales: List<Locale>,
            supported: List<AppLanguage> = entries
        ): AppLanguage {
            if (supported.isEmpty()) return DEFAULT
            val fallback = supported.firstOrNull { it == DEFAULT } ?: supported.first()
            val matchedTag = resolveBestSupportedTag(
                systemLocales = systemLocales,
                supportedTags = supported.map { it.languageTag },
                fallbackTag = fallback.languageTag
            )
            return supported.firstOrNull { it.languageTag.equals(matchedTag, ignoreCase = true) } ?: fallback
        }

        internal fun resolveBestSupportedTag(
            systemLocales: List<Locale>,
            supportedTags: List<String>,
            fallbackTag: String = DEFAULT.languageTag
        ): String {
            if (supportedTags.isEmpty()) return fallbackTag
            val parsedSupported = supportedTags.map { rawTag ->
                rawTag to parseLocaleComponents(Locale.forLanguageTag(rawTag.replace('_', '-')))
            }

            for (sysLocale in systemLocales) {
                val sys = parseLocaleComponents(sysLocale)
                if (sys.language.isEmpty()) continue

                parsedSupported.firstOrNull { (_, cand) ->
                    cand.language == sys.language &&
                        cand.script == sys.script &&
                        cand.region.isNotEmpty() &&
                        cand.region == sys.region
                }?.let { return it.first }

                if (sys.script.isNotEmpty()) {
                    parsedSupported.firstOrNull { (_, cand) ->
                        cand.language == sys.language && cand.script == sys.script
                    }?.let { return it.first }
                }

                parsedSupported.firstOrNull { (_, cand) ->
                    cand.language == sys.language &&
                        (cand.script.isEmpty() || sys.script.isEmpty() || cand.script == sys.script)
                }?.let { return it.first }
            }

            return fallbackTag
        }

        private data class LocaleComponents(
            val language: String,
            val script: String,
            val region: String
        )

        private fun normalizeCanonicalTag(locale: Locale): String {
            val c = parseLocaleComponents(locale)
            return buildString {
                append(c.language)
                if (c.script.isNotEmpty()) append('-').append(c.script)
                if (c.region.isNotEmpty()) append('-').append(c.region)
            }
        }

        private fun parseLocaleComponents(locale: Locale): LocaleComponents {
            val rawLang = locale.language.lowercase(Locale.ROOT)
            val normalizedLang = when (rawLang) {
                "in" -> "id"
                "iw" -> "he"
                "ji" -> "yi"
                else -> rawLang
            }
            val region = locale.country.uppercase(Locale.ROOT)
            val explicitScript = locale.script.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
            }
            val inferredScript = when {
                explicitScript.isNotEmpty() -> explicitScript
                normalizedLang == "zh" && (region == "TW" || region == "HK" || region == "MO") -> "Hant"
                normalizedLang == "zh" && (region == "CN" || region == "SG") -> "Hans"
                else -> ""
            }
            return LocaleComponents(
                language = normalizedLang,
                script = inferredScript,
                region = region
            )
        }
    }
}
