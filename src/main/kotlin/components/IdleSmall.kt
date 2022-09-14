package components

import org.openrndr.animatable.Animatable
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.isolated

class IdleSmall(val drawer: Drawer): Animatable() {

    var fade = 0.0

    fun fadeIn() {
        cancel()
        ::fade.animate(1.0, 1500)
    }

    fun fadeOut() {
        cancel()
        ::fade.animate(0.0, 1500)
    }

    fun draw() {
        updateAnimation()

        drawer.isolated {
            defaults()

            drawer.fill = ColorRGBa.GREEN.opacify(fade)
            drawer.circle(100.0, 100.0, 100.0)
        }
    }
}