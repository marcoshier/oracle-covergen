package extensions

import org.openrndr.Extension
import org.openrndr.Program
import org.openrndr.animatable.Animatable
import org.openrndr.animatable.easing.Easing
import org.openrndr.draw.Drawer
import org.openrndr.events.Event
import org.openrndr.math.Quaternion
import org.openrndr.math.Vector2
import org.openrndr.math.transforms.perspective
import kotlin.math.min

class QuaternionCamera : Extension {
    var orientation = Quaternion.IDENTITY

    var dragStart = Vector2(0.0, 0.0)

    var lastDragTime = 0.0


    var zoomOutStarted: Event<Unit> = Event()
    var zoomInStarted: Event<Unit> = Event()
    var zoomInFinished: Event<Unit> = Event()

    var orientationChanged: Event<Quaternion> = Event()


    inner class Zoom: Animatable() {
        var dragCharge = 0.0
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
            dragChargeIncrement = 0.0
            cancel()
            animate(::dragCharge, 0.0, (8500 * dragCharge).toLong(), Easing.QuadOut).completed.listen {
                zoomInFinished.trigger(Unit) }
        }
    }
    val zoom = Zoom()


    override fun setup(program: Program) {


        program.mouse.buttonDown.listen {
            println("buttonDown")
            zoom.cancel()
            dragStart = it.position
            lastDragTime = program.seconds
            program.window.requestDraw()
        }

        program.mouse.dragged.listen {
            println("dragged")
            orientation = Quaternion.fromAngles(
                -it.dragDisplacement.x / 10.0,
                -it.dragDisplacement.y / 10.0,
                0.0
            ) * orientation
            orientationChanged.trigger(orientation)

            lastDragTime = program.seconds

            val distance = (it.position - dragStart).length
            if (distance > 100.0) {
                zoom.dragChargeIncrement = (distance - 100.0) / 100000.0
            }

            zoom.cancel()
            program.window.requestDraw()
        }


        program.mouse.buttonUp.listen {
            zoom.discharge()
            program.window.requestDraw()
        }
    }

    override var enabled: Boolean = true

    override fun beforeDraw(drawer: Drawer, program: Program) {
        zoom.update()
        drawer.pushTransforms()
        val fov = (zoom.dragCharge * 35.0 + 12.0).coerceAtMost(45.0)
        drawer.projection = perspective(fov, drawer.width / drawer.height.toDouble(), 0.1, 50.0)
        drawer.view = orientation.matrix.matrix44
    }

    override fun afterDraw(drawer: Drawer, program: Program) {
        drawer.popTransforms()
    }
}