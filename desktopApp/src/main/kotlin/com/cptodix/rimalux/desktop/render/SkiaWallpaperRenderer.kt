package com.cptodix.rimalux.desktop.render

import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import com.cptodix.rimalux.oled.wallpaper.engine.ArcSectorPrimitive
import com.cptodix.rimalux.oled.wallpaper.engine.ColorFillMode
import com.cptodix.rimalux.oled.wallpaper.engine.ColorHarmonyEngine
import com.cptodix.rimalux.oled.wallpaper.engine.DeepBlackRatioEngine
import com.cptodix.rimalux.oled.wallpaper.engine.PolygonPrimitive
import com.cptodix.rimalux.oled.wallpaper.engine.PolylineRibbonPrimitive
import com.cptodix.rimalux.oled.wallpaper.engine.SynthesizedWallpaper
import com.cptodix.rimalux.oled.wallpaper.engine.VectorPrimitive
import com.cptodix.rimalux.oled.wallpaper.engine.VectorScene
import java.nio.file.Files
import java.nio.file.Path as NioPath
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Color4f
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.FilterTileMode
import org.jetbrains.skia.Gradient
import org.jetbrains.skia.Image
import org.jetbrains.skia.Paint
import org.jetbrains.skia.PaintMode
import org.jetbrains.skia.PaintStrokeCap
import org.jetbrains.skia.PaintStrokeJoin
import org.jetbrains.skia.PathBuilder
import org.jetbrains.skia.PathFillMode
import org.jetbrains.skia.Rect
import org.jetbrains.skia.Shader
import org.jetbrains.skia.Surface

/**
 * Skia rasterizer for the pure [VectorScene] engine output.
 *
 * This mirrors the Android `WallpaperRenderer` and the cross-platform rendering specification:
 * opaque black clear, aspect-correct viewport transform, non-zero polygon winding, even-odd
 * annulus, round polyline strokes, and optional black micro-seams.
 */
object SkiaWallpaperRenderer {

    data class Rendered(
        val image: Image,
        val measuredBlackPercent: Int,
        val widthPx: Int,
        val heightPx: Int
    )

    fun render(
        synthesized: SynthesizedWallpaper,
        widthPx: Int,
        heightPx: Int
    ): Image = render(
        scene = synthesized.scene,
        paletteColors = synthesized.paletteColors,
        pitchBlackRatio = synthesized.pitchBlackRatio,
        colorFillMode = synthesized.colorFillMode,
        widthPx = widthPx,
        heightPx = heightPx
    )

    fun render(
        scene: VectorScene,
        paletteColors: List<Long>,
        pitchBlackRatio: Float,
        colorFillMode: ColorFillMode,
        widthPx: Int,
        heightPx: Int
    ): Image {
        val (safeW, safeH) = DeepBlackRatioEngine.clampDimensionsToMaxAxis(widthPx, heightPx)
        val surface = Surface.makeRasterN32Premul(safeW, safeH)
        try {
            val canvas = surface.canvas
            drawScene(
                canvas = canvas,
                scene = scene,
                paletteColors = paletteColors,
                pitchBlackRatio = pitchBlackRatio,
                colorFillMode = colorFillMode,
                widthPx = safeW,
                heightPx = safeH
            )
            return surface.makeImageSnapshot()
        } finally {
            runCatching { surface.close() }
        }
    }

    fun renderAndMeasure(
        synthesized: SynthesizedWallpaper,
        widthPx: Int,
        heightPx: Int
    ): Rendered {
        val image = render(synthesized, widthPx, heightPx)
        return Rendered(
            image = image,
            measuredBlackPercent = measureTrueBlackPercentage(image),
            widthPx = image.width,
            heightPx = image.height
        )
    }

    fun measureTrueBlackPercentage(image: Image): Int {
        val imageBitmap = image.toComposeImageBitmap()
        val pixels = imageBitmap.toPixelMap()
        return DeepBlackRatioEngine.measureTrueBlackPercentage(
            widthPx = image.width,
            heightPx = image.height
        ) { x, y ->
            pixels[x, y].toArgb()
        }
    }

    fun writePng(image: Image, path: NioPath) {
        val data = image.encodeToData(EncodedImageFormat.PNG)
            ?: error("Skia could not encode PNG")
        path.parent?.let { Files.createDirectories(it) }
        Files.write(path, data.bytes)
    }

    fun renderPreview(
        synthesized: SynthesizedWallpaper,
        maxLongEdgePx: Int = 1920
    ): Rendered {
        val maxEdge = maxLongEdgePx.coerceIn(320, 2560)
        val aspect = synthesized.aspectRatio
        val width: Int
        val height: Int
        if (aspect <= 1f) {
            width = maxEdge
            height = (maxEdge * aspect).roundToInt().coerceAtLeast(180)
        } else {
            width = (maxEdge / aspect).roundToInt().coerceAtLeast(180)
            height = maxEdge
        }
        return renderAndMeasure(synthesized, width, height)
    }

