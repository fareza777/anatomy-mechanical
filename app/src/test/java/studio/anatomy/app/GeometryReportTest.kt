package studio.anatomy.app

import org.junit.Test
import studio.anatomy.app.model.buildScene
import studio.anatomy.app.objects.turbofan

class GeometryReportTest {
    @Test
    fun reportTurbofan() {
        val scene = buildScene(turbofan())
        var total = 0
        var vertices = 0
        scene.parts.forEach {
            total += it.mesh.faces.size
            vertices += it.mesh.vertices.size
        }
        println("TURBOFAN parts=${scene.parts.size} faces=$total vertices=$vertices")
        scene.parts
            .sortedByDescending { it.mesh.faces.size }
            .take(30)
            .forEach {
                println("%-24s %5d faces %6d verts  %s".format(it.id, it.mesh.faces.size, it.mesh.vertices.size, it.material.key))
            }
    }
}
