package studio.anatomy.app

import org.junit.Test
import studio.anatomy.app.core.CameraPose
import studio.anatomy.app.model.AnimContext
import studio.anatomy.app.model.buildScene
import studio.anatomy.app.objects.turbofan
import studio.anatomy.app.render.HotspotProjector
import studio.anatomy.app.render.RenderRequest

class PinReportTest {
    @Test
    fun reportDefaultPinLayout() {
        val spec = turbofan()
        val scene = buildScene(spec)
        val scale = 0.72f
        val request = RenderRequest(
            width = (720 * scale).toInt(),
            height = (1280 * scale).toInt(),
            pose = CameraPose(46f, 24f, 1.18f),
            anim = AnimContext(3f, 3f, 0.5f),
            safeTop = 240f,
            safeBottom = 340f,
            safeSide = 8f * 2f * scale,
            density = 1.44f,
        )
        val pins = HotspotProjector.project(scene, request)
        pins.forEach { pin ->
            println("PIN ${pin.number} ${pin.hotspot.id} screen=${(pin.x / scale).toInt()},${(pin.y / scale).toInt()}")
        }
    }
}