    private fun drawScene(
        canvas: Canvas,
        scene: VectorScene,
        paletteColors: List<Long>,
        pitchBlackRatio: Float,
        colorFillMode: ColorFillMode,
        widthPx: Int,
        heightPx: Int
    ) {
        val clampedBlack = DeepBlackRatioEngine.clampPitchBlackRatio(pitchBlackRatio)
        val isGradient = colorFillMode == ColorFillMode.GRADIENT
        canvas.clear(0xFF000000.toInt())

        val transform = DeepBlackRatioEngine.computeViewportTransform(
            canvasWidth = widthPx.toFloat(),
            canvasHeight = heightPx.toFloat(),
            sceneAspectRatio = scene.aspectRatio
        )
        val scale = transform.scale
        val offsetX = transform.offsetX
        val offsetY = transform.offsetY
        val drawMicroSeams = transform.drawMicroSeams

        val fillPaint = Paint().apply {
            isAntiAlias = true
            mode = PaintMode.FILL
            isDither = true
        }
        val strokePaint = Paint().apply {
            isAntiAlias = true
            mode = PaintMode.STROKE
            strokeCap = PaintStrokeCap.ROUND
            strokeJoin = PaintStrokeJoin.ROUND
            isDither = true
        }
        val seamPaint = Paint().apply {
            isAntiAlias = true
            mode = PaintMode.STROKE
            strokeJoin = PaintStrokeJoin.ROUND
            color = 0xFF000000.toInt()
        }

        val pathBuilder = PathBuilder()
        for (primitive in scene.primitives) {
            if (!DeepBlackRatioEngine.isPrimitiveVisible(primitive, clampedBlack)) continue

            val solidArgb = ColorHarmonyEngine.resolveArgbColor(
                paletteColors = paletteColors,
                colorIndex = primitive.colorIndex,
                shadeFactor = primitive.shadeFactor
            ).toInt()

            when (primitive) {
                is PolygonPrimitive -> {
                    val vertices = primitive.vertices
                    if (vertices.size < 3) continue
                    pathBuilder.reset()
                    pathBuilder.setFillType(PathFillMode.WINDING)
                    var minX = Float.POSITIVE_INFINITY
                    var minY = Float.POSITIVE_INFINITY
                    var maxX = Float.NEGATIVE_INFINITY
                    var maxY = Float.NEGATIVE_INFINITY

                    val x0 = offsetX + vertices[0].x * scale
                    val y0 = offsetY + vertices[0].y * scale
                    pathBuilder.moveTo(x0, y0)
                    minX = min(minX, x0)
                    minY = min(minY, y0)
                    maxX = max(maxX, x0)
                    maxY = max(maxY, y0)

                    for (i in 1 until vertices.size) {
                        val px = offsetX + vertices[i].x * scale
                        val py = offsetY + vertices[i].y * scale
                        pathBuilder.lineTo(px, py)
                        minX = min(minX, px)
                        minY = min(minY, py)
                        maxX = max(maxX, px)
                        maxY = max(maxY, py)
                    }
                    pathBuilder.closePath()
                    val path = pathBuilder.detach()

                    configurePaint(
                        paint = fillPaint,
                        isGradient = isGradient,
                        solidArgb = solidArgb,
                        paletteColors = paletteColors,
                        primitive = primitive,
                        minX = minX,
                        minY = minY,
                        maxX = maxX,
                        maxY = maxY
                    )
                    canvas.drawPath(path, fillPaint)

                    if (drawMicroSeams && primitive.strokeSeamNorm > 0f) {
                        seamPaint.strokeWidth = (primitive.strokeSeamNorm * scale).coerceIn(0.5f, 1.7f)
                        canvas.drawPath(path, seamPaint)
                    }
                }

                is ArcSectorPrimitive -> {
                    val cx = offsetX + primitive.center.x * scale
                    val cy = offsetY + primitive.center.y * scale
                    val outR = primitive.outerRadius * scale
                    val inR = primitive.innerRadius * scale
                    if (outR <= 0.5f) continue

                    val outerRect = Rect.makeLTRB(cx - outR, cy - outR, cx + outR, cy + outR)
                    configurePaint(
                        paint = fillPaint,
                        isGradient = isGradient,
                        solidArgb = solidArgb,
                        paletteColors = paletteColors,
                        primitive = primitive,
                        minX = cx - outR,
                        minY = cy - outR,
                        maxX = cx + outR,
                        maxY = cy + outR
                    )

                    when {
                        inR <= 0.5f && primitive.sweepAngleDeg >= 359.5f -> {
                            canvas.drawCircle(cx, cy, outR, fillPaint)
                        }

                        inR <= 0.5f -> {
                            pathBuilder.reset()
                            pathBuilder.setFillType(PathFillMode.WINDING)
                            pathBuilder.moveTo(cx, cy)
                            pathBuilder.arcTo(outerRect, primitive.startAngleDeg, primitive.sweepAngleDeg, false)
                            pathBuilder.closePath()
                            canvas.drawPath(pathBuilder.detach(), fillPaint)
                        }

                        primitive.sweepAngleDeg >= 359.5f -> {
                            val innerRect = Rect.makeLTRB(cx - inR, cy - inR, cx + inR, cy + inR)
                            pathBuilder.reset()
                            pathBuilder.setFillType(PathFillMode.EVEN_ODD)
                            pathBuilder.addOval(outerRect)
                            pathBuilder.addOval(innerRect)
                            canvas.drawPath(pathBuilder.detach(), fillPaint)
                        }

                        else -> {
                            val innerRect = Rect.makeLTRB(cx - inR, cy - inR, cx + inR, cy + inR)
                            pathBuilder.reset()
                            pathBuilder.setFillType(PathFillMode.WINDING)
                            val startRad = (primitive.startAngleDeg * PI / 180.0).toFloat()
                            pathBuilder.moveTo(cx + inR * cos(startRad), cy + inR * sin(startRad))
                            pathBuilder.arcTo(outerRect, primitive.startAngleDeg, primitive.sweepAngleDeg, false)
                            pathBuilder.arcTo(
                                innerRect,
                                primitive.startAngleDeg + primitive.sweepAngleDeg,
                                -primitive.sweepAngleDeg,
                                false
                            )
                            pathBuilder.closePath()
                            canvas.drawPath(pathBuilder.detach(), fillPaint)
                        }
                    }
                }

                is PolylineRibbonPrimitive -> {
                    val points = primitive.points
                    if (points.size < 2) continue
                    pathBuilder.reset()
                    var minX = Float.POSITIVE_INFINITY
                    var minY = Float.POSITIVE_INFINITY
                    var maxX = Float.NEGATIVE_INFINITY
                    var maxY = Float.NEGATIVE_INFINITY

                    val x0 = offsetX + points[0].x * scale
                    val y0 = offsetY + points[0].y * scale
                    pathBuilder.moveTo(x0, y0)
                    minX = min(minX, x0)
                    minY = min(minY, y0)
                    maxX = max(maxX, x0)
                    maxY = max(maxY, y0)

                    for (i in 1 until points.size) {
                        val px = offsetX + points[i].x * scale
                        val py = offsetY + points[i].y * scale
                        pathBuilder.lineTo(px, py)
                        minX = min(minX, px)
                        minY = min(minY, py)
                        maxX = max(maxX, px)
                        maxY = max(maxY, py)
                    }
                    if (primitive.closed) pathBuilder.closePath()

                    strokePaint.strokeWidth = (primitive.strokeWidthNorm * scale).coerceAtLeast(1.3f)
                    configurePaint(
                        paint = strokePaint,
                        isGradient = isGradient,
                        solidArgb = solidArgb,
                        paletteColors = paletteColors,
                        primitive = primitive,
                        minX = minX,
                        minY = minY,
                        maxX = maxX,
                        maxY = maxY
                    )
                    canvas.drawPath(pathBuilder.detach(), strokePaint)
                }
            }
        }
    }

    private fun configurePaint(
        paint: Paint,
        isGradient: Boolean,
        solidArgb: Int,
        paletteColors: List<Long>,
        primitive: VectorPrimitive,
        minX: Float,
        minY: Float,
        maxX: Float,
        maxY: Float
    ) {
        if (!isGradient) {
            paint.shader = null
            paint.color = solidArgb
            return
        }
        val segment = ColorHarmonyEngine.computeLinearGradient(
            paletteColors = paletteColors,
            primitive = primitive,
            minX = minX,
            minY = minY,
            maxX = maxX,
            maxY = maxY
        )
        paint.color = 0xFFFFFFFF.toInt()
        val gradient = Gradient(
            Gradient.Colors(
                arrayOf(
                    Color4f(segment.startColorArgb.toInt()),
                    Color4f(segment.endColorArgb.toInt())
                ),
                floatArrayOf(0f, 1f),
                FilterTileMode.CLAMP
            )
        )
        paint.shader = Shader.makeLinearGradient(
            segment.startX,
            segment.startY,
            segment.endX,
            segment.endY,
            gradient
        )
    }
}
