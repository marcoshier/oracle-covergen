package components

import extensions.QuaternionCamera
import org.openrndr.animatable.Animatable
import org.openrndr.extra.noise.uniform
import org.openrndr.math.Quaternion

class IdleController(val camera: QuaternionCamera, val dataModel:DataModel) : Animatable() {

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

    var counter = 0

    fun update() {
        updateAnimation()

        if (!hasAnimations()) {

            counter ++
            if(counter > 4) {
                camera.instantZoom()
                counter = 0
            }
            ::rotX.animate(Double.uniform(-0.1, 0.1), 100)
            ::rotY.animate(Double.uniform(-0.1, 0.1), 100)
            ::rotY.complete()
            ::dummy.animate(1.0, Int.uniform(200, 3000).toLong())
            ::dummy.complete()
            ::rotX.animate(0.0, 100)
            ::rotY.animate(0.0, 100).completed.listen {

                if (dataModel.activePoints.isNotEmpty()) {
                    ::dummy.animate(1.0, Int.uniform(5000, 8000).toLong())
                    ::dummy.complete()
                } else {
                    ::dummy.animate(1.0, Int.uniform(1000, 2000).toLong())
                    ::dummy.complete()
                }
            }

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