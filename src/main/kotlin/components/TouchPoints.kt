package components

import animatedCover.fontList
import org.openrndr.MouseEvent
import org.openrndr.animatable.Animatable
import org.openrndr.animatable.easing.Easing
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.isolated
import org.openrndr.draw.loadFont
import org.openrndr.draw.writer
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle

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

    private val font = loadFont("data/fonts/RobotoCondensed-Regular.ttf", 24.0)

    fun draw() {
        updateAnimation()
        drawer.isolated {
            defaults()
            drawer.fill = ColorRGBa.WHITE.opacify(0.3)
            drawer.circle(position, radius)

            drawer.fill = ColorRGBa.WHITE.opacify(1.0 - radius / 30.0)
            drawer.fontMap = font

            val text1 = "Drag outwards with one finger (no pinching) to zoom out"
            val f = ((1.0 - radius / 30.0) * text1.length).toInt()
            drawer.text(text1.uppercase().take(f), width - 580.0 * (1.0 - radius / 30.0), height - 58.0)

            val text2 = "or tap on the minimap on the bottom left"
            val f2 = ((1.0 - radius / 30.0) * text2.length).toInt()
            drawer.text(text2.uppercase().take(f2), width - 428.0 * (1.0 - radius / 30.0), height - 27.0)
        }
    }
}