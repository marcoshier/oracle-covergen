package components

import org.openrndr.MouseEvent
import org.openrndr.animatable.Animatable
import org.openrndr.animatable.easing.Easing
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.isolated
import org.openrndr.draw.loadFont
import org.openrndr.shape.Rectangle

class FacultyFilter(val drawer: Drawer, val model: FacultyFilterModel) : Animatable() {

    var fade = 1.0

    fun fadeOut() {
        cancel()
        ::fade.animate(0.0, 500, Easing.QuadInOut)
    }

    fun fadeIn() {
        cancel()
        ::fade.animate(1.0, 500, Easing.QuadInOut)
    }

    fun dragged(mouseEvent: MouseEvent) {
        for (i in names.indices) {
            if (mouseEvent.position in rectangle(i)) {
                mouseEvent.cancelPropagation()
            }
        }
    }

    fun buttonDown(mouseEvent: MouseEvent) {
        for (i in names.indices) {
            if (mouseEvent.position in rectangle(i)) {
                mouseEvent.cancelPropagation()
                model.states[i].visible = !model.states[i].visible
            }
        }
    }

    fun buttonUp(mouseEvent: MouseEvent) {

    }

    val names = listOf(
        "Architecture",
        "Physics",
        "Information Technology",
        "Chemistry",
        "Aero",
        "Faculty 7",
        "Faculty 8",
        "Faculty 10"
    )

    private fun rectangle(index: Int): Rectangle {
        val r = Rectangle(80.0, drawer.height/2.0 + 140.0 * (index-3.5), 550.0, 80.0)
        val rt = Rectangle( 80.0 + index * 550.0/ 8.0, drawer.height/2.0+140.0 * 0.0, 550.0/8.0, 80.0 )
        val rf = r * fade + rt * (1.0-fade)
        return rf
    }


    fun draw() {
        updateAnimation()

        drawer.isolated {
            drawer.defaults()
            for ((index, name) in names.withIndex()) {
                drawer.fill = ColorRGBa.GRAY.shade(0.5 + 0.5 * this@FacultyFilter.model.states[index].fade)
                val rf = rectangle(index)
                drawer.rectangle(rf)
                drawer.fill = ColorRGBa.WHITE
                drawer.stroke = null
                drawer.fontMap = loadFont("data/fonts/default.otf", 48.0)


                drawer.text(name.take((name.length*fade).toInt().coerceAtLeast(1)), rf.corner.x + 20.0, rf.center.y + 10.0)
            }
        }
    }
}