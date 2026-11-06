package com.cptodix.rimalux.desktop.data

import com.cptodix.rimalux.desktop.model.AppLanguage
import com.cptodix.rimalux.desktop.model.DesktopSettings
import com.cptodix.rimalux.desktop.model.UiSettings
import com.cptodix.rimalux.desktop.platform.AppLog
import com.cptodix.rimalux.desktop.platform.PlatformPaths
import java.util.Locale
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * JSON-backed settings store with atomic writes and corrupt-file recovery.
 */
class SettingsStore(
    private val paths: PlatformPaths,
    private val json: Json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
        isLenient = true
    }
) {
    private val mutex = Mutex()
    private val _state: MutableStateFlow<DesktopSettings> = MutableStateFlow(load())
    val state: StateFlow<DesktopSettings> = _state.asStateFlow()

    val settingsFile: Path get() = paths.configFile

    suspend fun update(transform: (DesktopSettings) -> DesktopSettings) {
        mutex.withLock {
            val next = normalize(transform(_state.value))
            _state.value = next
            withContext(Dispatchers.IO) {
                write(next)
            }
        }
    }

    suspend fun replace(settings: DesktopSettings) {
        update { settings }
    }

    fun snapshot(): DesktopSettings = _state.value

    private fun load(): DesktopSettings {
        paths.ensureDirectories()
        val file = paths.configFile
        if (!Files.exists(file)) {
            AppLog.info("No settings file at $file; using defaults.")
            val initialLanguage = AppLanguage.resolveInitialLanguage(listOf(Locale.getDefault()))
            return DesktopSettings(ui = UiSettings(languageTag = initialLanguage.languageTag))
        }
        return runCatching {
            val text = Files.readString(file)
            val dto = json.decodeFromString(SettingsFileDto.serializer(), text)
            normalize(SettingsMapper.toDomain(dto))
        }.getOrElse { error ->
            AppLog.error("Could not read settings file $file; backing it up and using defaults.", error)
            runCatching {
                val backup = file.resolveSibling(
                    "settings.json.bak.${Instant.now().epochSecond}"
                )
                Files.move(file, backup, StandardCopyOption.REPLACE_EXISTING)
            }
            DesktopSettings()
        }
    }

    private fun write(settings: DesktopSettings) {
        paths.ensureDirectories()
        val file = paths.configFile
        val temp = file.resolveSibling("settings.json.tmp")
        runCatching {
            val dto = SettingsMapper.toDto(settings)
            val text = json.encodeToString(SettingsFileDto.serializer(), dto)
            Files.writeString(temp, text)
            try {
                Files.move(
                    temp,
                    file,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
                )
            } catch (_: Exception) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING)
            }
        }.onFailure { error ->
            AppLog.error("Could not write settings file $file", error)
        }
    }

    private fun normalize(settings: DesktopSettings): DesktopSettings {
        val generation = settings.generation.copy(
            pitchBlackRatio = settings.generation.pitchBlackRatio.coerceIn(0f, 0.95f),
            pitchBlackRangeMin = minOf(
                settings.generation.pitchBlackRangeMin,
                settings.generation.pitchBlackRangeMax
            ).coerceIn(0f, 0.95f),
            pitchBlackRangeMax = maxOf(
                settings.generation.pitchBlackRangeMin,
                settings.generation.pitchBlackRangeMax
            ).coerceIn(0f, 0.95f)
        )
        val palettes = settings.resolvedPalettes()
        val paletteIds = palettes.map { it.id }.toSet()
        val selectedPaletteIds = settings.generation.selectedPaletteIds
            .filter { it in paletteIds }
            .toSet()
            .ifEmpty { setOf(palettes.first().id) }

        val current = settings.currentSet?.let { set ->
            val items = set.items
                .distinctBy { it.monitorId }
                .sortedBy { it.screenIndex }
            if (items.isEmpty()) null else set.copy(items = items)
        }
        val history = settings.history
            .filter { it.items.isNotEmpty() }
            .distinctBy { it.id }
            .take(24)

        return settings.copy(
            generation = generation.copy(selectedPaletteIds = selectedPaletteIds),
            display = settings.display.copy(
                customWidth = settings.display.customWidth.coerceIn(320, 8192),
                customHeight = settings.display.customHeight.coerceIn(240, 8192)
            ),
            autoChange = settings.autoChange.copy(
                intervalMinutes = settings.autoChange.intervalMinutes.coerceIn(1, 10080)
            ),
            currentSet = current,
            history = history,
            userPalettes = palettes
        )
    }
}
