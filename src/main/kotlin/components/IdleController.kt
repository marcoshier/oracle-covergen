package components

import extensions.QuaternionCamera
import org.openrndr.animatable.Animatable
import org.openrndr.math.Quaternion

class IdleController(val camera: QuaternionCamera) : Animatable() {

    var active = false

    fun idleModeStarted() {
        active = true
    }

    fun idleModeEnded() {
        active = false
    }

    var rotX = 0.0
    var rotY = 0.0

    fun update() {
        updateAnimation()

        if (!hasAnimations() && active) {

            Quaternion.fromAngles(
                rotX,
                rotY,
                0.0)


        }

    }

}