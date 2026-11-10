package studio.anatomy.app.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Shader
import studio.anatomy.app.core.Aabb
import studio.anatomy.app.core.CameraBasis
import studio.anatomy.app.core.CameraPose
import studio.anatomy.app.core.Mat4
import studio.anatomy.app.core.V2
import studio.anatomy.app.core.V3
import studio.anatomy.app.core.cameraBasis
import studio.anatomy.app.model.AnimContext
import studio.anatomy.app.model.CompiledScene
import studio.anatomy.app.model.Hotspot
import studio.anatomy.app.model.Part
import studio.anatomy.app.model.PlumePalette
import studio.anatomy.app.model.SceneEffect
import java.util.Collections
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

enum class RenderQuality { INTERACTIVE, FULL }

data class RenderRequest(
    val width: Int,
    val height: Int,
    val pose: CameraPose,
    val anim: AnimContext,
    val explode: Map<String, Float> = emptyMap(),
    val selectedSystem: String? = null,
    val focusPartId: String? = null,
    val xray: Boolean = false,
    val showGrid: Boolean = true,
    val quality: RenderQuality = RenderQuality.FULL,
    val safeTop: Float = 0f,
    val safeBottom: Float = 0f,
    val safeSide: Float = 0f,
    val safeRight: Float = -1f,
    val density: Float = 2.6f,
    val cacheLayers: Boolean = true,
    val detail: Boolean = true,
)

data class HotspotPin(
    val hotspot: Hotspot,
    val number: Int,
    val x: Float,
    val y: Float,
)

data class RenderReport(
    val pins: List<HotspotPin>,
    val faceCount: Int,
    val renderMillis: Long,
    val scale: Float,
    val centerX: Float,
    val centerY: Float,
    val bounds: android.graphics.RectF,
    val batches: Int = 0,
    val projectMillis: Long = 0,
    val paintMillis: Long = 0,
    val chromeMillis: Long = 0,
    val buildMillis: Long = 0,
    val sortMillis: Long = 0,
    val patterns: Int = 0,
)

private class FaceSlot {
    var screen = FloatArray(64)
        private set
    var count = 0
    var depth = 0f
    var bucket = 0
    var fill = 0
    var stroke = 0
    var strokeWidth = 0f
    var drawStroke = false
    var minX = 0f
    var maxX = 0f
    var minY = 0f
    var maxY = 0f
    var area = 0f
    var pattern = SurfacePattern.NONE
    var patternStrength = 0f
    var patternSeed = 0f
    var drawPattern = false

    fun ensureCapacity(vertices: Int) {
        val needed = vertices * 2
        if (screen.size >= needed) return
        var size = screen.size
        while (size < needed) size *= 2
        val grown = FloatArray(size)
        System.arraycopy(screen, 0, grown, 0, screen.size)
        screen = grown
    }
}

private const val HAZE_COLOR = 0xFF0A171D.toInt()

class SceneRenderer {

