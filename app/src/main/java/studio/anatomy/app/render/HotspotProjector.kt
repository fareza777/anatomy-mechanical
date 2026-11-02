package studio.anatomy.app.render

import studio.anatomy.app.core.Aabb
import studio.anatomy.app.core.Mat4
import studio.anatomy.app.core.V3
import studio.anatomy.app.core.cameraBasis
import studio.anatomy.app.model.CompiledScene
import studio.anatomy.app.model.Hotspot
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class ProjectedHotspot(
    val hotspot: Hotspot,
    val number: Int,
    val x: Float,
    val y: Float,
)

/**
 * Projects annotation anchors without drawing. Mirrors SceneRenderer's fit and
 * world composition so pins sit exactly on the rendered geometry.
 */
object HotspotProjector {

    fun project(scene: CompiledScene, request: RenderRequest): List<ProjectedHotspot> {
        val bounds = scene.bounds(request.explode)
        val visual = expand(bounds, 1.14f)
        val target = visual.center + scene.spec.focusOffset
        val basis = cameraBasis(request.pose)

        val side = request.safeSide
        val top = request.safeTop
        val bottom = request.safeBottom
        val availableWidth = (request.width - side * 2f - 16f).coerceAtLeast(40f)
        val availableHeight = (request.height - top - bottom - 8f).coerceAtLeast(40f)
        val centerX = request.width * 0.5f
        val centerY = top + availableHeight * 0.5f

        var maxX = 0.001f
        var maxY = 0.001f
        visual.corners().forEach { corner ->
            val rx = corner.x - target.x
            val ry = corner.y - target.y
            val rz = corner.z - target.z
            maxX = max(maxX, abs(rx * basis.right.x + ry * basis.right.y + rz * basis.right.z))
            maxY = max(maxY, abs(rx * basis.up.x + ry * basis.up.y + rz * basis.up.z))
        }
        val scale = (min(availableWidth / (maxX * 2f), availableHeight / (maxY * 2f)) * 0.94f * request.pose.zoom)
            .coerceIn(1f, 20000f)

        val worlds = arrayOfNulls<Mat4>(scene.parts.size)
        scene.order.forEach { index ->
            val part = scene.parts[index]
            var local = Mat4.translation(part.origin).multiply(part.rotation)
            part.animate?.let { animator -> local = local.multiply(animator(request.anim)) }
            local = local.multiply(Mat4.scale(part.scale))
            val parent = scene.parentIndex[index]
            val base = if (parent >= 0) (worlds[parent] ?: Mat4.identity()).multiply(local) else local
            val amount = request.explode[part.assembly] ?: 0f
            worlds[index] = if (amount > 0.0001f) {
                Mat4.translation(part.explode * amount).multiply(base)
            } else {
                base
            }
        }

        return scene.spec.hotspots.mapIndexedNotNull { index, hotspot ->
            val partIndex = scene.hotspotPartIndex[index]
            if (partIndex < 0) return@mapIndexedNotNull null
            val world = worlds[partIndex] ?: return@mapIndexedNotNull null
            val point = world.transform(hotspot.anchor)
            val rx = point.x - target.x
            val ry = point.y - target.y
            val rz = point.z - target.z
            val sx = centerX + (rx * basis.right.x + ry * basis.right.y + rz * basis.right.z) * scale
            val sy = centerY - (rx * basis.up.x + ry * basis.up.y + rz * basis.up.z) * scale
            ProjectedHotspot(hotspot, index + 1, sx, sy)
        }
    }

    private fun expand(bounds: Aabb, factor: Float): Aabb {
        val center = bounds.center
        val half = bounds.size * (factor * 0.5f)
        return Aabb(center - half, center + half)
    }

    @Suppress("unused")
    private fun V3.dot(other: V3): Float = x * other.x + y * other.y + z * other.z
}
