package studio.anatomy.app.render

import studio.anatomy.app.core.Aabb
import studio.anatomy.app.core.V3
import studio.anatomy.app.model.CompiledScene

internal data class SceneFraming(
    val target: V3,
    val fitBounds: Aabb,
)

/**
 * Resolves the point the camera should look at and the bounds used to fit the view.
 *
 * When a part is focused the camera follows that part, but frames it with some
 * surrounding context. Exploded assemblies are included so a focused part does not
 * slide out of frame while its assembly separates. Both the renderer and the pin
 * projector use this function so annotations stay registered with the geometry.
 */
internal fun sceneFraming(scene: CompiledScene, request: RenderRequest): SceneFraming {
    val sceneBounds = scene.bounds(request.explode)
    val visualBounds = expand(sceneBounds, 1.14f)
    val defaultTarget = visualBounds.center + scene.spec.focusOffset
    val partId = request.focusPartId
    if (partId == null) return SceneFraming(defaultTarget, visualBounds)

    val index = scene.partIndex[partId] ?: return SceneFraming(defaultTarget, visualBounds)
    val part = scene.parts[index]
    val amount = request.explode[part.assembly] ?: 0f
    val bounds = scene.restBounds[index].translated(part.explode * amount)
    val center = bounds.center
    val partHalf = maxOf(bounds.size.x, bounds.size.y, bounds.size.z) * 0.5f
    val sceneHalf = maxOf(visualBounds.size.x, visualBounds.size.y) * 0.62f

    // A small part still gets a useful stage around it; a large assembly never
    // pushes the rest of the model completely out of the frame.
    val half = (partHalf * 1.25f)
        .coerceAtLeast(0.11f)
        .coerceAtMost(sceneHalf.coerceAtLeast(0.11f))
    val fitBounds = Aabb(
        center - V3(half, half, half),
        center + V3(half, half, half),
    )
    return SceneFraming(center, fitBounds)
}

private fun expand(bounds: Aabb, factor: Float): Aabb {
    val center = bounds.center
    val half = bounds.size * (factor * 0.5f)
    return Aabb(center - half, center + half)
}
