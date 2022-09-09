package components

import org.openrndr.MouseEvent
import org.openrndr.animatable.Animatable
import org.openrndr.animatable.easing.Easing
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.isolated
import org.openrndr.math.Vector2

class TouchPoints(val drawer: Drawer) : Animatable() {

    var radius = 0.0
    var position = Vector2.ZERO

    fun buttonDown(event: MouseEvent) {
        cancel()
        ::radius.animate(30.0, 300, Easing.QuadInOut)
        position = event.position
    }

    fun dragged(event: MouseEvent) {
        position = event.position
    }

    fun buttonUp(event: MouseEvent) {
        cancel()
        ::radius.animate(0.0, 300, Easing.QuadInOut)
        position = event.position
    }

    fun draw() {
        updateAnimation()
        drawer.isolated {
            defaults()
            drawer.fill = ColorRGBa.WHITE.opacify(0.3)
            drawer.circle(position, radius)
        }
    }
}