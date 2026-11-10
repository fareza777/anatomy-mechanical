package studio.anatomy.app.render

import studio.anatomy.app.core.Mat4
import studio.anatomy.app.core.V3
import studio.anatomy.app.core.cameraBasis
import studio.anatomy.app.core.fitViewProjection
import studio.anatomy.app.model.CompiledScene
import studio.anatomy.app.model.Hotspot

data class ProjectedHotspot(
    val hotspot: Hotspot,
    val number: Int,
    val x: Float,
    val y: Float,
)

/**
 * Projects annotation anchors without drawing. It now uses the same [fitViewProjection]
 * as [SceneRenderer] instead of duplicating the framing maths, so pins cannot drift
 * away from the geometry when the camera model changes.
 */
object HotspotProjector {

    fun project(scene: CompiledScene, request: RenderRequest): List<ProjectedHotspot> {
        val framing = sceneFraming(scene, request)
        val basis = cameraBasis(request.pose)
        val view = fitViewProjection(
            basis = basis,
            target = framing.target,
            bounds = framing.fitBounds,
            width = request.width,
            height = request.height,
            safeTop = request.safeTop,
            safeBottom = request.safeBottom,
            safeSide = request.safeSide,
            safeRight = request.safeRight,
            zoom = request.pose.zoom,
        )

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

        val out = FloatArray(2)
        return scene.spec.hotspots.mapIndexedNotNull { index, hotspot ->
            val partIndex = scene.hotspotPartIndex[index]
            if (partIndex < 0) return@mapIndexedNotNull null
            val world = worlds[partIndex] ?: return@mapIndexedNotNull null
            view.project(world.transform(hotspot.anchor), out, 0)
            ProjectedHotspot(hotspot, index + 1, out[0], out[1])
        }
    }
}
