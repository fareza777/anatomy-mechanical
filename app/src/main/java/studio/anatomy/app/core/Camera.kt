package studio.anatomy.app.core

import kotlin.math.cos
import kotlin.math.sin

data class CameraPose(
    val yaw: Float = 38f,
    val pitch: Float = 27f,
    val zoom: Float = 1f,
) {
    fun clamped() = copy(
        yaw = wrapDegrees(yaw),
        pitch = pitch.coerceIn(-8f, 84f),
        zoom = zoom.coerceIn(0.55f, 2.4f),
    )
}

data class CameraBasis(
    val right: V3,
    val up: V3,
    val eye: V3,
    val forward: V3,
)

fun cameraBasis(pose: CameraPose): CameraBasis {
    val yaw = Math.toRadians(pose.yaw.toDouble())
    val pitch = Math.toRadians(pose.pitch.toDouble())
    val eye = V3(
        (cos(pitch) * sin(yaw)).toFloat(),
        sin(pitch).toFloat(),
        (cos(pitch) * cos(yaw)).toFloat(),
    ).normalized()
    val worldUp = V3.Y
    var right = worldUp.cross(eye)
    if (right.lengthSq() < 1e-5f) right = V3.X
    right = right.normalized()
    val up = eye.cross(right).normalized()
    return CameraBasis(right = right, up = up, eye = eye, forward = -eye)
}

data class PoseConfig(
    val yaw: Float,
    val pitch: Float,
    val zoom: Float = 1f,
)

fun PoseConfig.toPose() = CameraPose(yaw, pitch, zoom)
