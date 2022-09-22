package extensions

import org.openrndr.Extension
import org.openrndr.Program
import org.openrndr.animatable.Animatable
import org.openrndr.animatable.easing.Easing
import org.openrndr.draw.Drawer
import org.openrndr.draw.isolated
import org.openrndr.events.Event
import org.openrndr.math.Quaternion
import org.openrndr.math.Vector2
import org.openrndr.math.mix
import org.openrndr.math.transforms.perspective
import kotlin.math.min

class QuaternionCamera : Extension {
    var orientation = Quaternion.IDENTITY

    var dragStart = Vector2(0.0, 0.0)


    val minTravel = 350.0
    var maxZoomOut = 90.0

    var zoomOutStarted: Event<Unit> = Event()
    var zoomInStarted: Event<Unit> = Event()
    var zoomInFinished: Event<Unit> = Event()

    var zoomLockStarted: Event<Unit> = Event()
    var zoomLockFinished: Event<Unit> = Event()

    var orientationChanged: Event<Quaternion> = Event()

    var buttonDown = false

    inner class Zoom: Animatable() {

        var locked = false

        var dragCharge = 0.0
            set(value) {
                if (field != value) {

                    if (value >= 1.0 && value > field) {
                        if (!locked) {
                            locked = true
                            zoomLockStarted.trigger(Unit)
                       }
                   }
                    field = value

                }
            }

        var dragChargeIncrement = 0.0
            set(value) {
                if (field != value) {
                    if (field == 0.0) {
                        zoomOutStarted.trigger(Unit)
                    }
                    field = value
                }
            }
        fun update() {
            updateAnimation()
            dragCharge += dragChargeIncrement
        }

        fun discharge() {
            println("discharging")
            dragChargeIncrement = 0.0
            dragCharge = min(1.0, dragCharge)
            cancel()
            animate(::dragCharge, 0.0, (8500 * dragCharge).coerceAtMost(1000.0).toLong(), Easing.QuadOut).completed.listen {
                zoomInFinished.trigger(Unit) }
        }
    }
    val zoom = Zoom()


    fun instantZoom() {
        if(!zoom.locked) {
            zoom.dragChargeIncrement = 0.01
            zoomLockStarted.trigger(Unit)
            zoom.locked = true
        }
    }


    fun unlockZoom() {
        zoom.locked = false
        zoom.discharge()
        zoomLockFinished.trigger(Unit)
    }

    override fun setup(program: Program) {
        program.mouse.buttonDown.listen {
            if (!it.propagationCancelled) {
                buttonDown = true
                zoom.cancel()
                dragStart = it.position
                program.window.requestDraw()
            }
        }

        program.mouse.dragged.listen {
            if (!it.propagationCancelled) {
                if (!buttonDown) {
                    return@listen
                }
                val sensity = mix(1.0 / 100.0, 1.0 / 10.0, zoom.dragCharge)

                orientation = Quaternion.fromAngles(
                    it.dragDisplacement.x * sensity,
                    it.dragDisplacement.y * sensity,
                    0.0
                ) * orientation
                orientationChanged.trigger(orientation)


                    val distance = (it.position - dragStart).length
                    if (distance > minTravel) {
                        zoom.dragChargeIncrement = (distance - minTravel) / 100000.0
                    }
                    zoom.cancel()

            }
        }


        program.mouse.buttonUp.listen {
            if (!it.propagationCancelled) {
                buttonDown = false

                if (!zoom.locked) {
                    zoom.discharge()
                }
            }
        }
    }

    override var enabled: Boolean = true

    override fun beforeDraw(drawer: Drawer, program: Program) {
        zoom.update()
        drawer.pushTransforms()
        val fov = (zoom.dragCharge * 76.0 + 12.0).coerceAtMost(maxZoomOut)
        drawer.projection = perspective(fov, 2880.0/1920.0, 0.1, 50.0)
        drawer.view = orientation.matrix.matrix44
    }

    override fun afterDraw(drawer: Drawer, program: Program) {
        drawer.popTransforms()
    }
}