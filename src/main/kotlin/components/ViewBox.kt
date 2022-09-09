package components

import org.openrndr.color.ColorRGBa.Companion.TRANSPARENT
import org.openrndr.draw.Drawer
import org.openrndr.draw.isolated
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.draw.renderTarget
import org.openrndr.math.Vector2

class ViewBox(val drawer: Drawer, val position: Vector2, val width:Int, val height: Int, val drawFunction:()->Unit) {
    val target = renderTarget(width, height) {
        colorBuffer()
        depthBuffer()
    }

    fun draw() {
        drawer.isolatedWithTarget(target) {
            drawer.clear(TRANSPARENT)
            drawFunction()
        }
        drawer.isolated {
            drawer.defaults()
            drawer.ortho(0.0, 2880.0+1080.0, 1920.0,0.0,-1.0, 1.0)
            drawer.image(target.colorBuffer(0), position)
        }
    }

}