    private val slots = ArrayList<FaceSlot>()
    private val drawList = ArrayList<FaceSlot>()
    private val depthComparator = Comparator<FaceSlot> { a, b -> a.depth.compareTo(b.depth) }
    private val batchComparator = Comparator<FaceSlot> { a, b ->
        var result = a.bucket.compareTo(b.bucket)
        if (result != 0) return@Comparator result
        result = a.fill.compareTo(b.fill)
        if (result != 0) return@Comparator result
        result = a.stroke.compareTo(b.stroke)
        if (result != 0) return@Comparator result
        if (a.drawStroke != b.drawStroke) return@Comparator if (a.drawStroke) -1 else 1
        a.depth.compareTo(b.depth)
    }
    private var vertexPositions = FloatArray(1 shl 15)
    private var vertexColors = IntArray(1 shl 14)
    private var outlinePositions = FloatArray(1 shl 15)
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val vignettePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val grainPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val addXfermode = PorterDuffXfermode(PorterDuff.Mode.ADD)
    private val vertexPaint = Paint().apply {
        isDither = true
        color = 0xFFFFFFFF.toInt()
    }
    private val path = Path()
    private val batchPath = Path()
    private var grainShader: Shader? = null
    private var backdropBitmap: Bitmap? = null
    private var backdropKey: String = ""
    private var chromeBitmap: Bitmap? = null
    private var chromeKey: String = ""
    private var lastChromeRequest: String = ""
    private var haloShader: Shader? = null
    private var overlayBitmap: Bitmap? = null
    private var overlayKey: String = ""
    private var paintedPatterns = 0
    private val castShadow = CastShadow()
    private val floorMatrix = Matrix()
    private val floorValues = FloatArray(9)
    private val shadowDrawPaint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val poolPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var poolShader: Shader? = null
    private var poolAccent = 0
    private var glowSprite: Bitmap? = null
    private val glowSpritePaint = Paint(Paint.FILTER_BITMAP_FLAG).apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD) }
    private val glowRect = android.graphics.RectF()
    private var glowBoxes = FloatArray(4 * 16)
    private var glowColors = IntArray(16)
    private var glowCount = 0
    private val hazeByBucket = FloatArray(16)
    private val worlds = ArrayList<Mat4?>()
    private val renderWorlds = ArrayList<Mat4?>()
    private val screenPoints = FloatArray(512)

    private var scale = 1f
    private var centerX = 0f
    private var centerY = 0f
    private var target = V3.ZERO
    private var basis: CameraBasis = cameraBasis(CameraPose())
    private var request: RenderRequest? = null
    private var scene: CompiledScene? = null

    fun draw(canvas: Canvas, scene: CompiledScene, request: RenderRequest): RenderReport {
        val started = System.nanoTime()
        var projectedAt = started
        var paintedAt = started
        this.scene = scene
        this.request = request
        val width = request.width.coerceAtLeast(2)
        val height = request.height.coerceAtLeast(2)
        val accent = scene.spec.accent
        val bounds = scene.bounds(request.explode)
        val visualBounds = expand(bounds, 1.14f)
        val framing = sceneFraming(scene, request)
        target = framing.target
        basis = cameraBasis(request.pose)
        computeFit(request, framing.fitBounds, width, height)
        computeWorlds(scene, request)

        drawChrome(canvas, scene, request, bounds, accent, width, height)
        val chromeAt = System.nanoTime()

        drawList.clear()
        glowCount = 0
        val groundY = bounds.min.y
        val groundScale = 1f / (bounds.size.y * 0.30f).coerceAtLeast(0.05f)
        // Dense scenes need the aggressive threshold to hold frame time; lighter models can
        // keep their thin shell strips, which read as missing panels if dropped.
        // Only speck-sized facets are dropped. Larger cut-offs punched visible holes in
        // thin shells (the fan cowl lost whole strips while it spun).
        val interactiveMinArea = 6f
        var slotIndex = 0
        var minDepth = Float.MAX_VALUE
        var maxDepth = -Float.MAX_VALUE
        scene.parts.forEachIndexed { index, part ->
            val world = renderWorlds[index] ?: return@forEachIndexed
            val mode = faceMode(part, request)
            val includeBackfaces = part.doubleSided || request.xray
            val glows = part.material.emissive && mode == FaceMode.NORMAL
            // Only scaled parts need their normals renormalised; checking one matrix row
            // per part keeps the square root out of the per-facet loop for everything else.
            val rowSq = world.m[0] * world.m[0] + world.m[1] * world.m[1] + world.m[2] * world.m[2]
            val colSq = world.m[0] * world.m[0] + world.m[4] * world.m[4] + world.m[8] * world.m[8]
            val scaled = abs(rowSq - 1f) > 0.004f || abs(colSq - 1f) > 0.004f || part.scale != V3.ONE
            var partMinX = Float.MAX_VALUE
            var partMaxX = -Float.MAX_VALUE
            var partMinY = Float.MAX_VALUE
            var partMaxY = -Float.MAX_VALUE
            part.mesh.faces.forEach { face ->
                var nx = transformNormalX(world, face.normal)
                var ny = transformNormalY(world, face.normal)
                var nz = transformNormalZ(world, face.normal)
                if (scaled) {
                    // Scaled parts carry scaled normals; lighting needs them at unit length.
                    val normalLength = sqrt(nx * nx + ny * ny + nz * nz)
                    if (normalLength > 1e-6f) {
                        nx /= normalLength
                        ny /= normalLength
                        nz /= normalLength
                    }
                }
                val dot = nx * basis.eye.x + ny * basis.eye.y + nz * basis.eye.z
                val backface = dot <= 0f
                if (backface && !includeBackfaces) return@forEach
                val slot = slotAt(slotIndex)
                slotIndex++
                slot.ensureCapacity(face.indices.size)
                var depth = 0f
                var minX = Float.MAX_VALUE
                var maxX = -Float.MAX_VALUE
                var minY = Float.MAX_VALUE
                var maxY = -Float.MAX_VALUE
                val m = world.m
                var screenIndex = 0
                var worldY = 0f
                for (vertexIndex in face.indices) {
                    val vertex = part.mesh.vertices[vertexIndex]
                    val wx = m[0] * vertex.x + m[1] * vertex.y + m[2] * vertex.z + m[3]
                    val wy = m[4] * vertex.x + m[5] * vertex.y + m[6] * vertex.z + m[7]
                    val wz = m[8] * vertex.x + m[9] * vertex.y + m[10] * vertex.z + m[11]
                    val rx = wx - target.x
                    val ry = wy - target.y
                    val rz = wz - target.z
                    val sx = centerX + (rx * basis.right.x + ry * basis.right.y + rz * basis.right.z) * scale
                    val sy = centerY - (rx * basis.up.x + ry * basis.up.y + rz * basis.up.z) * scale
                    val d = rx * basis.eye.x + ry * basis.eye.y + rz * basis.eye.z
                    depth += d
                    worldY += wy
                    if (sx < minX) minX = sx
                    if (sx > maxX) maxX = sx
                    if (sy < minY) minY = sy
                    if (sy > maxY) maxY = sy
                    slot.screen[screenIndex * 2] = sx
                    slot.screen[screenIndex * 2 + 1] = sy
                    screenIndex++
                }
                if (screenIndex < 3) return@forEach
                val area = (maxX - minX) * (maxY - minY)
                val minArea = if (request.quality == RenderQuality.INTERACTIVE) interactiveMinArea else 2.2f
                if (area < minArea) return@forEach
                depth /= screenIndex
                if (maxX < 0f || minX > width.toFloat() || maxY < 0f || minY > height.toFloat()) return@forEach
                val firstVertex = part.mesh.vertices[face.indices[0]]
                val seedRaw = firstVertex.x * 73.1f + firstVertex.y * 19.7f + firstVertex.z * 41.3f + index * 0.37f
                val faceSeed = (sin(seedRaw * 1.7f) * 0.5f + 0.5f).coerceIn(0f, 1f)
                val partHeight = part.mesh.bounds.size.y.coerceAtLeast(0.001f)
                val heightRatio = ((firstVertex.y - part.mesh.bounds.min.y) / partHeight).coerceIn(0f, 1f)
                // Contact darkening: facets close to the stage floor sit in the model's own
                // shade, which grounds the machine instead of leaving it evenly lit.
                val lift = ((worldY / screenIndex - groundY) * groundScale).coerceIn(0f, 1f)
                val occlusion = (0.88f + 0.12f * heightRatio) * (0.80f + 0.20f * lift * (2f - lift))
                val fill = Shading.shade(
                    part.material,
                    if (backface) -nx else nx,
                    if (backface) -ny else ny,
                    if (backface) -nz else nz,
                    mode,
                    request.anim.ambientTime,
                    faceSeed,
                    occlusion,
                    basis.eye.x,
                    basis.eye.y,
                    basis.eye.z,
                )
                slot.count = screenIndex
                slot.depth = depth
                if (depth < minDepth) minDepth = depth
                if (depth > maxDepth) maxDepth = depth
                slot.fill = fill
                slot.stroke = part.material.edgeColorFor(mode, fill)
                slot.strokeWidth = max(1.05f, request.density * 0.5f)
                slot.drawStroke = request.quality == RenderQuality.FULL && mode != FaceMode.GHOST &&
                    area > 9f
                slot.minX = minX
                slot.maxX = maxX
                slot.minY = minY
                slot.maxY = maxY
                slot.area = area
                slot.pattern = part.material.pattern
                slot.patternStrength = part.material.patternStrength
                slot.patternSeed = faceSeed
                slot.drawPattern = request.quality == RenderQuality.FULL &&
                    mode != FaceMode.GHOST && mode != FaceMode.DIMMED &&
                    slot.pattern != SurfacePattern.NONE && slot.patternStrength > 0.01f &&
                    area >= 300f
                drawList.add(slot)
                if (glows) {
                    if (minX < partMinX) partMinX = minX
                    if (maxX > partMaxX) partMaxX = maxX
                    if (minY < partMinY) partMinY = minY
                    if (maxY > partMaxY) partMaxY = maxY
                }
            }
            if (glows && partMaxX > partMinX) {
                addGlowBox(partMinX, partMinY, partMaxX, partMaxY, part.material.baseColor)
            }
        }
        val buildAt = System.nanoTime()
        projectedAt = System.nanoTime()
        // The batched vertex path was tested for moving frames and produced
        // missing n-gon surfaces on this device, so every frame uses the same
        // correct path painter. Performance is controlled with resolution and
        // culling instead of changing the geometry pipeline.
        val batchCount = paintDetailed(canvas, request)
        paintedAt = System.nanoTime()

        drawEmissiveGlow(canvas, width, height)
        drawEffects(canvas, scene, request)
        if (request.cacheLayers) {
            drawVignette(canvas, width, height, request.quality)
        } else {
            paintVignette(canvas, width, height, request.quality)
        }

        val pins = projectPins(scene, request)
        val projected = projectBounds(visualBounds)
        val elapsed = (System.nanoTime() - started) / 1_000_000f
        return RenderReport(
            pins = pins,
            faceCount = drawList.size,
            renderMillis = elapsed.toLong(),
            scale = scale,
            centerX = centerX,
            centerY = centerY,
            bounds = projected,
            batches = batchCount,
            projectMillis = (projectedAt - started) / 1_000_000,
            paintMillis = (paintedAt - projectedAt) / 1_000_000,
            chromeMillis = (chromeAt - started) / 1_000_000,
            buildMillis = (buildAt - chromeAt) / 1_000_000,
            sortMillis = (projectedAt - buildAt) / 1_000_000,
            patterns = paintedPatterns,
        )
    }

    private fun computeFit(request: RenderRequest, bounds: Aabb, width: Int, height: Int) {
        val side = request.safeSide
        val rightSide = if (request.safeRight >= 0f) request.safeRight else side
        val top = request.safeTop
        val bottom = request.safeBottom
        val availableWidth = (width - side - rightSide - 16f).coerceAtLeast(40f)
        val availableHeight = (height - top - bottom - 8f).coerceAtLeast(40f)
        centerX = side + availableWidth * 0.5f
        centerY = top + availableHeight * 0.5f
        var maxX = 0.001f
        var maxY = 0.001f
        bounds.corners().forEach { corner ->
            val rx = corner.x - target.x
            val ry = corner.y - target.y
            val rz = corner.z - target.z
            val px = abs(rx * basis.right.x + ry * basis.right.y + rz * basis.right.z)
            val py = abs(rx * basis.up.x + ry * basis.up.y + rz * basis.up.z)
            maxX = max(maxX, px)
            maxY = max(maxY, py)
        }
        val fitScale = min(availableWidth / (maxX * 2f), availableHeight / (maxY * 2f)) * 0.94f * request.pose.zoom
        scale = fitScale.coerceIn(1f, 20000f)
    }

    private fun computeWorlds(scene: CompiledScene, request: RenderRequest) {
        val count = scene.parts.size
        while (worlds.size < count) worlds.add(null)
        while (renderWorlds.size < count) renderWorlds.add(null)
        scene.order.forEach { index ->
            val part = scene.parts[index]
            var local = Mat4.translation(part.origin).multiply(part.rotation)
            part.animate?.let { animator ->
                local = local.multiply(animator(request.anim))
            }
            local = local.multiply(Mat4.scale(part.scale))
            val parent = scene.parentIndex[index]
            val base = if (parent >= 0) (worlds[parent] ?: Mat4.identity()).multiply(local) else local
            worlds[index] = base
            val amount = request.explode[part.assembly] ?: 0f
            renderWorlds[index] = if (amount > 0.0001f) {
                Mat4.translation(part.explode * amount).multiply(base)
            } else {
                base
            }
        }
    }

    private fun faceMode(part: Part, request: RenderRequest): FaceMode {
        val selected = request.selectedSystem
        return when {
            selected == null && request.xray -> FaceMode.GHOST
            selected == null -> FaceMode.NORMAL
            part.system == selected -> FaceMode.SELECTED
            request.xray -> FaceMode.GHOST
            else -> FaceMode.DIMMED
        }
    }

    private fun drawChrome(
        canvas: Canvas,
        scene: CompiledScene,
        request: RenderRequest,
        bounds: Aabb,
        accent: Int,
        width: Int,
        height: Int,
    ) {
        if (!request.cacheLayers) {
            paintBackdrop(canvas, width, height, accent, request.quality)
            drawLightPool(canvas, bounds, accent)
            if (request.showGrid) drawFloor(canvas, scene, request, bounds, accent)
            drawContactShadow(canvas, bounds)
            drawCastShadow(canvas, scene, request, bounds)
            return
        }
        val key = "$width:$height:${request.pose.yaw}:${request.pose.pitch}:${request.pose.zoom}" +
            ":${request.explode.hashCode()}:${request.showGrid}:$accent:${request.quality}" +
            ":${request.focusPartId ?: "-"}" +
            ":${bounds.min.x},${bounds.min.y},${bounds.min.z},${bounds.max.x},${bounds.max.y},${bounds.max.z}"
        var bitmap = chromeBitmap
        // While the view is moving every frame has a new key, so caching would only add a
        // full-screen copy. Paint straight to the canvas and cache once the view settles.
        val moving = key != lastChromeRequest
        lastChromeRequest = key
        if (moving && key != chromeKey) {
            drawBackdrop(canvas, width, height, accent, request.quality)
            drawLightPool(canvas, bounds, accent)
            if (request.showGrid) drawFloor(canvas, scene, request, bounds, accent)
            drawContactShadow(canvas, bounds)
            drawCastShadow(canvas, scene, request, bounds)
            return
        }
        if (key != chromeKey || bitmap == null) {
            if (bitmap == null || bitmap.width != width || bitmap.height != height) {
                bitmap?.recycle()
                bitmap = Bitmap.createBitmap(width.coerceAtLeast(1), height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
                chromeBitmap = bitmap
            }
            val chromeCanvas = Canvas(bitmap)
            // The gradient backdrop does not depend on the view, so it is painted once and
            // copied. Only the floor, light pool and shadows are redrawn while orbiting.
            drawBackdrop(chromeCanvas, width, height, accent, request.quality)
            drawLightPool(chromeCanvas, bounds, accent)
            if (request.showGrid) drawFloor(chromeCanvas, scene, request, bounds, accent)
            drawContactShadow(chromeCanvas, bounds)
            drawCastShadow(chromeCanvas, scene, request, bounds)
            chromeKey = key
        }
        canvas.drawBitmap(bitmap, 0f, 0f, null)
    }

    private fun drawBackdrop(canvas: Canvas, width: Int, height: Int, accent: Int, quality: RenderQuality) {
        val key = "$width:$height:$accent:${centerY.toInt()}:$quality"
        if (key != backdropKey || backdropBitmap == null) {
            if (backdropBitmap == null || backdropBitmap!!.width != width || backdropBitmap!!.height != height) {
                backdropBitmap?.recycle()
                backdropBitmap = Bitmap.createBitmap(width.coerceAtLeast(1), height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
            }
            paintBackdrop(Canvas(backdropBitmap!!), width, height, accent, quality)
            backdropKey = key
        }
        backdropBitmap?.let { canvas.drawBitmap(it, 0f, 0f, null) }
    }

    /**
     * Maps the stage floor (world X/Z at height [y], in world units) onto the screen. The
     * camera is orthographic, so the floor plane is an exact affine image and anything
     * painted through this matrix lies flat on the stage from every angle.
     */
    private fun setFloorMatrix(y: Float) {
        projectTo(V3(0f, y, 0f), screenPoints, 0)
        projectTo(V3(1f, y, 0f), screenPoints, 2)
        projectTo(V3(0f, y, 1f), screenPoints, 4)
        floorValues[0] = screenPoints[2] - screenPoints[0]
        floorValues[1] = screenPoints[4] - screenPoints[0]
        floorValues[2] = screenPoints[0]
        floorValues[3] = screenPoints[3] - screenPoints[1]
        floorValues[4] = screenPoints[5] - screenPoints[1]
        floorValues[5] = screenPoints[1]
        floorValues[6] = 0f
        floorValues[7] = 0f
        floorValues[8] = 1f
        floorMatrix.setValues(floorValues)
    }

    /** A soft pool of key light on the stage floor under the model. */
    private fun drawLightPool(canvas: Canvas, bounds: Aabb, accent: Int) {
        if (poolShader == null || poolAccent != accent) {
            val warm = ColorMath.mix(accent, 0xFFE6F4FA.toInt(), 0.55f)
            poolShader = RadialGradient(
                0f, 0f, 1f,
                intArrayOf(
                    ColorMath.withAlpha(warm, 40),
                    ColorMath.withAlpha(warm, 19),
                    ColorMath.withAlpha(accent, 7),
                    0x00000000,
                ),
                floatArrayOf(0f, 0.38f, 0.74f, 1f),
                Shader.TileMode.CLAMP,
            )
            poolAccent = accent
        }
        val radius = max(bounds.size.x, bounds.size.z) * 0.98f + 0.5f
        setFloorMatrix(bounds.min.y - 0.012f)
        canvas.save()
        canvas.concat(floorMatrix)
        canvas.translate(bounds.center.x, bounds.center.z)
        canvas.scale(radius, radius)
        poolPaint.shader = poolShader
        canvas.drawCircle(0f, 0f, 1f, poolPaint)
        poolPaint.shader = null
        canvas.restore()
    }

    /**
     * The model's real silhouette, cast on the floor by the key light. The shadow lives in
     * floor space, so it is rasterised once per pose of the mechanism and then only
     * re-projected: orbiting the camera costs a single bitmap draw.
     */
    private fun drawCastShadow(canvas: Canvas, scene: CompiledScene, request: RenderRequest, bounds: Aabb) {
        if (request.xray && request.selectedSystem == null) return
        val live = request.quality == RenderQuality.INTERACTIVE
        castShadow.update(scene, renderWorlds, bounds, request.explode.hashCode(), allowStale = live)
        val bitmap = castShadow.bitmap ?: return
        setFloorMatrix(castShadow.floorY)
        canvas.save()
        canvas.concat(floorMatrix)
        canvas.translate(castShadow.originX, castShadow.originZ)
        canvas.scale(castShadow.worldPerPixel, castShadow.worldPerPixel)
        shadowDrawPaint.color = 0xFF02060A.toInt()
        shadowDrawPaint.alpha = 168
        canvas.drawBitmap(bitmap, 0f, 0f, shadowDrawPaint)
        canvas.restore()
    }

    private fun addGlowBox(minX: Float, minY: Float, maxX: Float, maxY: Float, color: Int) {
        if (glowCount >= 48) return
        if (glowCount >= glowColors.size) {
            glowColors = glowColors.copyOf(glowColors.size * 2)
            glowBoxes = glowBoxes.copyOf(glowBoxes.size * 2)
        }
        val offset = glowCount * 4
        glowBoxes[offset] = minX
        glowBoxes[offset + 1] = minY
        glowBoxes[offset + 2] = maxX
        glowBoxes[offset + 3] = maxY
        glowColors[glowCount] = color
        glowCount++
    }

    /**
     * Light spill around emitters: one pre-blurred sprite per glowing part, added over the
     * finished facets. No blur pass and no allocation per frame.
     */
    private fun drawEmissiveGlow(canvas: Canvas, width: Int, height: Int) {
        if (glowCount == 0) return
        val sprite = glowSprite ?: createGlowSprite().also { glowSprite = it }
        val limit = width.toFloat() * height * 0.22f
        for (i in 0 until glowCount) {
            val offset = i * 4
            val w = glowBoxes[offset + 2] - glowBoxes[offset]
            val h = glowBoxes[offset + 3] - glowBoxes[offset + 1]
            if (w * h > limit) continue
            val cx = (glowBoxes[offset] + glowBoxes[offset + 2]) * 0.5f
            val cy = (glowBoxes[offset + 1] + glowBoxes[offset + 3]) * 0.5f
            val pad = max(6f, min(w, h) * 0.9f)
            val halfW = w * 0.62f + pad
            val halfH = h * 0.62f + pad
            glowRect.set(cx - halfW, cy - halfH, cx + halfW, cy + halfH)
            glowSpritePaint.color = glowColors[i]
            glowSpritePaint.alpha = 60
            canvas.drawBitmap(sprite, null, glowRect, glowSpritePaint)
        }
    }

    private fun createGlowSprite(): Bitmap {
        val size = 64
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ALPHA_8)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.shader = RadialGradient(
            size * 0.5f, size * 0.5f, size * 0.5f,
            intArrayOf(0xFF000000.toInt(), 0x66000000, 0x1C000000, 0x00000000),
            floatArrayOf(0f, 0.34f, 0.68f, 1f),
            Shader.TileMode.CLAMP,
        )
        Canvas(bitmap).drawCircle(size * 0.5f, size * 0.5f, size * 0.5f, paint)
        return bitmap
    }

    private fun paintBackdrop(canvas: Canvas, width: Int, height: Int, accent: Int, quality: RenderQuality) {
        backgroundPaint.shader = LinearGradient(
            0f, 0f, 0f, height.toFloat(),
            intArrayOf(0xFF12242B.toInt(), 0xFF0A141A.toInt(), 0xFF04090D.toInt()),
            floatArrayOf(0f, 0.52f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)
        glowPaint.shader = RadialGradient(
            width * 0.5f,
            centerY,
            max(width, height) * 0.62f,
            intArrayOf(ColorMath.withAlpha(accent, 32), ColorMath.withAlpha(accent, 9), 0x00000000),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP,
        )
        glowPaint.xfermode = null
        canvas.drawCircle(width * 0.5f, centerY, max(width, height) * 0.62f, glowPaint)
        glowPaint.shader = RadialGradient(
            width * 0.16f,
            height * 0.82f,
            max(width, height) * 0.54f,
            intArrayOf(ColorMath.withAlpha(accent, 12), ColorMath.withAlpha(accent, 4), 0x00000000),
            floatArrayOf(0f, 0.56f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(width * 0.16f, height * 0.82f, max(width, height) * 0.54f, glowPaint)
        glowPaint.shader = null
        if (quality == RenderQuality.FULL) {
            linePaint.color = ColorMath.withAlpha(0xFFFFFFFF.toInt(), 9)
            linePaint.strokeWidth = 1f
            val step = width / 8f
            var x = 0f
            while (x <= width) {
                canvas.drawLine(x, 0f, x, height * 0.14f, linePaint)
                canvas.drawLine(x, height * 0.86f, x, height.toFloat(), linePaint)
                x += step
            }
        }
        backgroundPaint.shader = null
    }

    private fun drawFloor(canvas: Canvas, scene: CompiledScene, request: RenderRequest, bounds: Aabb, accent: Int) {
        val floorY = bounds.min.y - max(0.12f, bounds.size.y * 0.075f)
        val size = max(bounds.size.x, bounds.size.z)
        val radius = size * 0.78f + 0.4f
        val center = V3(bounds.center.x, floorY, bounds.center.z)
        val gridColor = ColorMath.withAlpha(accent, 12)
        val majorColor = ColorMath.withAlpha(accent, 22)

        val extent = max(bounds.size.x, bounds.size.z) * 1.5f + 1.5f
        val spacing = niceSpacing(extent / 13f)
        var x = -extent
        while (x <= extent) {
            projectTo(center + V3(x, 0f, -extent), screenPoints, 0)
            projectTo(center + V3(x, 0f, extent), screenPoints, 2)
            val major = abs(x / spacing - Math.round(x / spacing).toFloat()) < 0.01f
            linePaint.color = if (major) majorColor else gridColor
            linePaint.strokeWidth = if (major) 1.1f else 0.7f
            canvas.drawLine(screenPoints[0], screenPoints[1], screenPoints[2], screenPoints[3], linePaint)
            x += spacing
        }
        var z = -extent
        while (z <= extent) {
            projectTo(center + V3(-extent, 0f, z), screenPoints, 0)
            projectTo(center + V3(extent, 0f, z), screenPoints, 2)
            val major = abs(z / spacing - Math.round(z / spacing).toFloat()) < 0.01f
            linePaint.color = if (major) majorColor else gridColor
            linePaint.strokeWidth = if (major) 1.1f else 0.7f
            canvas.drawLine(screenPoints[0], screenPoints[1], screenPoints[2], screenPoints[3], linePaint)
            z += spacing
        }

        val ringColor = ColorMath.withAlpha(accent, 52)
        val ringPointCount = 72
        path.rewind()
        for (i in 0 until ringPointCount) {
            val angle = 2.0 * Math.PI * i / ringPointCount
            projectTo(center + V3((radius * cos(angle)).toFloat(), 0f, (radius * sin(angle)).toFloat()), screenPoints, 0)
            if (i == 0) path.moveTo(screenPoints[0], screenPoints[1]) else path.lineTo(screenPoints[0], screenPoints[1])
        }
        path.close()
        linePaint.color = ringColor
        linePaint.strokeWidth = 1.2f
        canvas.drawPath(path, linePaint)

        for (tick in 0 until 36) {
            val angle = 2.0 * Math.PI * tick / 36
            val inner = if (tick % 3 == 0) radius - 0.16f else radius - 0.08f
            projectTo(center + V3((inner * cos(angle)).toFloat(), 0f, (inner * sin(angle)).toFloat()), screenPoints, 0)
            projectTo(center + V3((radius * cos(angle)).toFloat(), 0f, (radius * sin(angle)).toFloat()), screenPoints, 2)
            linePaint.color = if (tick % 3 == 0) ColorMath.withAlpha(accent, 96) else ColorMath.withAlpha(accent, 38)
            linePaint.strokeWidth = if (tick % 3 == 0) 1.2f else 0.8f
            canvas.drawLine(screenPoints[0], screenPoints[1], screenPoints[2], screenPoints[3], linePaint)
        }

        val arcPointCount = 22
        for (arc in 0 until 2) {
            path.rewind()
            val start = (if (arc == 0) 16 else 196) * Math.PI / 180.0
            val sweep = 24 * Math.PI / 180.0
            for (i in 0..arcPointCount) {
                val angle = start + sweep * i / arcPointCount
                projectTo(center + V3((radius * 1.025f * cos(angle)).toFloat(), 0f, (radius * 1.025f * sin(angle)).toFloat()), screenPoints, 0)
                if (i == 0) path.moveTo(screenPoints[0], screenPoints[1]) else path.lineTo(screenPoints[0], screenPoints[1])
            }
            linePaint.color = ColorMath.withAlpha(accent, 150)
            linePaint.strokeWidth = 2.1f
            canvas.drawPath(path, linePaint)
        }
    }

    private fun niceSpacing(raw: Float): Float {
        if (raw <= 0.25f) return 0.25f
        val power = Math.pow(10.0, Math.floor(kotlin.math.log10(raw.toDouble()))).toFloat()
        val normalized = raw / power
        val nice = when {
            normalized <= 1f -> 1f
            normalized <= 2f -> 2f
            normalized <= 5f -> 5f
            else -> 10f
        }
        return nice * power
    }

    /**
     * Soft ambient shade under the whole machine. The cast shadow gives the shape; this
     * only seats the model on the stage. One cached unit gradient, laid on the floor plane.
     */
    private fun drawContactShadow(canvas: Canvas, bounds: Aabb) {
        val shader = haloShader ?: RadialGradient(
            0f, 0f, 1f,
            intArrayOf(0x8C000000.toInt(), 0x52000000, 0x14000000, 0x00000000),
            floatArrayOf(0f, 0.46f, 0.80f, 1f),
            Shader.TileMode.CLAMP,
        ).also { haloShader = it }
        val radius = max(bounds.size.x, bounds.size.z) * 0.74f + 0.15f
        setFloorMatrix(bounds.min.y - 0.011f)
        canvas.save()
        canvas.concat(floorMatrix)
        canvas.translate(bounds.center.x, bounds.center.z)
        canvas.scale(radius, radius)
        shadowPaint.shader = shader
        canvas.drawCircle(0f, 0f, 1f, shadowPaint)
        shadowPaint.shader = null
        canvas.restore()
    }

    private fun drawEffects(canvas: Canvas, scene: CompiledScene, request: RenderRequest) {
        val effects = scene.spec.effects(request.anim)
        effects.forEach { effect ->
            when (effect) {
                is SceneEffect.Glow -> drawGlow(canvas, effect)
                is SceneEffect.Plume -> drawPlume(canvas, effect, request)
                is SceneEffect.Flow -> drawFlow(canvas, effect, request)
                is SceneEffect.Ring -> drawRing(canvas, effect)
                is SceneEffect.Beam -> drawBeam(canvas, effect)
                is SceneEffect.Spark -> drawSpark(canvas, effect)
            }
        }
    }

    private fun drawGlow(canvas: Canvas, glow: SceneEffect.Glow) {
        if (glow.intensity <= 0.01f) return
        projectTo(glow.position, screenPoints, 0)
        val x = screenPoints[0]
        val y = screenPoints[1]
        val radius = max(8f, glow.radius * scale)
        val alpha = (255 * glow.intensity.coerceIn(0f, 1f)).toInt()
        glowPaint.xfermode = addXfermode
        glowPaint.shader = RadialGradient(
            x, y, radius,
            intArrayOf(ColorMath.withAlpha(glow.color, (alpha * 0.62f).toInt()), ColorMath.withAlpha(glow.color, (alpha * 0.18f).toInt()), 0x00000000),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(x, y, radius, glowPaint)
        glowPaint.shader = null
        glowPaint.xfermode = null
    }

    private fun drawPlume(canvas: Canvas, plume: SceneEffect.Plume, request: RenderRequest) {
        if (plume.intensity <= 0.01f) return
        val direction = plume.direction.normalized()
        val origin = plume.origin
        val tip = origin + direction * plume.length
        projectTo(origin, screenPoints, 0)
        projectTo(tip, screenPoints, 2)
        val ox = screenPoints[0]
        val oy = screenPoints[1]
        val tx = screenPoints[2]
        val ty = screenPoints[3]
        val lengthPx = sqrt((tx - ox) * (tx - ox) + (ty - oy) * (ty - oy))
        if (lengthPx < 4f) return
        val angle = Math.toDegrees(atan2((ty - oy).toDouble(), (tx - ox).toDouble())).toFloat()
        val flicker = 0.88f + 0.12f * sin(request.anim.ambientTime * 17.3f) * sin(request.anim.ambientTime * 6.1f + 1.7f)
        val intensity = plume.intensity.coerceIn(0f, 1f) * flicker
        val radius = max(4f, plume.radius * scale)
        val outer = when (plume.palette) {
            PlumePalette.PLASMA -> 0xFF4F8DFF.toInt()
            PlumePalette.AMBER -> 0xFFFF8B3D.toInt()
            PlumePalette.STEAM -> 0xFFB7D8D6.toInt()
            PlumePalette.DIESEL -> 0xFF8C6B4A.toInt()
        }
        val middle = when (plume.palette) {
            PlumePalette.PLASMA -> 0xFF65F4E4.toInt()
            PlumePalette.AMBER -> 0xFFFFC46B.toInt()
            PlumePalette.STEAM -> 0xFFE8F3EF.toInt()
            PlumePalette.DIESEL -> 0xFFD2A06A.toInt()
        }
        val core = when (plume.palette) {
            PlumePalette.PLASMA -> 0xFFE4FFF9.toInt()
            PlumePalette.AMBER -> 0xFFFFF1CE.toInt()
            PlumePalette.STEAM -> 0xFFFFFFFF.toInt()
            PlumePalette.DIESEL -> 0xFFF7E2BD.toInt()
        }
        val alpha = (intensity * 255f).toInt().coerceIn(0, 255)
        val length = lengthPx * (0.86f + 0.14f * intensity)
        canvas.save()
        canvas.translate(ox, oy)
        canvas.rotate(angle)
        glowPaint.xfermode = addXfermode

        val haloRadius = radius * 3.2f
        glowPaint.shader = RadialGradient(
            0f, 0f, haloRadius,
            intArrayOf(
                ColorMath.withAlpha(outer, (alpha * 0.38f).toInt()),
                ColorMath.withAlpha(middle, (alpha * 0.12f).toInt()),
                0x00000000,
            ),
            floatArrayOf(0f, 0.42f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(0f, 0f, haloRadius, glowPaint)
        glowPaint.shader = null

        fun plumePath(layerLength: Float, layerWidth: Float, wobble: Float, phase: Float) {
            path.rewind()
            val samples = 22
            for (i in 0..samples) {
                val t = i.toFloat() / samples
                val envelope = Math.pow((1f - t).toDouble(), 0.58).toFloat()
                val waver = sin(t * 9.5f + request.anim.ambientTime * 8.3f + phase) * wobble * t
                val x = layerLength * t
                val w = layerWidth * envelope * (0.82f + 0.18f * sin(t * 5.2f + phase)) + waver
                if (i == 0) path.moveTo(0f, -w) else path.lineTo(x, -max(0f, w))
            }
            path.lineTo(layerLength, 0f)
            for (i in samples downTo 0) {
                val t = i.toFloat() / samples
                val envelope = Math.pow((1f - t).toDouble(), 0.58).toFloat()
                val waver = sin(t * 9.5f + request.anim.ambientTime * 8.3f + phase + 1.4f) * wobble * t
                val x = layerLength * t
                val w = layerWidth * envelope * (0.82f + 0.18f * sin(t * 5.2f + phase + 0.7f)) + waver
                path.lineTo(x, max(0f, w))
            }
            path.close()
        }

        fun drawLayer(
            lengthFactor: Float,
            widthFactor: Float,
            startColor: Int,
            endColor: Int,
            startAlpha: Int,
            middleAlpha: Int,
            wobble: Float,
            phase: Float,
        ) {
            plumePath(length * lengthFactor, radius * widthFactor, wobble, phase)
            glowPaint.shader = LinearGradient(
                0f, 0f, length * lengthFactor, 0f,
                intArrayOf(
                    ColorMath.withAlpha(startColor, (alpha * startAlpha / 255f).toInt()),
                    ColorMath.withAlpha(endColor, (alpha * middleAlpha / 255f).toInt()),
                    0x00000000,
                ),
                floatArrayOf(0f, 0.45f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawPath(path, glowPaint)
            glowPaint.shader = null
        }

        drawLayer(0.98f, 1.05f, outer, outer, 72, 40, radius * 0.08f, 0.2f)
        drawLayer(0.86f, 0.62f, middle, outer, 138, 58, radius * 0.055f, 1.1f)
        drawLayer(0.6f, 0.3f, core, middle, 232, 100, radius * 0.025f, 2.4f)

        if (plume.palette == PlumePalette.AMBER || plume.palette == PlumePalette.PLASMA) {
            for (shock in listOf(0.18f, 0.33f, 0.47f)) {
                val x = length * shock
                val r = radius * (0.22f - shock * 0.1f)
                glowPaint.color = ColorMath.withAlpha(core, (alpha * (0.42f - shock * 0.4f)).toInt())
                canvas.drawCircle(x, 0f, max(1.2f, r), glowPaint)
            }
            for (i in 0 until 4) {
                val t = 0.12f + i * 0.17f
                val x = length * t
                val y = sin(request.anim.ambientTime * 12f + i * 1.9f) * radius * (1f - t) * 0.5f
                glowPaint.color = ColorMath.withAlpha(core, (alpha * (0.25f - t * 0.2f)).toInt())
                canvas.drawCircle(x, y, max(0.8f, radius * 0.05f), glowPaint)
            }
        }
        glowPaint.xfermode = null
        canvas.restore()
    }

    private fun drawFlow(canvas: Canvas, flow: SceneEffect.Flow, request: RenderRequest) {
        if (flow.points.size < 2) return
        val segments = ArrayList<Float>()
        var total = 0f
        for (i in 0 until flow.points.size - 1) {
            val length = (flow.points[i + 1] - flow.points[i]).length()
            segments.add(length)
            total += length
        }
        if (total < 1e-4f) return
        glowPaint.xfermode = addXfermode
        for (i in 0 until flow.count) {
            var t = (flow.phase + request.anim.ambientTime * flow.speed + i.toFloat() / flow.count) % 1f
            t = (t + 1f) % 1f
            var distance = t * total
            var segmentIndex = 0
            while (segmentIndex < segments.size && distance > segments[segmentIndex]) {
                distance -= segments[segmentIndex]
                segmentIndex++
            }
            if (segmentIndex >= segments.size) continue
            val local = (distance / segments[segmentIndex]).coerceIn(0f, 1f)
            val point = flow.points[segmentIndex].lerp(flow.points[segmentIndex + 1], local)
            projectTo(point, screenPoints, 0)
            val x = screenPoints[0]
            val y = screenPoints[1]
            val fade = min(1f, min(t / 0.12f, (1f - t) / 0.12f))
            val radius = max(1.2f, flow.dotRadius * scale * (0.7f + 0.3f * sin(i * 2.3f)))
            glowPaint.color = ColorMath.withAlpha(flow.color, (200 * fade).toInt())
            canvas.drawCircle(x, y, radius, glowPaint)
            glowPaint.shader = RadialGradient(
                x, y, radius * 3.2f,
                intArrayOf(ColorMath.withAlpha(flow.color, (100 * fade).toInt()), ColorMath.withAlpha(flow.color, 0)),
                null,
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(x, y, radius * 3.2f, glowPaint)
            glowPaint.shader = null
        }
        glowPaint.xfermode = null
    }

    private fun drawRing(canvas: Canvas, ring: SceneEffect.Ring) {
        if (ring.intensity <= 0.01f) return
        val center = ring.center
        val radius = ring.radius * ring.phase
        path.rewind()
        val steps = 48
        for (i in 0..steps) {
            val angle = 2.0 * Math.PI * i / steps
            projectTo(center + V3((radius * cos(angle)).toFloat(), 0f, (radius * sin(angle)).toFloat()), screenPoints, 0)
            if (i == 0) path.moveTo(screenPoints[0], screenPoints[1]) else path.lineTo(screenPoints[0], screenPoints[1])
        }
        linePaint.color = ColorMath.withAlpha(ring.color, (180 * ring.intensity * (1f - ring.phase)).toInt())
        linePaint.strokeWidth = 1.6f
        glowPaint.xfermode = addXfermode
        canvas.drawPath(path, linePaint)
        glowPaint.xfermode = null
    }

    private fun drawBeam(canvas: Canvas, beam: SceneEffect.Beam) {
        projectTo(beam.start, screenPoints, 0)
        projectTo(beam.end, screenPoints, 2)
        linePaint.color = ColorMath.withAlpha(beam.color, (60 * beam.intensity).toInt())
        linePaint.strokeWidth = 8f
        glowPaint.xfermode = addXfermode
        canvas.drawLine(screenPoints[0], screenPoints[1], screenPoints[2], screenPoints[3], linePaint)
        linePaint.color = ColorMath.withAlpha(beam.color, (210 * beam.intensity).toInt())
        linePaint.strokeWidth = 1.5f
        canvas.drawLine(screenPoints[0], screenPoints[1], screenPoints[2], screenPoints[3], linePaint)
        glowPaint.xfermode = null
    }

    private fun drawSpark(canvas: Canvas, spark: SceneEffect.Spark) {
        projectTo(spark.position, screenPoints, 0)
        glowPaint.xfermode = addXfermode
        glowPaint.color = ColorMath.withAlpha(spark.color, (220 * spark.intensity).toInt())
        canvas.drawCircle(screenPoints[0], screenPoints[1], max(1.1f, spark.size * scale), glowPaint)
        glowPaint.xfermode = null
    }

    private fun drawVignette(canvas: Canvas, width: Int, height: Int, quality: RenderQuality) {
        val key = "v:$width:$height:$quality"
        if (key != overlayKey || overlayBitmap == null) {
            if (overlayBitmap == null || overlayBitmap!!.width != width || overlayBitmap!!.height != height) {
                overlayBitmap?.recycle()
                overlayBitmap = Bitmap.createBitmap(width.coerceAtLeast(1), height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
            }
            paintVignette(Canvas(overlayBitmap!!), width, height, quality)
            overlayKey = key
        }
        overlayBitmap?.let { canvas.drawBitmap(it, 0f, 0f, null) }
    }

    private fun paintVignette(canvas: Canvas, width: Int, height: Int, quality: RenderQuality) {
        vignettePaint.shader = RadialGradient(
            width * 0.5f,
            height * 0.46f,
            max(width, height) * 0.78f,
            intArrayOf(0x00000000, 0x08000000, 0x5E000000, 0xAC000000.toInt()),
            floatArrayOf(0f, 0.58f, 0.88f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), vignettePaint)
        vignettePaint.shader = null
        if (quality == RenderQuality.FULL) drawGrain(canvas, width, height)
    }

    private fun drawGrain(canvas: Canvas, width: Int, height: Int) {
        // Baked into the cached vignette layer, so this costs nothing per frame. The
        // alpha is low enough to read as a photographic finish rather than noise.
        if (grainShader == null) grainShader = createGrainShader()
        grainPaint.shader = grainShader
        grainPaint.alpha = 9
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), grainPaint)
        grainPaint.shader = null
        grainPaint.alpha = 255
    }

    @Suppress("unused")
    private fun createGrainShader(): Shader {
        val size = 128
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(size * size)
        val random = java.util.Random(91L)
        for (i in pixels.indices) {
            val value = random.nextInt(255)
            pixels[i] = (value shl 24) or (value shl 16) or (value shl 8) or value
        }
        bitmap.setPixels(pixels, 0, size, 0, 0, size, size)
        return android.graphics.BitmapShader(bitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
    }

    private fun projectPins(scene: CompiledScene, request: RenderRequest): List<HotspotPin> {
        val pins = ArrayList<HotspotPin>(scene.spec.hotspots.size)
        scene.spec.hotspots.forEachIndexed { index, hotspot ->
            val partIndex = scene.hotspotPartIndex[index]
            if (partIndex < 0) return@forEachIndexed
            val world = renderWorlds[partIndex] ?: return@forEachIndexed
            val worldPoint = world.transform(hotspot.anchor)
            projectTo(worldPoint, screenPoints, 0)
            pins.add(HotspotPin(hotspot, index + 1, screenPoints[0], screenPoints[1]))
        }
        return pins
    }

    private fun projectBounds(bounds: Aabb): android.graphics.RectF {
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        bounds.corners().forEach { corner ->
            projectTo(corner, screenPoints, 0)
            minX = min(minX, screenPoints[0])
            minY = min(minY, screenPoints[1])
            maxX = max(maxX, screenPoints[0])
            maxY = max(maxY, screenPoints[1])
        }
        return android.graphics.RectF(minX, minY, maxX, maxY)
    }

    private fun projectTo(point: V3, output: FloatArray, offset: Int) {
        val rx = point.x - target.x
        val ry = point.y - target.y
        val rz = point.z - target.z
        output[offset] = centerX + (rx * basis.right.x + ry * basis.right.y + rz * basis.right.z) * scale
        output[offset + 1] = centerY - (rx * basis.up.x + ry * basis.up.y + rz * basis.up.z) * scale
    }

    private fun transformNormalX(world: Mat4, normal: V3): Float =
        world.m[0] * normal.x + world.m[1] * normal.y + world.m[2] * normal.z
    private fun transformNormalY(world: Mat4, normal: V3): Float =
        world.m[4] * normal.x + world.m[5] * normal.y + world.m[6] * normal.z
    private fun transformNormalZ(world: Mat4, normal: V3): Float =
        world.m[8] * normal.x + world.m[9] * normal.y + world.m[10] * normal.z

    /** Crisp anti-aliased facets for a still scene: grouped fills, then inked outlines. */
    private fun paintDetailed(canvas: Canvas, request: RenderRequest): Int {
        // Anti-aliasing is expensive when the scene is rebuilt every frame. Keep it
        // for still inspection and let the lower-resolution live buffer hide the
        // slightly harder facet edges during motion.
        fillPaint.isAntiAlias = request.quality == RenderQuality.FULL
        var minDepth = Float.MAX_VALUE
        var maxDepth = -Float.MAX_VALUE
        for (slot in drawList) {
            if (slot.depth < minDepth) minDepth = slot.depth
            if (slot.depth > maxDepth) maxDepth = slot.depth
        }
        // Wider depth slices let more same-coloured facets share one path. Facets in the
        // same slice still sort by depth, so the only reordering is between identical
        // shades, which is invisible.
        val depthBuckets = if (request.quality == RenderQuality.INTERACTIVE) 6 else 14
        val depthSpan = (maxDepth - minDepth).coerceAtLeast(0.001f)
        for (slot in drawList) {
            slot.bucket = (((slot.depth - minDepth) / depthSpan) * (depthBuckets - 1)).toInt()
                .coerceIn(0, depthBuckets - 1)
        }
        drawList.sortWith(batchComparator)
        // Aerial perspective: the far side of the machine sinks a little into the stage
        // colour. It is applied per depth slice, when a batch is painted, so it is free.
        for (bucket in 0 until depthBuckets) {
            hazeByBucket[bucket] = 0.24f * (1f - bucket / (depthBuckets - 1f))
        }

        var batchCount = 0
        var batchBucket = 0
        var batchFill = Int.MIN_VALUE
        var batchStroke = Int.MIN_VALUE
        var batchStrokeWidth = 0f
        var batchDrawsStroke = false
        var hasBatch = false
        batchPath.rewind()
        fun flushBatch() {
            if (!hasBatch) return
            batchCount++
            val haze = hazeByBucket[batchBucket]
            fillPaint.color = hazed(batchFill, haze)
            canvas.drawPath(batchPath, fillPaint)
            if (batchDrawsStroke) {
                strokePaint.color = hazed(batchStroke, haze)
                strokePaint.strokeWidth = batchStrokeWidth
                canvas.drawPath(batchPath, strokePaint)
            }
            batchPath.rewind()
            hasBatch = false
        }
        for (slot in drawList) {
            if (hasBatch && (
                    slot.bucket != batchBucket ||
                    slot.fill != batchFill ||
                    slot.stroke != batchStroke ||
                    slot.drawStroke != batchDrawsStroke ||
                    slot.strokeWidth != batchStrokeWidth
                )
            ) {
                flushBatch()
            }
            if (!hasBatch) {
                batchBucket = slot.bucket
                batchFill = slot.fill
                batchStroke = slot.stroke
                batchStrokeWidth = slot.strokeWidth
                batchDrawsStroke = slot.drawStroke
                hasBatch = true
            }
            for (i in 0 until slot.count) {
                val x = slot.screen[i * 2]
                val y = slot.screen[i * 2 + 1]
                if (i == 0) batchPath.moveTo(x, y) else batchPath.lineTo(x, y)
            }
            batchPath.close()
        }
        flushBatch()
        paintedPatterns = drawSurfacePatterns(canvas, request)
        return batchCount
    }

    private fun hazed(color: Int, amount: Float): Int {
        if (amount <= 0.004f) return color
        val alpha = (color ushr 24) and 0xFF
        return ColorMath.mix(color, ColorMath.withAlpha(HAZE_COLOR, alpha), amount)
    }

    /**
     * Texture-like marks without texture memory or per-face bitmap sampling.
     * Only a small budget of large, opaque faces is decorated, and only at FULL
     * quality. The pass is clipped to the face and reuses one Path/Paint pair.
     */
    private fun drawSurfacePatterns(canvas: Canvas, request: RenderRequest): Int {
        if (request.quality != RenderQuality.FULL || !request.detail) return 0
        var count = 0
        for (slot in drawList) {
            if (!slot.drawPattern) continue
            if (count >= 8) break
            val width = slot.maxX - slot.minX
            val height = slot.maxY - slot.minY
            if (width < 14f || height < 10f) continue
            path.rewind()
            for (i in 0 until slot.count) {
                val x = slot.screen[i * 2]
                val y = slot.screen[i * 2 + 1]
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            val alpha = (slot.patternStrength * 46f).toInt().coerceIn(10, 46)
            val lightSurface = ColorMath.luminance(slot.fill) > 0.40f
            linePaint.color = if (lightSurface) {
                ColorMath.withAlpha(0xFF071014.toInt(), alpha)
            } else {
                ColorMath.withAlpha(0xFFE4F4EE.toInt(), alpha)
            }
            linePaint.strokeWidth = if (slot.pattern == SurfacePattern.GRID) 0.85f else 0.65f
            canvas.save()
            canvas.clipPath(path)
            val left = slot.minX + 2f
            val top = slot.minY + 2f
            val right = slot.maxX - 2f
            val bottom = slot.maxY - 2f
            when (slot.pattern) {
                SurfacePattern.BRUSHED -> {
                    for (i in 0..6) {
                        val x = left + (right - left) * (i / 7f)
                        canvas.drawLine(x, bottom, x + height * 0.32f, top, linePaint)
                    }
                }
                SurfacePattern.PANEL -> {
                    val x = left + (right - left) * 0.36f
                    val y = top + (bottom - top) * 0.58f
                    canvas.drawLine(x, top, x, bottom, linePaint)
                    canvas.drawLine(left, y, right, y, linePaint)
                    canvas.drawCircle(x, y, 1.8f, fillPaint.apply { color = linePaint.color })
                }
                SurfacePattern.GRID -> {
                    for (i in 1..4) {
                        val x = left + (right - left) * (i / 5f)
                        val y = top + (bottom - top) * (i / 5f)
                        canvas.drawLine(x, top, x, bottom, linePaint)
                        canvas.drawLine(left, y, right, y, linePaint)
                    }
                }
                SurfacePattern.SPECKLE -> {
                    for (i in 0 until 14) {
                        val a = sin((slot.patternSeed * 31f + i * 12.9898f)) * 0.5f + 0.5f
                        val b = sin((slot.patternSeed * 57f + i * 78.233f)) * 0.5f + 0.5f
                        val x = left + (right - left) * a
                        val y = top + (bottom - top) * b
                        canvas.drawCircle(x, y, if (i % 3 == 0) 1.15f else 0.7f, fillPaint.apply { color = linePaint.color })
                    }
                }
                SurfacePattern.PATINA -> {
                    for (i in 0 until 9) {
                        val a = sin((slot.patternSeed * 47f + i * 9.73f)) * 0.5f + 0.5f
                        val b = sin((slot.patternSeed * 23f + i * 17.41f)) * 0.5f + 0.5f
                        canvas.drawCircle(left + (right - left) * a, top + (bottom - top) * b, 2.2f, fillPaint.apply { color = linePaint.color })
                    }
                }
                SurfacePattern.HAZARD -> {
                    for (i in 0..5) {
                        val x = left + (right - left) * (i / 6f)
                        canvas.drawLine(x, bottom, x + height * 0.55f, top, linePaint)
                    }
                }
                SurfacePattern.NONE -> Unit
            }
            canvas.restore()
            count++
        }
        fillPaint.color = 0xFFFFFFFF.toInt()
        return count
    }

    /** Cheap flat-shaded facets, used only while the camera or mechanism is moving. */
    private fun paintFast(canvas: Canvas, request: RenderRequest): Int {
        drawList.sortWith(depthComparator)
        var batchCount = 0
        var faceStart = 0
        while (faceStart < drawList.size) {
            val faceEnd = min(drawList.size, faceStart + 1600)
            var triangles = 0
            for (i in faceStart until faceEnd) {
                triangles += (drawList[i].count - 2).coerceAtLeast(0)
            }
            val vertices = triangles * 3
            // Fresh arrays per frame: HWUI keeps the vertex buffer referenced until the
            // frame is submitted, so reusing one array corrupts geometry frame to frame.
            val positions = FloatArray(vertices * 2)
            val colors = IntArray(vertices)
            var vertex = 0
            for (i in faceStart until faceEnd) {
                vertex = emitTriangles(drawList[i], drawList[i].fill, positions, colors, vertex)
            }
            if (vertex > 0) {
                canvas.drawVertices(
                    Canvas.VertexMode.TRIANGLES,
                    vertex,
                    positions,
                    0,
                    null,
                    0,
                    colors,
                    0,
                    null,
                    0,
                    0,
                    vertexPaint,
                )
            }
            batchCount++
            faceStart = faceEnd
        }

        return batchCount
    }

    private fun emitTriangles(
        slot: FaceSlot,
        color: Int,
        positions: FloatArray,
        colors: IntArray,
        start: Int,
    ): Int {
        var vertex = start
        val count = slot.count
        val screen = slot.screen
        for (i in 1 until count - 1) {
            val base = vertex * 2
            positions[base] = screen[0]
            positions[base + 1] = screen[1]
            positions[base + 2] = screen[i * 2]
            positions[base + 3] = screen[i * 2 + 1]
            positions[base + 4] = screen[i * 2 + 2]
            positions[base + 5] = screen[i * 2 + 3]
            colors[vertex] = color
            colors[vertex + 1] = color
            colors[vertex + 2] = color
            vertex += 3
        }
        return vertex
    }

    private fun slotAt(index: Int): FaceSlot {
        while (slots.size <= index) slots.add(FaceSlot())
        return slots[index]
    }

    private fun expand(bounds: Aabb, factor: Float): Aabb {
        val center = bounds.center
        val half = bounds.size * (factor * 0.5f)
        return Aabb(center - half, center + half)
    }
}
