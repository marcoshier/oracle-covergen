package components

import animatedCover.fontList
import org.openrndr.animatable.Animatable
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.isolated
import org.openrndr.draw.loadFont

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

    private val message = "Touch to start navigating the digital library"

    fun draw() {
        updateAnimation()

        drawer.isolated {
            defaults()

            stroke = null
            fill = ColorRGBa.BLACK.opacify(0.52 * fade)
            rectangle(drawer.bounds)

            fill = ColorRGBa.WHITE
            fontMap = loadFont(fontList[3].first, 40.0)
            text(message.take((fade * message.length).toInt()), 30.0, drawer.height - 50.0)
        }
    }
}