package studio.anatomy.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import studio.anatomy.app.core.Aabb
import studio.anatomy.app.core.CameraPose
import studio.anatomy.app.core.V3
import studio.anatomy.app.core.cameraBasis
import studio.anatomy.app.core.clipPolygonToDepth
import studio.anatomy.app.core.fitViewProjection

class ProjectionTest {

    private val basis = cameraBasis(CameraPose(yaw = 34f, pitch = 26f, zoom = 1f))
    private val bounds = Aabb(V3(-0.5f, -0.5f, -0.5f), V3(0.5f, 0.5f, 0.5f))

    private fun projection(zoom: Float = 1f) = fitViewProjection(
        basis = basis,
        target = V3.ZERO,
        bounds = bounds,
        width = 1000,
        height = 1400,
        safeTop = 60f,
        safeBottom = 120f,
        safeSide = 40f,
        safeRight = 90f,
        zoom = zoom,
    )

    @Test
    fun fittedBoundsStayInsideTheSafeArea() {
        val projection = projection()
        val out = FloatArray(2)
        bounds.corners().forEach { corner ->
            projection.project(corner, out, 0)
            assertTrue("x=${out[0]}", out[0] in 39f..(1000f - 89f))
            assertTrue("y=${out[1]}", out[1] in 59f..(1400f - 119f))
        }
    }

    @Test
    fun nearerGeometryProjectsLarger() {
        val projection = projection()
        val far = FloatArray(2)
        val near = FloatArray(2)
        // Same lateral offset, different depth toward the camera.
        val farPoint = basis.eye * (-0.4f) + basis.right * 0.4f
        val nearPoint = basis.eye * 0.4f + basis.right * 0.4f
        projection.project(farPoint, far, 0)
        projection.project(nearPoint, near, 0)
        val farOffset = kotlin.math.abs(far[0] - projection.centerX)
        val nearOffset = kotlin.math.abs(near[0] - projection.centerX)
        assertTrue("near=$nearOffset far=$farOffset", nearOffset > farOffset)
    }

    @Test
    fun zoomShortensTheFraming() {
        val wide = projection(zoom = 1f)
        val tight = projection(zoom = 1.4f)
        assertTrue(tight.focal > wide.focal)
        assertTrue(tight.pixelsPerUnit > wide.pixelsPerUnit)
    }

    @Test
    fun clippingKeepsOnlyTheFarSide() {
        // Unit quad on the xy plane, depths are the x coordinates.
        val points = floatArrayOf(
            0f, 0f, 0f,
            1f, 0f, 0f,
            1f, 1f, 0f,
            0f, 1f, 0f,
        )
        val depths = floatArrayOf(0f, 1f, 1f, 0f)
        val out = FloatArray(6 * 3)
        val count = clipPolygonToDepth(points, depths, 4, 0.5f, out)
        assertEquals(4, count)
        for (i in 0 until count) {
            assertTrue(out[i * 3] <= 0.501f)
        }
    }

    @Test
    fun clippingEverythingReturnsNothing() {
        val points = floatArrayOf(0f, 0f, 0f, 1f, 0f, 0f, 1f, 1f, 0f)
        val depths = floatArrayOf(2f, 2f, 2f)
        val out = FloatArray(3 * 2 * 3)
        assertEquals(0, clipPolygonToDepth(points, depths, 3, 0.5f, out))
    }

    @Test
    fun clippingIsStableWhenThePlaneTouchesAVertex() {
        val points = floatArrayOf(0f, 0f, 0f, 1f, 0f, 0f, 1f, 1f, 0f)
        val depths = floatArrayOf(0.5f, 1f, 1f)
        val out = FloatArray(3 * 2 * 3)
        val count = clipPolygonToDepth(points, depths, 3, 0.5f, out)
        assertTrue(count in 1..6)
    }
}
