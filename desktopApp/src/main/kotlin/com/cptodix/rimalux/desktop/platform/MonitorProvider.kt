package com.cptodix.rimalux.desktop.platform

import com.cptodix.rimalux.desktop.model.MonitorBounds
import com.cptodix.rimalux.desktop.model.MonitorInfo
import java.awt.GraphicsEnvironment
import kotlin.math.roundToInt
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

interface MonitorProvider {
    fun listMonitors(): List<MonitorInfo>
}

class AwtMonitorProvider : MonitorProvider {
    override fun listMonitors(): List<MonitorInfo> {
        return runCatching {
            val environment = GraphicsEnvironment.getLocalGraphicsEnvironment()
            val default = environment.defaultScreenDevice
            environment.screenDevices.mapIndexed { index, device ->
                val configuration = device.defaultConfiguration
                val bounds = configuration.bounds
                val transform = configuration.defaultTransform
                val scaleX = transform.scaleX.toFloat().takeIf { it > 0f } ?: 1f
                val scaleY = transform.scaleY.toFloat().takeIf { it > 0f } ?: 1f
                val widthPx = (bounds.width * scaleX).roundToInt().coerceAtLeast(320)
                val heightPx = (bounds.height * scaleY).roundToInt().coerceAtLeast(240)
                MonitorInfo(
                    id = "awt-${device.iDstring}",
                    name = device.iDstring.ifBlank { "Display ${index + 1}" },
                    bounds = MonitorBounds(bounds.x, bounds.y, bounds.width, bounds.height),
                    widthPx = widthPx,
                    heightPx = heightPx,
                    scale = scaleX,
                    isPrimary = device == default,
                    plasmaScreenIndex = index
                )
            }
        }.getOrElse { error ->
            AppLog.error("AWT monitor detection failed", error)
            emptyList()
        }
    }
}

/**
 * KDE-specific monitor provider using `kscreen-doctor -o -j`.
 *
 * It provides the physical mode resolution (important for HiDPI and rotated outputs), while AWT
 * remains the fallback.
 */
class KdeMonitorProvider(
    private val runner: ProcessRunner,
    private val json: Json = Json { ignoreUnknownKeys = true; isLenient = true }
) : MonitorProvider {

    override fun listMonitors(): List<MonitorInfo> {
        val executable = runner.findExecutable("kscreen-doctor") ?: return emptyList()
        val result = runner.run(listOf(executable, "-o", "-j"))
        if (!result.success) return emptyList()
        val raw = result.stdout
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        if (start < 0 || end <= start) return emptyList()
        val text = raw.substring(start, end + 1)
        val dto = runCatching {
            json.decodeFromString(KscreenRootDto.serializer(), text)
        }.getOrElse { error ->
            AppLog.warning("Could not parse kscreen-doctor JSON", error)
            return emptyList()
        }

        val enabled = dto.outputs.filter { it.connected && it.enabled }
        if (enabled.isEmpty()) return emptyList()
        val primaryIndex = enabled.indexOfFirst { it.priority == 1 }.takeIf { it >= 0 }
        return enabled.mapIndexed { index, output ->
            val mode = output.modes.firstOrNull { it.id == output.currentModeId }
            var width = mode?.size?.width ?: output.size.width
            var height = mode?.size?.height ?: output.size.height
            val rotation = output.rotation
            if (rotation == 2 || rotation == 8) {
                val tmp = width
                width = height
                height = tmp
            }
            val scale = output.scale.takeIf { it > 0f } ?: 1f
            val logicalWidth = (width / scale).roundToInt().coerceAtLeast(1)
            val logicalHeight = (height / scale).roundToInt().coerceAtLeast(1)
            MonitorInfo(
                id = "kde-${output.name}",
                name = output.name,
                bounds = MonitorBounds(
                    x = output.pos.x,
                    y = output.pos.y,
                    width = logicalWidth,
                    height = logicalHeight
                ),
                widthPx = width.coerceAtLeast(320),
                heightPx = height.coerceAtLeast(240),
                scale = scale,
                isPrimary = primaryIndex == index,
                plasmaScreenIndex = index
            )
        }
    }
}

class DefaultMonitorProvider(
    private val runner: ProcessRunner = ProcessRunner(),
    private val awt: MonitorProvider = AwtMonitorProvider()
) : MonitorProvider {
    override fun listMonitors(): List<MonitorInfo> {
        val desktop = System.getenv("XDG_CURRENT_DESKTOP").orEmpty().lowercase()
        if (desktop.contains("kde") && runner.hasExecutable("kscreen-doctor")) {
            val kde = runCatching { KdeMonitorProvider(runner).listMonitors() }.getOrDefault(emptyList())
            if (kde.isNotEmpty()) return kde
        }
        return awt.listMonitors()
    }
}

@Serializable
private data class KscreenRootDto(
    val outputs: List<KscreenOutputDto> = emptyList()
)

@Serializable
private data class KscreenOutputDto(
    val name: String = "",
    val connected: Boolean = false,
    val enabled: Boolean = false,
    val currentModeId: String = "",
    val pos: KscreenPointDto = KscreenPointDto(),
    val size: KscreenSizeDto = KscreenSizeDto(),
    val rotation: Int = 1,
    val scale: Float = 1f,
    val priority: Int = 0,
    val modes: List<KscreenModeDto> = emptyList()
)

@Serializable
private data class KscreenModeDto(
    val id: String = "",
    val name: String = "",
    val size: KscreenSizeDto = KscreenSizeDto()
)

@Serializable
private data class KscreenPointDto(
    val x: Int = 0,
    val y: Int = 0
)

@Serializable
private data class KscreenSizeDto(
    val width: Int = 0,
    val height: Int = 0
)
