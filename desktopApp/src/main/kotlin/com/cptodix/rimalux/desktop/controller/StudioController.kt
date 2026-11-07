package com.cptodix.rimalux.desktop.controller

import androidx.compose.ui.graphics.toComposeImageBitmap
import com.cptodix.rimalux.desktop.data.SettingsStore
import com.cptodix.rimalux.desktop.model.AppLanguage
import com.cptodix.rimalux.desktop.model.ApplyTarget
import com.cptodix.rimalux.desktop.model.DesktopScreen
import com.cptodix.rimalux.desktop.model.DesktopSettings
import com.cptodix.rimalux.desktop.model.DisplaySettings
import com.cptodix.rimalux.desktop.model.FitMode
import com.cptodix.rimalux.desktop.model.GenerationConfig
import com.cptodix.rimalux.desktop.model.MonitorBounds
import com.cptodix.rimalux.desktop.model.MonitorInfo
import com.cptodix.rimalux.desktop.model.ResolutionMode
import com.cptodix.rimalux.desktop.model.UiSettings
import com.cptodix.rimalux.desktop.model.WallpaperItem
import com.cptodix.rimalux.desktop.model.WallpaperSet
import com.cptodix.rimalux.desktop.platform.AppLog
import com.cptodix.rimalux.desktop.platform.ApplyResult
import com.cptodix.rimalux.desktop.platform.AutostartManager
import com.cptodix.rimalux.desktop.platform.DefaultMonitorProvider
import com.cptodix.rimalux.desktop.platform.MonitorProvider
import com.cptodix.rimalux.desktop.platform.PlatformPaths
import com.cptodix.rimalux.desktop.platform.ProcessRunner
import com.cptodix.rimalux.desktop.platform.WallpaperBackend
import com.cptodix.rimalux.desktop.platform.WallpaperBackendFactory
import com.cptodix.rimalux.desktop.platform.WallpaperFileStore
import com.cptodix.rimalux.desktop.platform.openInFileManager
import com.cptodix.rimalux.desktop.render.SkiaWallpaperRenderer
import com.cptodix.rimalux.desktop.scheduler.AutoChangePolicy
import com.cptodix.rimalux.oled.wallpaper.engine.ArtPattern
import com.cptodix.rimalux.oled.wallpaper.engine.ColorFillMode
import com.cptodix.rimalux.oled.wallpaper.engine.ColorPalette
import com.cptodix.rimalux.oled.wallpaper.engine.CoreGenerationSpec
import com.cptodix.rimalux.oled.wallpaper.engine.DeepBlackRatioEngine
import com.cptodix.rimalux.oled.wallpaper.engine.MathPatternGenerator
import com.cptodix.rimalux.oled.wallpaper.engine.PaletteMode
import com.cptodix.rimalux.oled.wallpaper.engine.PatternWeightMode
import com.cptodix.rimalux.oled.wallpaper.engine.PitchBlackMode
import com.cptodix.rimalux.oled.wallpaper.engine.RandomPaletteGenerator
import com.cptodix.rimalux.oled.wallpaper.engine.SynthesizedWallpaper
import com.cptodix.rimalux.oled.wallpaper.engine.WallpaperGeneratorEngine
import java.util.Collections
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedDeque
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class StudioController(
    private val paths: PlatformPaths = PlatformPaths(),
    private val store: SettingsStore = SettingsStore(paths),
    private val runner: ProcessRunner = ProcessRunner(),
    private val monitorProvider: MonitorProvider = DefaultMonitorProvider(runner),
    private val backendFactory: WallpaperBackendFactory = WallpaperBackendFactory(runner, paths),
    private val fileStore: WallpaperFileStore = WallpaperFileStore(paths),
    private val autostartManager: AutostartManager = AutostartManager(paths),
    private val backendOverride: WallpaperBackend? = null,
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {
    private val _uiState = MutableStateFlow(StudioUiState())
    val uiState: StateFlow<StudioUiState> = _uiState.asStateFlow()

    private val generationMutex = Mutex()
    private val applyMutex = Mutex()
    private val autoRotationMutex = Mutex()
    private val past = ConcurrentLinkedDeque<WallpaperSet>()
    private val aheadQueue = ConcurrentLinkedDeque<RenderedSet>()
    private val pastRenderedCache = Collections.synchronizedMap(
        object : LinkedHashMap<String, RenderedSet>(8, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, RenderedSet>?): Boolean =
                size > 4
        }
    )
    private var synthesisJob: Job? = null
    private var prefetchJob: Job? = null
    private var autoChangeJob: Job? = null
    private var configPersistJob: Job? = null
    private var uiPersistJob: Job? = null
    private var deletedPaletteIds: Set<String> = emptySet()
    private var backend: WallpaperBackend = backendFactory.detect(store.snapshot())

    private companion object {
        const val PREFETCH_TARGET = 3
    }

    init {
        scope.launch { initialize() }
        startAutoChangeLoop()
    }

    private suspend fun initialize() {
        val snapshot = store.snapshot()
        val detected = runCatching { monitorProvider.listMonitors() }
            .getOrElse { error ->
                AppLog.error("Monitor detection failed", error)
                emptyList()
            }
        val monitors = detected.ifEmpty { listOf(fallbackMonitor()) }
        val selected = snapshot.display.selected(monitors)?.id ?: monitors.first().id
        past.clear()
        past.addAll(snapshot.history)
        deletedPaletteIds = snapshot.deletedPaletteIds
        backend = backendOverride ?: backendFactory.detect(snapshot)
        val language = AppLanguage.fromTagOrDefault(snapshot.ui.languageTag)
        Locale.setDefault(language.toLocale())

        _uiState.value = StudioUiState(
            config = snapshot.generation,
            display = snapshot.display.copy(selectedMonitorId = selected),
            ui = snapshot.ui.copy(languageTag = language.languageTag),
            autoChange = snapshot.autoChange,
            monitors = monitors,
            selectedMonitorId = selected,
            history = past.toList(),
            userPalettes = snapshot.userPalettes,
            screen = snapshot.ui.lastScreen,
            isGenerating = true,
            backendInfo = backend.info,
            languageTag = language.languageTag,
            canGoBack = past.isNotEmpty(),
            canGoForward = false
        )

        val current = snapshot.currentSet
        if (current != null && current.items.isNotEmpty()) {
            restoreSet(current, persist = false)
        } else {
            generateNewSet(pushHistory = false, persist = true)
        }

        if (snapshot.autoChange.runAtLogin) {
            runCatching { autostartManager.ensureEnabled() }
                .onFailure { AppLog.warning("Could not restore Start at Login entry", it) }
        }
        ensureAutoSchedule()
    }

    // ---------------------------------------------------------------------
    // Generation
    // ---------------------------------------------------------------------

    fun randomize() {
        generateNewSet(pushHistory = true, persist = true)
    }

    fun previous() {
        val previous = past.pollFirst() ?: return
        val cached = pastRenderedCache[previous.id]
        if (cached != null) {
            scope.launch {
                commitRenderedSet(cached, pushHistory = false, persist = true)
            }
        } else {
            restoreSet(previous, persist = true)
        }
    }

    fun next() {
        val buffered = aheadQueue.pollFirst()
        if (buffered != null) {
            _uiState.update { it.copy(aheadReadyCount = aheadQueue.size) }
            scope.launch {
                commitRenderedSet(buffered, pushHistory = true, persist = true)
            }
        } else {
            generateNewSet(pushHistory = true, persist = true)
        }
    }

    fun previewPattern(pattern: ArtPattern) {
        val current = _uiState.value.current ?: return
        val primary = current.set.items.firstOrNull() ?: return
        launchSynthesis {
            val state = _uiState.value
            val generated = ArrayList<GeneratedItem>(state.monitors.size)
            for ((index, monitor) in state.monitors.withIndex()) {
                val existing = current.set.items.firstOrNull { it.monitorId == monitor.id } ?: primary
                val (width, height) = resolveSize(monitor, state.display)
                val synth = WallpaperGeneratorEngine.synthesizeFromParameters(
                    seed = existing.seed,
                    pattern = pattern,
                    paletteColors = existing.paletteColors,
                    colorFillMode = state.config.colorFillMode,
                    pitchBlackRatio = state.config.resolvePitchBlackRatio(Random(existing.seed)),
                    intricacy = existing.intricacy,
                    aspectRatio = height.toFloat() / width.toFloat(),
                    patternWeightMode = state.config.patternWeightMode
                )
                generated += GeneratedItem(
                    item = buildItem(monitor, index, synth, width, height, state.display),
                    synthesized = synth
                )
            }
            val timestamp = System.currentTimeMillis()
            commit(
                generated,
                "gen_${timestamp}_${primary.seed.toString(16)}",
                timestamp,
                pushHistory = false,
                persist = true
            )
        }
    }

    fun setPatternsRandomized(enabled: Boolean) {
        updateConfig(RegenerationMode.RESOLVE) { it.copy(patternsRandomized = enabled) }
    }

    fun togglePattern(pattern: ArtPattern) {
        updateConfig(RegenerationMode.RESOLVE) { config ->
            if (config.patternsRandomized) {
                config.copy(
                    patternsRandomized = false,
                    selectedPatterns = setOf(pattern)
                )
            } else {
                val existing = config.selectedPatterns
                val next = if (pattern in existing && existing.size > 1) {
                    existing - pattern
                } else {
                    existing + pattern
                }
                config.copy(selectedPatterns = next)
            }
        }
    }

    fun selectAllPatterns() {
        updateConfig(RegenerationMode.RESOLVE) {
            it.copy(
                patternsRandomized = false,
                selectedPatterns = ArtPattern.entries.toSet()
            )
        }
    }

    fun selectNonePatterns() {
        updateConfig(RegenerationMode.RESOLVE) {
            it.copy(
                patternsRandomized = false,
                selectedPatterns = emptySet()
            )
        }
    }

    fun setPatternWeightMode(mode: PatternWeightMode) {
        updateConfig(RegenerationMode.PRESERVE) { it.copy(patternWeightMode = mode) }
    }

    fun setColorFillMode(mode: ColorFillMode) {
        updateConfig(RegenerationMode.PRESERVE) { it.copy(colorFillMode = mode) }
    }

    fun setPaletteMode(mode: PaletteMode) {
        if (_uiState.value.config.paletteMode == mode) return
        val updated = _uiState.value.config.copy(paletteMode = mode)
        _uiState.update { it.copy(config = updated) }
        scheduleConfigPersist(updated)
        if (mode == PaletteMode.RANDOMIZED) {
            shuffleColors()
        } else {
            regenerateResolving()
        }
    }

    fun togglePalette(paletteId: String) {
        updateConfig(RegenerationMode.RESOLVE) { config ->
            val next = if (paletteId in config.selectedPaletteIds) {
                config.selectedPaletteIds - paletteId
            } else {
                config.selectedPaletteIds + paletteId
            }
            config.copy(
                selectedPaletteIds = next.ifEmpty { setOf(paletteId) },
                paletteMode = PaletteMode.PALETTE_LIST
            )
        }
    }

    fun selectAllPalettes() {
        val all = _uiState.value.userPalettes.map { it.id }.toSet()
        updateConfig(RegenerationMode.RESOLVE) {
            it.copy(selectedPaletteIds = all, paletteMode = PaletteMode.PALETTE_LIST)
        }
    }

    fun selectNonePalettes() {
        updateConfig(RegenerationMode.RESOLVE) {
            it.copy(selectedPaletteIds = emptySet(), paletteMode = PaletteMode.PALETTE_LIST)
        }
    }

    // ---------------------------------------------------------------------
    // Auto-change / tray
    // ---------------------------------------------------------------------

    fun setAutoChangeEnabled(enabled: Boolean) {
        val state = _uiState.value
        val now = clock()
        val interval = AutoChangePolicy.clampIntervalMinutes(state.autoChange.intervalMinutes)
        val next = if (enabled) {
            val existing = state.autoChange.nextRunEpochMillis
            if (AutoChangePolicy.isValidFutureTarget(existing, now, interval)) {
                existing
            } else {
                AutoChangePolicy.nextRunFrom(now, interval)
            }
        } else {
            0L
        }
        updateAutoChange(
            state.autoChange.copy(
                enabled = enabled,
                nextRunEpochMillis = next,
                lastError = null
            )
        )
    }

    fun setAutoInterval(minutes: Int) {
        val safeMinutes = AutoChangePolicy.clampIntervalMinutes(minutes)
        val state = _uiState.value
        val now = clock()
        val next = if (state.autoChange.enabled) {
            AutoChangePolicy.nextRunFrom(
                maxOf(now, state.autoChange.lastRunEpochMillis),
                safeMinutes
            )
        } else {
            0L
        }
        updateAutoChange(
            state.autoChange.copy(
                intervalMinutes = safeMinutes,
                nextRunEpochMillis = next,
                lastError = null
            )
        )
    }

    fun setRunAtLogin(enabled: Boolean) {
        val result = autostartManager.setEnabled(enabled)
        if (result.isSuccess) {
            updateAutoChange(_uiState.value.autoChange.copy(runAtLogin = enabled))
        } else {
            postStatus(
                UiMessage(
                    "desktop_autostart_unavailable",
                    listOf(result.exceptionOrNull()?.localizedMessage ?: "Unavailable")
                )
            )
        }
    }

    fun setKeepInTray(enabled: Boolean) {
        updateAutoChange(_uiState.value.autoChange.copy(keepInTray = enabled))
    }

    fun setAutoTargets(monitorIds: Set<String>) {
        val display = _uiState.value.display.copy(targetMonitorIds = monitorIds)
        _uiState.update { it.copy(display = display) }
        scheduleUiPersist { it.copy(display = display) }
    }

    fun toggleAutoTargetMonitor(monitorId: String) {
        val state = _uiState.value
        val allIds = state.monitors.map { it.id }.toSet()
        val current = state.display.targetMonitorIds.ifEmpty { allIds }
        val next = if (monitorId in current) current - monitorId else current + monitorId
        if (next.isEmpty()) return
        setAutoTargets(next)
    }

    fun changeWallpaperNow() {
        scope.launch { performAutoRotation(force = true) }
    }

    suspend fun performAutoRotation(force: Boolean): ApplyResult? {
        return autoRotationMutex.withLock {
            val state = _uiState.value
            if (!force && !state.autoChange.enabled) return@withLock null
            if (state.current == null) {
                return@withLock ApplyResult(false, 0, "Wallpaper is still loading")
            }
            val now = clock()
            val interval = AutoChangePolicy.clampIntervalMinutes(state.autoChange.intervalMinutes)
            if (!force && AutoChangePolicy.shouldSkip(
                    lastRunEpochMillis = state.autoChange.lastRunEpochMillis,
                    nowEpochMillis = now,
                    minutes = interval
                )
            ) {
                updateAutoChange(
                    state.autoChange.copy(
                        nextRunEpochMillis = AutoChangePolicy.nextRunFrom(
                            maxOf(now, state.autoChange.lastRunEpochMillis),
                            interval
                        )
                    )
                )
                return@withLock ApplyResult(true, 0, "Skipped duplicate rotation")
            }

            val rendered = runCatching {
                prefetchJob?.cancel()
                aheadQueue.pollFirst() ?: createNewRenderedSet()
            }.getOrElse { error ->
                AppLog.error("Auto-change generation failed", error)
                val failed = state.autoChange.copy(
                    lastError = error.localizedMessage ?: "Generation failed",
                    nextRunEpochMillis = AutoChangePolicy.nextRunFrom(now, interval)
                )
                updateAutoChange(failed)
                postStatus(
                    UiMessage(
                        "desktop_error_generate",
                        listOf(error.localizedMessage ?: "Unknown error")
                    )
                )
                return@withLock ApplyResult(false, 0, "Generation failed")
            }

            commitRenderedSet(rendered, pushHistory = true, persist = true)

            val targetIds = state.display.targets(state.monitors).map { it.id }.toSet()
            val targetItems = rendered.set.items
                .filter { it.monitorId in targetIds }
                .ifEmpty { rendered.set.items }

            val result = runCatching {
                val files = withContext(Dispatchers.Default) {
                    fileStore.renderSet(rendered.set.copy(items = targetItems))
                }
                val applyResult = backend.apply(files, state.display.fitMode)
                if (applyResult.success) {
                    val measured = files.associate { it.item.monitorId to it.measuredBlackPercent }
                    val updatedItems = rendered.set.items.map { item ->
                        measured[item.monitorId]?.let { item.copy(measuredBlackPercent = it) } ?: item
                    }
                    val updatedRenderedItems = rendered.items.map { item ->
                        measured[item.item.monitorId]?.let {
                            item.copy(
                                item = item.item.copy(measuredBlackPercent = it),
                                measuredBlackPercent = it
                            )
                        } ?: item
                    }
                    val updatedSet = rendered.set.copy(items = updatedItems)
                    _uiState.update {
                        it.copy(
                            current = RenderedSet(updatedSet, updatedRenderedItems),
                            isApplying = false
                        )
                    }
                    val auto = state.autoChange.copy(
                        lastRunEpochMillis = now,
                        nextRunEpochMillis = if (state.autoChange.enabled) {
                            AutoChangePolicy.nextRunFrom(now, interval)
                        } else {
                            0L
                        },
                        lastError = null
                    )
                    _uiState.update { it.copy(autoChange = auto) }
                    store.update { it.copy(currentSet = updatedSet, autoChange = auto) }
                    fileStore.cleanup(
                        keepSetIds = (
                            listOf(updatedSet.id) + past.take(3).map { set -> set.id }
                            ).toSet()
                    )
                    val average = files.map { it.measuredBlackPercent }.average().roundToInt()
                    postStatus(
                        UiMessage(
                            "msg_wallpaper_applied",
                            listOf("${files.size} display(s)", average)
                        )
                    )
                    applyResult
                } else {
                    val auto = state.autoChange.copy(
                        lastRunEpochMillis = now,
                        nextRunEpochMillis = AutoChangePolicy.nextRunFrom(now, interval),
                        lastError = (listOf(applyResult.message) + applyResult.errors)
                            .joinToString(" — ")
                    )
                    _uiState.update { it.copy(autoChange = auto) }
                    store.update { it.copy(autoChange = auto) }
                    postStatus(
                        UiMessage(
                            "desktop_error_apply",
                            listOf(auto.lastError ?: applyResult.message)
                        )
                    )
                    applyResult
                }
            }.getOrElse { error ->
                AppLog.error("Auto-change apply failed", error)
                val auto = state.autoChange.copy(
                    lastRunEpochMillis = now,
                    nextRunEpochMillis = AutoChangePolicy.nextRunFrom(now, interval),
                    lastError = error.localizedMessage ?: "Apply failed"
                )
                updateAutoChange(auto)
                postStatus(
                    UiMessage(
                        "desktop_error_apply",
                        listOf(error.localizedMessage ?: "Unknown error")
                    )
                )
                ApplyResult(false, 0, "Apply failed")
            }

            result
        }
    }

    private fun updateAutoChange(settings: com.cptodix.rimalux.desktop.model.AutoChangeSettings) {
        _uiState.update { it.copy(autoChange = settings) }
        scope.launch {
            store.update { it.copy(autoChange = settings) }
        }
    }

    private fun startAutoChangeLoop() {
        autoChangeJob = scope.launch {
            while (isActive) {
                delay(15_000L)
                val state = _uiState.value
                if (!state.autoChange.enabled || state.current == null) continue
                val now = clock()
                val next = state.autoChange.nextRunEpochMillis
                if (next == 0L) {
                    updateAutoChange(
                        state.autoChange.copy(
                            nextRunEpochMillis = AutoChangePolicy.nextRunFrom(
                                now,
                                state.autoChange.intervalMinutes
                            )
                        )
                    )
                } else if (now >= next) {
                    performAutoRotation(force = false)
                }
            }
        }
    }

    private suspend fun ensureAutoSchedule() {
        val state = _uiState.value
        if (!state.autoChange.enabled || state.current == null) return
        val now = clock()
        val interval = AutoChangePolicy.clampIntervalMinutes(state.autoChange.intervalMinutes)
        val next = state.autoChange.nextRunEpochMillis
        when {
            next == 0L -> updateAutoChange(
                state.autoChange.copy(
                    nextRunEpochMillis = AutoChangePolicy.nextRunFrom(now, interval)
                )
            )

            next <= now && state.autoChange.lastRunEpochMillis > 0L &&
                now - state.autoChange.lastRunEpochMillis >= AutoChangePolicy.intervalMillis(interval) -> {
                performAutoRotation(force = false)
            }

            next <= now -> updateAutoChange(
                state.autoChange.copy(
                    nextRunEpochMillis = AutoChangePolicy.nextRunFrom(now, interval)
                )
            )
        }
    }

    fun setPitchBlackMode(mode: PitchBlackMode) {
        updateConfig(RegenerationMode.PRESERVE) { it.copy(pitchBlackMode = mode) }
    }

    fun setPitchBlackRatio(ratio: Float) {
        updateConfig(RegenerationMode.PRESERVE, debounceMs = 120) {
            it.copy(pitchBlackRatio = ratio.coerceIn(0f, 0.95f))
        }
    }

    fun setPitchBlackRange(min: Float, max: Float) {
        updateConfig(RegenerationMode.PRESERVE, debounceMs = 120) {
            val lo = minOf(min, max).coerceIn(0f, 0.95f)
            val hi = maxOf(min, max).coerceIn(lo, 0.95f)
            it.copy(pitchBlackRangeMin = lo, pitchBlackRangeMax = hi)
        }
    }

    fun shuffleColors() {
        val current = _uiState.value.current ?: return
        val state = _uiState.value
        val baseSeed = current.set.items.firstOrNull()?.seed ?: System.nanoTime()
        val shared = RandomPaletteGenerator.generate(baseSeed xor 0x5DEECE66DL)
        val overrides = current.set.items.associate { item ->
            val colors = if (state.display.matchAcrossDisplays) {
                shared
            } else {
                RandomPaletteGenerator.generate(item.seed xor System.nanoTime())
            }
            item.monitorId to colors
        }
        regeneratePreserving(colorsOverride = overrides)
    }

    fun addCustomPalette(name: String, colors: List<Long>) {
        val safeColors = colors.filter { (it and 0xFFFFFFL) != 0L }.take(6)
        if (safeColors.size < 2) return
        val palette = ColorPalette(
            id = "custom_${System.currentTimeMillis()}",
            name = name.ifBlank { "Custom" },
            colors = safeColors
        )
        val state = _uiState.value
        val palettes = listOf(palette) + state.userPalettes.filterNot { it.id == palette.id }
        deletedPaletteIds = deletedPaletteIds - palette.id
        _uiState.update { it.copy(userPalettes = palettes) }
        scope.launch {
            store.update {
                it.copy(
                    userPalettes = palettes,
                    deletedPaletteIds = deletedPaletteIds,
                    generation = it.generation.copy(
                        paletteMode = PaletteMode.PALETTE_LIST,
                        selectedPaletteIds = it.generation.selectedPaletteIds + palette.id
                    )
                )
            }
        }
        updateConfig(RegenerationMode.RESOLVE) {
            it.copy(
                paletteMode = PaletteMode.PALETTE_LIST,
                selectedPaletteIds = it.selectedPaletteIds + palette.id
            )
        }
    }

    fun deletePalette(paletteId: String) {
        val state = _uiState.value
        if (state.userPalettes.size <= 1) return
        val palettes = state.userPalettes.filterNot { it.id == paletteId }
        if (palettes.isEmpty()) return
        deletedPaletteIds = deletedPaletteIds + paletteId
        _uiState.update { it.copy(userPalettes = palettes) }
        val fallback = palettes.first().id
        val selected = state.config.selectedPaletteIds - paletteId
        updateConfig(RegenerationMode.RESOLVE) {
            it.copy(
                selectedPaletteIds = selected.ifEmpty { setOf(fallback) }
            )
        }
        scope.launch {
            store.update {
                it.copy(
                    userPalettes = palettes,
                    deletedPaletteIds = deletedPaletteIds
                )
            }
        }
    }

    private fun updateConfig(
        mode: RegenerationMode,
        debounceMs: Long = 0,
        transform: (GenerationConfig) -> GenerationConfig
    ) {
        val updated = transform(_uiState.value.config)
        _uiState.update { it.copy(config = updated) }
        scheduleConfigPersist(updated)
        when (mode) {
            RegenerationMode.PRESERVE -> regeneratePreserving(debounceMs = debounceMs)
            RegenerationMode.RESOLVE -> regenerateResolving(debounceMs = debounceMs)
        }
    }

    private fun scheduleConfigPersist(config: GenerationConfig) {
        configPersistJob?.cancel()
        configPersistJob = scope.launch {
            delay(250)
            store.update { it.copy(generation = config) }
        }
    }

    private fun generateNewSet(pushHistory: Boolean, persist: Boolean) {
        launchSynthesis {
            buildNewSet(pushHistory = pushHistory, persist = persist)
        }
    }

    private suspend fun buildNewSet(pushHistory: Boolean, persist: Boolean) {
        commitRenderedSet(
            rendered = createNewRenderedSet(),
            pushHistory = pushHistory,
            persist = persist
        )
    }

    private suspend fun createNewRenderedSet(): RenderedSet {
        val state = _uiState.value
        val targets = state.monitors
        val palettes = state.userPalettes
        val spec = state.config.toCoreSpec()
        val usedSeeds = HashSet<Long>()
        val usedPatterns = ArrayList<ArtPattern>()
        val usedColors = ArrayList<List<Long>>()
        past.forEach { set -> set.items.forEach { usedSeeds += it.seed } }
        aheadQueue.forEach { set -> set.set.items.forEach { usedSeeds += it.seed } }
        state.current?.set?.items?.forEach { usedSeeds += it.seed }

        val primarySeed = newSeed(usedSeeds)
        usedSeeds += primarySeed
        val generated = ArrayList<GeneratedItem>(targets.size)
        for ((index, monitor) in targets.withIndex()) {
            val seed = if (state.display.matchAcrossDisplays) {
                primarySeed
            } else {
                newSeed(usedSeeds).also { usedSeeds += it }
            }
            val (width, height) = resolveSize(monitor, state.display)
            val synth = WallpaperGeneratorEngine.synthesize(
                seed = seed,
                spec = spec,
                availablePalettes = palettes,
                aspectRatio = height.toFloat() / width.toFloat(),
                excludePattern = if (state.display.matchAcrossDisplays) null else usedPatterns.lastOrNull(),
                excludeColors = if (state.display.matchAcrossDisplays) null else usedColors.lastOrNull()
            )
            usedPatterns += synth.pattern
            usedColors += synth.paletteColors
            generated += GeneratedItem(
                item = buildItem(monitor, index, synth, width, height, state.display),
                synthesized = synth
            )
        }
        val timestamp = System.currentTimeMillis()
        val setId = "gen_${timestamp}_${primarySeed.toString(16)}"
        return renderGenerated(generated, setId, timestamp, wasAutoScheduled = false)
    }

    private fun regeneratePreserving(
        debounceMs: Long = 0,
        colorsOverride: Map<String, List<Long>>? = null
    ) {
        launchSynthesis(debounceMs) {
            val state = _uiState.value
            val current = state.current
            if (current == null) {
                buildNewSet(pushHistory = false, persist = true)
                return@launchSynthesis
            }
            val items = current.set.items
            val primary = items.firstOrNull()
            val targets = state.monitors
            val generated = ArrayList<GeneratedItem>(targets.size)
            for ((index, monitor) in targets.withIndex()) {
                val existing = items.firstOrNull { it.monitorId == monitor.id } ?: primary
                val seed = existing?.seed ?: newSeed(emptySet())
                val pattern = existing?.pattern ?: MathPatternGenerator.resolvePatternForSeed(
                    seed = seed,
                    patternsRandomized = state.config.patternsRandomized,
                    selectedPatterns = state.config.selectedPatterns
                )
                val colors = colorsOverride?.get(monitor.id)
                    ?: existing?.paletteColors
                    ?: RandomPaletteGenerator.generate(seed)
                val (width, height) = resolveSize(monitor, state.display)
                val ratio = state.config.resolvePitchBlackRatio(Random(seed xor 0x51ED270BL))
                val intricacy = existing?.intricacy
                    ?: WallpaperGeneratorEngine.sampleIntricacyForSeed(seed)
                val synth = WallpaperGeneratorEngine.synthesizeFromParameters(
                    seed = seed,
                    pattern = pattern,
                    paletteColors = colors,
                    colorFillMode = state.config.colorFillMode,
                    pitchBlackRatio = ratio,
                    intricacy = intricacy,
                    aspectRatio = height.toFloat() / width.toFloat(),
                    patternWeightMode = state.config.patternWeightMode
                )
                generated += GeneratedItem(
                    item = buildItem(monitor, index, synth, width, height, state.display),
                    synthesized = synth
                )
            }
            val timestamp = System.currentTimeMillis()
            val setId = "gen_${timestamp}_${generated.firstOrNull()?.item?.seed?.toString(16) ?: "0"}"
            commit(generated, setId, timestamp, pushHistory = false, persist = true)
        }
    }

    private fun regenerateResolving(debounceMs: Long = 0) {
        launchSynthesis(debounceMs) {
            val state = _uiState.value
            val current = state.current
            if (current == null) {
                buildNewSet(pushHistory = false, persist = true)
                return@launchSynthesis
            }
            val seeds = current.set.items.associate { it.monitorId to it.seed }
            val spec = state.config.toCoreSpec()
            val generated = ArrayList<GeneratedItem>(state.monitors.size)
            val usedSeeds = HashSet<Long>()
            for ((index, monitor) in state.monitors.withIndex()) {
                val seed = seeds[monitor.id] ?: newSeed(usedSeeds)
                usedSeeds += seed
                val (width, height) = resolveSize(monitor, state.display)
                val synth = WallpaperGeneratorEngine.synthesize(
                    seed = seed,
                    spec = spec,
                    availablePalettes = state.userPalettes,
                    aspectRatio = height.toFloat() / width.toFloat()
                )
                generated += GeneratedItem(
                    item = buildItem(monitor, index, synth, width, height, state.display),
                    synthesized = synth
                )
            }
            val timestamp = System.currentTimeMillis()
            val setId = "gen_${timestamp}_${generated.firstOrNull()?.item?.seed?.toString(16) ?: "0"}"
            commit(generated, setId, timestamp, pushHistory = false, persist = true)
        }
    }

    private fun restoreSet(set: WallpaperSet, persist: Boolean) {
        launchSynthesis {
            val state = _uiState.value
            val primary = set.items.firstOrNull()
            val generated = ArrayList<GeneratedItem>(state.monitors.size)
            for ((index, monitor) in state.monitors.withIndex()) {
                val existing = set.items.firstOrNull { it.monitorId == monitor.id } ?: primary
                val seed = existing?.seed ?: newSeed(emptySet())
                val pattern = existing?.pattern ?: ArtPattern.SILK_RIBBONS
                val colors = existing?.paletteColors ?: RandomPaletteGenerator.generate(seed)
                val (width, height) = resolveSize(monitor, state.display)
                val synth = WallpaperGeneratorEngine.synthesizeFromParameters(
                    seed = seed,
                    pattern = pattern,
                    paletteColors = colors,
                    colorFillMode = existing?.colorFillMode ?: state.config.colorFillMode,
                    pitchBlackRatio = existing?.pitchBlackRatio
                        ?: state.config.resolvePitchBlackRatio(Random(seed)),
                    intricacy = existing?.intricacy
                        ?: WallpaperGeneratorEngine.sampleIntricacyForSeed(seed),
                    aspectRatio = height.toFloat() / width.toFloat(),
                    patternWeightMode = existing?.patternWeightMode ?: state.config.patternWeightMode
                )
                generated += GeneratedItem(
                    item = buildItem(monitor, index, synth, width, height, state.display),
                    synthesized = synth
                )
            }
            commit(
                generated = generated,
                setId = set.id,
                timestamp = set.timestampMillis,
                pushHistory = false,
                persist = persist,
                wasAutoScheduled = set.wasAutoScheduled
            )
        }
    }

    private fun buildItem(
        monitor: MonitorInfo,
        index: Int,
        synth: SynthesizedWallpaper,
        widthPx: Int,
        heightPx: Int,
        display: DisplaySettings
    ): WallpaperItem {
        val screenIndex = display.plasmaScreenIndexById[monitor.id]
            ?: monitor.plasmaScreenIndex
            ?: index
        return WallpaperItem(
            monitorId = monitor.id,
            monitorName = monitor.name,
            screenIndex = screenIndex,
            seed = synth.seed,
            pattern = synth.pattern,
            paletteColors = synth.paletteColors,
            colorFillMode = synth.colorFillMode,
            pitchBlackRatio = synth.pitchBlackRatio,
            intricacy = synth.intricacy,
            patternWeightMode = synth.patternWeightMode,
            widthPx = widthPx,
            heightPx = heightPx
        )
    }

    private suspend fun renderGenerated(
        generated: List<GeneratedItem>,
        setId: String,
        timestamp: Long,
        wasAutoScheduled: Boolean = false
    ): RenderedSet {
        val renderedItems = ArrayList<RenderedItem>(generated.size)
        for (entry in generated) {
            val preview = withContext(Dispatchers.Default) {
                SkiaWallpaperRenderer.renderPreview(entry.synthesized, maxLongEdgePx = 1920)
            }
            val imageBitmap = preview.image.toComposeImageBitmap()
            runCatching { preview.image.close() }
            renderedItems += RenderedItem(
                item = entry.item.copy(measuredBlackPercent = preview.measuredBlackPercent),
                synthesized = entry.synthesized,
                preview = imageBitmap,
                measuredBlackPercent = preview.measuredBlackPercent
            )
        }
        val set = WallpaperSet(
            id = setId,
            timestampMillis = timestamp,
            items = renderedItems.map { it.item },
            wasAutoScheduled = wasAutoScheduled
        )
        return RenderedSet(set, renderedItems)
    }

    private suspend fun commit(
        generated: List<GeneratedItem>,
        setId: String,
        timestamp: Long,
        pushHistory: Boolean,
        persist: Boolean,
        wasAutoScheduled: Boolean = false
    ) {
        commitRenderedSet(
            rendered = renderGenerated(generated, setId, timestamp, wasAutoScheduled),
            pushHistory = pushHistory,
            persist = persist
        )
    }

    private suspend fun commitRenderedSet(
        rendered: RenderedSet,
        pushHistory: Boolean,
        persist: Boolean
    ) {
        if (pushHistory) {
            _uiState.value.current?.let { current ->
                past.removeIf { it.id == current.set.id }
                past.addFirst(current.set)
                pastRenderedCache[current.set.id] = current
                while (past.size > 24) {
                    past.pollLast()
                }
            }
        }
        val history = past.toList()
        _uiState.update {
            it.copy(
                current = rendered,
                history = history,
                canGoBack = past.isNotEmpty(),
                canGoForward = false,
                isGenerating = false,
                previewError = null,
                aheadReadyCount = aheadQueue.size
            )
        }
        if (persist) {
            val state = _uiState.value
            store.update {
                it.copy(
                    generation = state.config,
                    display = state.display,
                    ui = state.ui,
                    autoChange = state.autoChange,
                    currentSet = rendered.set,
                    history = history,
                    userPalettes = state.userPalettes
                )
            }
        }
        ensurePrefetch()
    }

    private fun ensurePrefetch() {
        if (prefetchJob?.isActive == true) return
        if (aheadQueue.size >= PREFETCH_TARGET) return
        prefetchJob = scope.launch {
            try {
                while (aheadQueue.size < PREFETCH_TARGET && isActive) {
                    val rendered = createNewRenderedSet()
                    if (!isActive) break
                    aheadQueue.addLast(rendered)
                    _uiState.update { it.copy(aheadReadyCount = aheadQueue.size) }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                AppLog.error("Wallpaper prefetch failed", error)
            }
        }
    }

    private fun launchSynthesis(debounceMs: Long = 0, block: suspend () -> Unit) {
        prefetchJob?.cancel()
        aheadQueue.clear()
        _uiState.update { it.copy(aheadReadyCount = 0) }
        synthesisJob?.cancel()
        synthesisJob = scope.launch {
            if (debounceMs > 0) delay(debounceMs)
            generationMutex.withLock {
                _uiState.update { it.copy(isGenerating = true, previewError = null) }
                try {
                    block()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Throwable) {
                    AppLog.error("Wallpaper synthesis failed", error)
                    _uiState.update {
                        it.copy(
                            isGenerating = false,
                            previewError = error.localizedMessage ?: "Unknown synthesis error",
                            statusMessage = UiMessage(
                                "desktop_error_generate",
                                listOf(error.localizedMessage ?: "Unknown error")
                            )
                        )
                    }
                } finally {
                    _uiState.update { it.copy(isGenerating = false) }
                }
            }
        }
    }

    // ---------------------------------------------------------------------
    // Display / settings
    // ---------------------------------------------------------------------

    fun selectMonitor(monitorId: String) {
        val display = _uiState.value.display.copy(selectedMonitorId = monitorId)
        _uiState.update { it.copy(display = display, selectedMonitorId = monitorId) }
        scheduleUiPersist { it.copy(display = display) }
    }

    fun setResolutionMode(mode: ResolutionMode) {
        val display = _uiState.value.display.copy(resolutionMode = mode)
        _uiState.update { it.copy(display = display) }
        scheduleUiPersist { it.copy(display = display) }
        regeneratePreserving()
    }

    fun setCustomResolution(width: Int, height: Int) {
        val display = _uiState.value.display.copy(
            resolutionMode = ResolutionMode.CUSTOM,
            customWidth = width.coerceIn(320, 8192),
            customHeight = height.coerceIn(240, 8192)
        )
        _uiState.update { it.copy(display = display) }
        scheduleUiPersist { it.copy(display = display) }
        regeneratePreserving()
    }

    fun setFitMode(mode: FitMode) {
        val display = _uiState.value.display.copy(fitMode = mode)
        _uiState.update { it.copy(display = display) }
        scheduleUiPersist { it.copy(display = display) }
    }

    fun setMatchAcrossDisplays(enabled: Boolean) {
        val display = _uiState.value.display.copy(matchAcrossDisplays = enabled)
        _uiState.update { it.copy(display = display) }
        scheduleUiPersist { it.copy(display = display) }
        if (enabled) {
            regenerateMatching()
        } else {
            regenerateUniqueFromCurrent()
        }
    }

    private fun regenerateMatching() {
        launchSynthesis {
            val state = _uiState.value
            val current = state.current
            if (current == null) {
                buildNewSet(pushHistory = false, persist = true)
                return@launchSynthesis
            }
            val primary = current.set.items.firstOrNull() ?: return@launchSynthesis
            val generated = ArrayList<GeneratedItem>(state.monitors.size)
            for ((index, monitor) in state.monitors.withIndex()) {
                val (width, height) = resolveSize(monitor, state.display)
                val synth = WallpaperGeneratorEngine.synthesizeFromParameters(
                    seed = primary.seed,
                    pattern = primary.pattern,
                    paletteColors = primary.paletteColors,
                    colorFillMode = state.config.colorFillMode,
                    pitchBlackRatio = primary.pitchBlackRatio,
                    intricacy = primary.intricacy,
                    aspectRatio = height.toFloat() / width.toFloat(),
                    patternWeightMode = state.config.patternWeightMode
                )
                generated += GeneratedItem(
                    item = buildItem(monitor, index, synth, width, height, state.display),
                    synthesized = synth
                )
            }
            val timestamp = System.currentTimeMillis()
            commit(
                generated,
                "gen_${timestamp}_${primary.seed.toString(16)}",
                timestamp,
                pushHistory = false,
                persist = true
            )
        }
    }

    private fun regenerateUniqueFromCurrent() {
        launchSynthesis {
            val state = _uiState.value
            val current = state.current
            if (current == null) {
                buildNewSet(pushHistory = false, persist = true)
                return@launchSynthesis
            }
            val primary = current.set.items.firstOrNull()
            val usedSeeds = HashSet<Long>()
            val usedPatterns = ArrayList<ArtPattern>()
            val usedColors = ArrayList<List<Long>>()
            val generated = ArrayList<GeneratedItem>(state.monitors.size)
            for ((index, monitor) in state.monitors.withIndex()) {
                val seed = if (index == 0 && primary != null) primary.seed else newSeed(usedSeeds)
                usedSeeds += seed
                val (width, height) = resolveSize(monitor, state.display)
                val synth = WallpaperGeneratorEngine.synthesize(
                    seed = seed,
                    spec = state.config.toCoreSpec(),
                    availablePalettes = state.userPalettes,
                    aspectRatio = height.toFloat() / width.toFloat(),
                    excludePattern = usedPatterns.lastOrNull(),
                    excludeColors = usedColors.lastOrNull()
                )
                usedPatterns += synth.pattern
                usedColors += synth.paletteColors
                generated += GeneratedItem(
                    item = buildItem(monitor, index, synth, width, height, state.display),
                    synthesized = synth
                )
            }
            val timestamp = System.currentTimeMillis()
            commit(
                generated,
                "gen_${timestamp}_${generated.firstOrNull()?.item?.seed?.toString(16) ?: "0"}",
                timestamp,
                pushHistory = false,
                persist = true
            )
        }
    }

    fun setScreen(screen: DesktopScreen) {
        _uiState.update { it.copy(screen = screen) }
        scheduleUiPersist { it.copy(ui = it.ui.copy(lastScreen = screen)) }
    }

    fun setLanguage(language: AppLanguage) {
        Locale.setDefault(language.toLocale())
        _uiState.update {
            it.copy(
                languageTag = language.languageTag,
                ui = it.ui.copy(languageTag = language.languageTag)
            )
        }
        scheduleUiPersist { it.copy(ui = it.ui.copy(languageTag = language.languageTag)) }
    }

    fun setCustomWallpaperCommand(command: String) {
        val ui = _uiState.value.ui.copy(customWallpaperCommand = command)
        _uiState.update { it.copy(ui = ui) }
        backend = backendFactory.detect(_uiState.value.toSettings().copy(ui = ui))
        _uiState.update { it.copy(backendInfo = backend.info) }
        scheduleUiPersist { it.copy(ui = ui) }
    }

    fun setBackendOverride(override: String) {
        val ui = _uiState.value.ui.copy(backendOverride = override)
        _uiState.update { it.copy(ui = ui) }
        backend = backendFactory.detect(_uiState.value.toSettings().copy(ui = ui))
        _uiState.update { it.copy(backendInfo = backend.info) }
        scheduleUiPersist { it.copy(ui = ui) }
    }

    fun refreshMonitors() {
        scope.launch {
            val detected = runCatching { monitorProvider.listMonitors() }.getOrDefault(emptyList())
            val monitors = detected.ifEmpty { listOf(fallbackMonitor()) }
            val state = _uiState.value
            val selected = state.display.selected(monitors)?.id ?: monitors.first().id
            _uiState.update {
                it.copy(
                    monitors = monitors,
                    selectedMonitorId = selected,
                    display = it.display.copy(selectedMonitorId = selected)
                )
            }
            scheduleUiPersist { it.copy(display = it.display.copy(selectedMonitorId = selected)) }
            regeneratePreserving()
        }
    }

    fun refreshBackend() {
        backend = backendFactory.detect(_uiState.value.toSettings())
        _uiState.update { it.copy(backendInfo = backend.info) }
    }

    private fun scheduleUiPersist(transform: (DesktopSettings) -> DesktopSettings) {
        uiPersistJob?.cancel()
        uiPersistJob = scope.launch {
            delay(200)
            store.update(transform)
        }
    }

    // ---------------------------------------------------------------------
    // Apply / export
    // ---------------------------------------------------------------------

    fun applyWallpaper(target: ApplyTarget) {
        scope.launch {
            applyMutex.withLock {
                val state = _uiState.value
                val current = state.current
                if (current == null) {
                    postStatus(UiMessage("desktop_error_no_wallpaper"))
                    return@withLock
                }
                val targetItems = when (target) {
                    ApplyTarget.ALL_DISPLAYS -> current.set.items
                    ApplyTarget.SELECTED_DISPLAY -> current.set.items.filter {
                        it.monitorId == state.selectedMonitorId
                    }.ifEmpty { current.set.items.take(1) }
                }
                if (targetItems.isEmpty()) {
                    postStatus(UiMessage("desktop_error_no_wallpaper"))
                    return@withLock
                }
                _uiState.update { it.copy(isApplying = true) }
                try {
                    val files = withContext(Dispatchers.Default) {
                        fileStore.renderSet(current.set.copy(items = targetItems))
                    }
                    val result = backend.apply(files, state.display.fitMode)
                    if (result.success) {
                        val measured = files.associate { it.item.monitorId to it.measuredBlackPercent }
                        val updatedItems = current.set.items.map { item ->
                            measured[item.monitorId]?.let { item.copy(measuredBlackPercent = it) } ?: item
                        }
                        val updatedRendered = current.items.map { rendered ->
                            measured[rendered.item.monitorId]?.let {
                                rendered.copy(
                                    item = rendered.item.copy(measuredBlackPercent = it),
                                    measuredBlackPercent = it
                                )
                            } ?: rendered
                        }
                        val updatedSet = current.set.copy(items = updatedItems)
                        _uiState.update {
                            it.copy(
                                current = RenderedSet(updatedSet, updatedRendered),
                                isApplying = false
                            )
                        }
                        val auto = if (state.autoChange.enabled) {
                            state.autoChange.copy(
                                lastRunEpochMillis = clock(),
                                nextRunEpochMillis = AutoChangePolicy.nextRunFrom(
                                    clock(),
                                    state.autoChange.intervalMinutes
                                ),
                                lastError = null
                            )
                        } else {
                            state.autoChange
                        }
                        _uiState.update { it.copy(autoChange = auto) }
                        store.update {
                            it.copy(
                                currentSet = updatedSet,
                                autoChange = auto
                            )
                        }
                        fileStore.cleanup(
                            keepSetIds = (
                                listOf(updatedSet.id) +
                                    past.take(3).map { set -> set.id }
                                ).toSet()
                        )
                        val average = files.map { it.measuredBlackPercent }.average().roundToInt()
                        val targetName = if (target == ApplyTarget.SELECTED_DISPLAY) {
                            state.selectedMonitor?.name ?: "Desktop"
                        } else {
                            "${files.size} display(s)"
                        }
                        postStatus(
                            UiMessage(
                                "msg_wallpaper_applied",
                                listOf(targetName, average)
                            )
                        )
                    } else {
                        AppLog.error(
                            "Wallpaper apply failed: ${result.message}; ${result.errors.joinToString("; ")}"
                        )
                        postStatus(
                            UiMessage(
                                "desktop_error_apply",
                                listOf((listOf(result.message) + result.errors).joinToString(" — "))
                            )
                        )
                        _uiState.update { it.copy(isApplying = false) }
                    }
                } catch (error: Throwable) {
                    AppLog.error("Wallpaper apply failed", error)
                    postStatus(
                        UiMessage(
                            "desktop_error_apply",
                            listOf(error.localizedMessage ?: "Unknown error")
                        )
                    )
                    _uiState.update { it.copy(isApplying = false) }
                }
            }
        }
    }

    fun savePngs(target: ApplyTarget) {
        scope.launch {
            val state = _uiState.value
            val current = state.current
            if (current == null) {
                postStatus(UiMessage("desktop_error_no_wallpaper"))
                return@launch
            }
            val targetItems = when (target) {
                ApplyTarget.ALL_DISPLAYS -> current.set.items
                ApplyTarget.SELECTED_DISPLAY -> current.set.items.filter {
                    it.monitorId == state.selectedMonitorId
                }.ifEmpty { current.set.items.take(1) }
            }
            _uiState.update { it.copy(isSaving = true) }
            try {
                val saved = withContext(Dispatchers.Default) {
                    val files = fileStore.renderSet(current.set.copy(items = targetItems))
                    fileStore.saveToPictures(files)
                }
                postStatus(
                    UiMessage(
                        "desktop_saved_count",
                        listOf(saved.size, paths.picturesDir.toString())
                    )
                )
            } catch (error: Throwable) {
                AppLog.error("PNG export failed", error)
                postStatus(
                    UiMessage(
                        "desktop_error_save",
                        listOf(error.localizedMessage ?: "Unknown error")
                    )
                )
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun openConfigFolder() = openInFileManager(paths.configDir)
    fun openCacheFolder() = openInFileManager(paths.cacheDir)
    fun openPicturesFolder() = openInFileManager(paths.picturesDir)
    fun openLogsFolder() = openInFileManager(paths.logsDir)

    fun dismissStatus() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    fun shutdown() {
        synthesisJob?.cancel()
        prefetchJob?.cancel()
        autoChangeJob?.cancel()
        scope.coroutineContext[Job]?.cancel()
    }

    private fun postStatus(message: UiMessage) {
        _uiState.update { it.copy(statusMessage = message) }
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private fun resolveSize(monitor: MonitorInfo, display: DisplaySettings): Pair<Int, Int> {
        val raw = when (display.resolutionMode) {
            ResolutionMode.AUTO -> monitor.widthPx to monitor.heightPx
            ResolutionMode.P1080 -> presetSize(monitor, 1920)
            ResolutionMode.P1440 -> presetSize(monitor, 2560)
            ResolutionMode.P2160 -> presetSize(monitor, 3840)
            ResolutionMode.CUSTOM -> display.customWidth to display.customHeight
        }
        return DeepBlackRatioEngine.clampDimensionsToMaxAxis(raw.first, raw.second)
    }

    private fun presetSize(monitor: MonitorInfo, longEdge: Int): Pair<Int, Int> {
        val aspect = monitor.aspectRatio
        return if (aspect <= 1f) {
            val width = longEdge
            val height = (width * aspect).roundToInt().coerceAtLeast(240)
            width to height
        } else {
            val height = longEdge
            val width = (height / aspect).roundToInt().coerceAtLeast(320)
            width to height
        }
    }

    private fun newSeed(excluded: Set<Long>): Long {
        val rng = Random(System.nanoTime() xor System.currentTimeMillis() xor excluded.hashCode().toLong())
        return WallpaperGeneratorEngine.nextDistinctSeed(rng, excluded + 0L)
    }

    private fun fallbackMonitor(): MonitorInfo = MonitorInfo(
        id = "virtual-primary",
        name = "Desktop",
        bounds = MonitorBounds(0, 0, 1920, 1080),
        widthPx = 1920,
        heightPx = 1080,
        scale = 1f,
        isPrimary = true,
        plasmaScreenIndex = 0
    )

    private fun StudioUiState.toSettings(): DesktopSettings = DesktopSettings(
        generation = config,
        display = display,
        ui = ui,
        autoChange = autoChange,
        currentSet = current?.set,
        history = history,
        userPalettes = userPalettes,
        deletedPaletteIds = deletedPaletteIds
    )
}
