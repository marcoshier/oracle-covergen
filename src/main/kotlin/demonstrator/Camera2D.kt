package demonstrator

import org.openrndr.Extension
import org.openrndr.Program
import org.openrndr.draw.Drawer
import org.openrndr.math.Matrix44
import org.openrndr.math.transforms.buildTransform

class Camera2D : Extension {
    override var enabled = true
    var view = Matrix44.IDENTITY
    override fun setup(program: Program) {
        program.mouse.dragged.listen {
            view = buildTransform { translate(it.dragDisplacement) } * view
        }
        program.mouse.scrolled.listen {
            val scaleFactor = 1.0 - it.rotation.y * 0.03
            view = buildTransform {
                translate(it.position)
                scale(scaleFactor)
                translate(-it.position)
            } * view
        }
    }

    override fun beforeDraw(drawer: Drawer, program: Program) {
        drawer.view = view
    }
}