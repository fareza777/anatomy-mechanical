package studio.anatomy.app.core

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Perspective camera shared by the scene renderer and the hotspot projector.
 *
 * The camera orbits [target] at [distance] world units. Geometry is projected with a
 * perspective divide, so parts closer to the viewer read larger than parts behind the
 * mechanism. [focal] is fitted so the supplied bounds stay inside the safe area.
 *
 * Before this existed the app used a pure orthographic scale, which flattened the
 * depth of every model: an exploded assembly looked like a flat diagram.
 */
data class ViewProjection(
    val basis: CameraBasis,
    val target: V3,
    val distance: Float,
    val focal: Float,
    val centerX: Float,
    val centerY: Float,
    val nearDepth: Float,
) {
    /** Screen pixels per world unit at the target plane. Used to size glows and shadows. */
    val pixelsPerUnit: Float get() = focal / distance

    /** Signed distance toward the camera. Larger values are nearer the viewer. */
    fun depth(point: V3): Float {
        val rx = point.x - target.x
        val ry = point.y - target.y
        val rz = point.z - target.z
        return rx * basis.eye.x + ry * basis.eye.y + rz * basis.eye.z
    }

    /** Distance along the view direction, always positive so the divide stays safe. */
    fun viewDepth(point: V3): Float = (distance - depth(point)).coerceAtLeast(nearDepth)

    fun project(point: V3, out: FloatArray, offset: Int) {
        val rx = point.x - target.x
        val ry = point.y - target.y
        val rz = point.z - target.z
        val viewDepth = (distance - (rx * basis.eye.x + ry * basis.eye.y + rz * basis.eye.z))
            .coerceAtLeast(nearDepth)
        val inverse = focal / viewDepth
        out[offset] = centerX + (rx * basis.right.x + ry * basis.right.y + rz * basis.right.z) * inverse
        out[offset + 1] = centerY - (rx * basis.up.x + ry * basis.up.y + rz * basis.up.z) * inverse
    }
}

/**
 * Fits a perspective projection around [bounds] inside the padded viewport.
 *
 * The lens is solved against the eight bounds corners instead of a single radius, so
 * the near corners cannot spill outside the stage once perspective is applied.
 */
fun fitViewProjection(
    basis: CameraBasis,
    target: V3,
    bounds: Aabb,
    width: Int,
    height: Int,
    safeTop: Float,
    safeBottom: Float,
    safeSide: Float,
    safeRight: Float,
    zoom: Float,
): ViewProjection {
    val side = safeSide
    val rightSide = if (safeRight >= 0f) safeRight else side
    val availableWidth = (width - side - rightSide - 16f).coerceAtLeast(40f)
    val availableHeight = (height - safeTop - safeBottom - 8f).coerceAtLeast(40f)
    val centerX = side + availableWidth * 0.5f
    val centerY = safeTop + availableHeight * 0.5f

    val radius = max(0.02f, bounds.radius)
    // A longer lens keeps products from looking like fish-eye shots while still giving
    // the near/far size difference that sells the depth.
    val distance = radius * CAMERA_DISTANCE_FACTOR
    val nearDepth = distance * NEAR_FRACTION

    var focal = Float.MAX_VALUE
    bounds.corners().forEach { corner ->
        val rx = corner.x - target.x
        val ry = corner.y - target.y
        val rz = corner.z - target.z
        val u = rx * basis.right.x + ry * basis.right.y + rz * basis.right.z
        val v = rx * basis.up.x + ry * basis.up.y + rz * basis.up.z
        val w = rx * basis.eye.x + ry * basis.eye.y + rz * basis.eye.z
        val viewDepth = (distance - w).coerceAtLeast(nearDepth)
        if (abs(u) > 1e-4f) focal = min(focal, availableWidth * 0.5f * viewDepth / abs(u))
        if (abs(v) > 1e-4f) focal = min(focal, availableHeight * 0.5f * viewDepth / abs(v))
    }
    if (focal == Float.MAX_VALUE) focal = max(availableWidth, availableHeight)
    focal = (focal * 0.94f * zoom).coerceIn(1f, 40_000f)

    return ViewProjection(
        basis = basis,
        target = target,
        distance = distance,
        focal = focal,
        centerX = centerX,
        centerY = centerY,
        nearDepth = nearDepth,
    )
}

/**
 * Sutherland-Hodgman clip of a convex polygon against the plane `depth <= plane`.
 *
 * [points] holds xyz triples, [depths] the per-vertex depth used for the test. The
 * result is written to [out] (also xyz triples, capacity `(count * 2 + 2) * 3`) and the
 * number of surviving vertices is returned. Used to slice the model for CUTAWAY.
 */
fun clipPolygonToDepth(
    points: FloatArray,
    depths: FloatArray,
    count: Int,
    plane: Float,
    out: FloatArray,
): Int {
    if (count < 3) return 0
    var written = 0
    for (i in 0 until count) {
        val j = if (i + 1 == count) 0 else i + 1
        val ai = i * 3
        val bi = j * 3
        val ad = depths[i]
        val bd = depths[j]
        val aInside = ad <= plane
        val bInside = bd <= plane
        if (aInside) {
            val oi = written * 3
            out[oi] = points[ai]
            out[oi + 1] = points[ai + 1]
            out[oi + 2] = points[ai + 2]
            written++
        }
        if (aInside != bInside) {
            val delta = bd - ad
            val t = if (abs(delta) < 1e-6f) 0.5f else (plane - ad) / delta
            val oi = written * 3
            out[oi] = points[ai] + (points[bi] - points[ai]) * t
            out[oi + 1] = points[ai + 1] + (points[bi + 1] - points[ai + 1]) * t
            out[oi + 2] = points[ai + 2] + (points[bi + 2] - points[ai + 2]) * t
            written++
        }
    }
    return written
}

/** Camera distance as a multiple of the fitted bounds radius. */
const val CAMERA_DISTANCE_FACTOR = 3.2f

/** Geometry nearer than this fraction of the camera distance is clamped, not inverted. */
const val NEAR_FRACTION = 0.12f
