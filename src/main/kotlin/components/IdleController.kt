package components

import extensions.QuaternionCamera
import org.openrndr.animatable.Animatable
import org.openrndr.extra.noise.uniform
import org.openrndr.math.Quaternion

class IdleController(val camera: QuaternionCamera) : Animatable() {

    var active = false

    fun start() {
        active = true
    }

    fun end() {
        active = false
    }

    var rotX = 0.1
    var rotY = 0.1
    var dummy = 0.0

    fun update() {
        updateAnimation()

        if (!hasAnimations()) {
            ::rotX.animate(Double.uniform(-0.1, 0.1), 100)
            ::rotY.animate(Double.uniform(-0.1, 0.1), 100)
            ::rotY.complete()
            ::dummy.animate(1.0, Int.uniform(200, 3000).toLong())
            ::dummy.complete()
            ::rotX.animate(0.0, 100)
            ::rotY.animate(0.0, 100)
            ::dummy.animate(1.0, Int.uniform(2000, 3000).toLong())
            ::dummy.complete()

        }

        if (active) {
            camera.orientation = Quaternion.fromAngles(
                rotX,
                rotY,
                0.0
            ) * camera.orientation

            camera.orientationChanged.trigger(camera.orientation)
        }

    }

}