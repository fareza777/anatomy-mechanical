package studio.anatomy.app.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import studio.anatomy.app.core.Aabb
import studio.anatomy.app.core.Mat4
import studio.anatomy.app.model.CompiledScene
import java.nio.ByteBuffer
import kotlin.math.max

/**
 * The model's silhouette as cast on the stage floor by the key light.
 *
 * The shadow is rasterised in floor space (world X/Z), not screen space, into a small
 * alpha-only texture and blurred once. Because the camera is orthographic the renderer can
 * lay that texture on the floor with a single affine bitmap draw, so the shadow follows the
 * camera for free. It is rebuilt only when the scene or its exploded layout changes.
 */
internal class CastShadow {
    var bitmap: Bitmap? = null
        private set

    /** World X/Z of the texture's top-left texel, and the world size of one texel. */
    var originX = 0f
        private set
    var originZ = 0f
        private set
    var worldPerPixel = 1f
        private set
    var floorY = 0f
        private set

    private var builtScene: CompiledScene? = null
    private var builtKey = 0
    private var staleFrames = 0
    private val path = Path()
    private val paint = Paint().apply {
        style = Paint.Style.FILL
        color = 0xFF000000.toInt()
        isAntiAlias = false
    }
    private var pixels = ByteArray(SIZE * SIZE)
    private var scratch = ByteArray(SIZE * SIZE)

    fun update(
        scene: CompiledScene,
        worlds: List<Mat4?>,
        bounds: Aabb,
        layoutKey: Int,
        allowStale: Boolean,
    ) {
        val current = bitmap
        if (current != null && builtScene === scene && builtKey == layoutKey) return
        // While a slider is being dragged the layout changes every frame. The last shadow
        // is still correctly placed in floor space, so it is reused for a few frames
        // instead of re-rasterising the whole model each time.
        if (allowStale && current != null && builtScene === scene && staleFrames < STALE_LIMIT) {
            staleFrames++
            return
        }
        staleFrames = 0
        rebuild(scene, worlds, bounds)
        builtScene = scene
        builtKey = layoutKey
    }

    private fun rebuild(scene: CompiledScene, worlds: List<Mat4?>, bounds: Aabb) {
        val target = bitmap ?: Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ALPHA_8).also { bitmap = it }
        target.eraseColor(0)
        val canvas = Canvas(target)

        // The shadow light is the key light pulled towards vertical: the direction stays
        // readable but the shadow remains close to the machine instead of streaking away.
        val slopeX = -Shading.lightX / Shading.lightY * SHADOW_REACH
        val slopeZ = -Shading.lightZ / Shading.lightY * SHADOW_REACH
        val ground = bounds.min.y - 0.01f
        val height = bounds.size.y
        val reachX = slopeX * height
        val reachZ = slopeZ * height
        val minX = bounds.min.x + minOf(0f, reachX)
        val maxX = bounds.max.x + max(0f, reachX)
        val minZ = bounds.min.z + minOf(0f, reachZ)
        val maxZ = bounds.max.z + max(0f, reachZ)
        val span = max(maxX - minX, maxZ - minZ).coerceAtLeast(0.01f)
        val margin = span * 0.09f
        val texel = (span + margin * 2f) / SIZE
        val left = (minX + maxX) * 0.5f - texel * SIZE * 0.5f
        val top = (minZ + maxZ) * 0.5f - texel * SIZE * 0.5f
        val inverse = 1f / texel

        scene.parts.forEachIndexed { index, part ->
            val world = worlds.getOrNull(index) ?: return@forEachIndexed
            // Glass and emitters do not block the key light.
            if (part.material.emissive || part.material.alpha < 200) return@forEachIndexed
            val m = world.m
            val vertices = part.mesh.vertices
            path.rewind()
            for (face in part.mesh.faces) {
                var first = true
                for (vertexIndex in face.indices) {
                    val vertex = vertices[vertexIndex]
                    val wx = m[0] * vertex.x + m[1] * vertex.y + m[2] * vertex.z + m[3]
                    val wy = m[4] * vertex.x + m[5] * vertex.y + m[6] * vertex.z + m[7]
                    val wz = m[8] * vertex.x + m[9] * vertex.y + m[10] * vertex.z + m[11]
                    val lift = (wy - ground).coerceAtLeast(0f)
                    val px = (wx + slopeX * lift - left) * inverse
                    val py = (wz + slopeZ * lift - top) * inverse
                    if (first) {
                        path.moveTo(px, py)
                        first = false
                    } else {
                        path.lineTo(px, py)
                    }
                }
                path.close()
            }
            canvas.drawPath(path, paint)
        }
        path.rewind()
        soften(target)

        originX = left
        originZ = top
        worldPerPixel = texel
        floorY = ground
    }

    /** Three box-blur passes approximate a Gaussian penumbra. Runs once per rebuild. */
    private fun soften(target: Bitmap) {
        val buffer = ByteBuffer.wrap(pixels)
        target.copyPixelsToBuffer(buffer)
        repeat(3) {
            blurRows(pixels, scratch)
            blurColumns(scratch, pixels)
        }
        buffer.rewind()
        target.copyPixelsFromBuffer(buffer)
    }

    private fun blurRows(source: ByteArray, output: ByteArray) {
        val window = BLUR_RADIUS * 2 + 1
        for (y in 0 until SIZE) {
            val row = y * SIZE
            var sum = 0
            for (x in -BLUR_RADIUS..BLUR_RADIUS) {
                sum += source[row + x.coerceIn(0, SIZE - 1)].toInt() and 0xFF
            }
            for (x in 0 until SIZE) {
                output[row + x] = (sum / window).toByte()
                val leaving = (x - BLUR_RADIUS).coerceAtLeast(0)
                val entering = (x + BLUR_RADIUS + 1).coerceAtMost(SIZE - 1)
                sum += (source[row + entering].toInt() and 0xFF) - (source[row + leaving].toInt() and 0xFF)
            }
        }
    }

    private fun blurColumns(source: ByteArray, output: ByteArray) {
        val window = BLUR_RADIUS * 2 + 1
        for (x in 0 until SIZE) {
            var sum = 0
            for (y in -BLUR_RADIUS..BLUR_RADIUS) {
                sum += source[y.coerceIn(0, SIZE - 1) * SIZE + x].toInt() and 0xFF
            }
            for (y in 0 until SIZE) {
                output[y * SIZE + x] = (sum / window).toByte()
                val leaving = (y - BLUR_RADIUS).coerceAtLeast(0)
                val entering = (y + BLUR_RADIUS + 1).coerceAtMost(SIZE - 1)
                sum += (source[entering * SIZE + x].toInt() and 0xFF) - (source[leaving * SIZE + x].toInt() and 0xFF)
            }
        }
    }

    private companion object {
        const val SIZE = 256
        const val BLUR_RADIUS = 2
        const val SHADOW_REACH = 0.62f
        const val STALE_LIMIT = 5
    }
}
