package extensions

import org.openrndr.Extension
import org.openrndr.Program
import org.openrndr.draw.Drawer
import org.openrndr.events.Event
import org.openrndr.math.Quaternion
import org.openrndr.math.Vector2
import org.openrndr.math.transforms.perspective
import kotlin.math.min

class QuaternionCamera : Extension {
    var orientation = Quaternion.IDENTITY

    var dragStart = Vector2(0.0, 0.0)
    var dragCurrent = Vector2(0.0, 0.0)

    var lastDragTime = 0.0
    var dragCharge = 0.0
    var down = false

    var zoomOutStarted: Event<Unit> = Event()
    var zoomInFinished: Event<Unit> = Event()

    var orientationChanged: Event<Quaternion> = Event()

    var zooming = false

    override fun setup(program: Program) {
        program.mouse.dragged.listen {
            orientation = Quaternion.fromAngles(
                -it.dragDisplacement.x / 10.0,
                -it.dragDisplacement.y / 10.0,
                0.0
            ) * orientation
            orientationChanged.trigger(orientation)
            dragCurrent = it.position
            lastDragTime = program.seconds
            program.window.requestDraw()
        }

        program.mouse.buttonDown.listen {
            dragStart = it.position
            dragCurrent = it.position
            lastDragTime = program.seconds
            down = true
            program.window.requestDraw()
        }

        program.mouse.buttonUp.listen {
            down = false
            program.window.requestDraw()
        }
    }

    override var enabled: Boolean = true

    override fun beforeDraw(drawer: Drawer, program: Program) {
        drawer.pushTransforms()
        if (!down) {
            dragCharge *= 0.99
            if (dragCharge < 0.01) {
                if (zooming) {
                    zooming = false
                    zoomInFinished.trigger(Unit)
                }
            }
        } else {
            val distance = (dragCurrent - dragStart).length
            if (distance > 100.0) {
                dragCharge = min(1.0, dragCharge + (distance - 100.0) / 100000.0)
                if (dragCharge > 0 && !zooming) {
                    zooming = true
                    zoomOutStarted.trigger(Unit)
                }
            }
        }
        val fov = (dragCharge * 35.0 + 12.0).coerceAtMost(45.0)
        drawer.projection = perspective(fov, drawer.width / drawer.height.toDouble(), 0.1, 50.0)
        drawer.view = orientation.matrix.matrix44
    }

    override fun afterDraw(drawer: Drawer, program: Program) {
        drawer.popTransforms()
    }
}