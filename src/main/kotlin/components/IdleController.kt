package components

import extensions.QuaternionCamera
import org.openrndr.animatable.Animatable
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

    fun update() {
        updateAnimation()

        if (!hasAnimations()) {


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