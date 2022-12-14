package documentation.resources

import org.openrndr.MouseEvent
import org.openrndr.animatable.Animatable
import org.openrndr.animatable.easing.Easing
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.isolated
import org.openrndr.draw.loadFont
import org.openrndr.extra.shapes.RoundedRectangle
import org.openrndr.extra.shapes.roundedRectangle
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle

class FacultyFilter(val drawer: Drawer, val model: FacultyFilterModel) : Animatable() {

    var fade = 1.0
    var dummy = 0.0
    var facultyList = model.facultyList

    fun fadeOut() {
        cancel()
        ::fade.animate(0.0, 800, Easing.QuadInOut)
    }

    fun fadeIn() {
        cancel()
        ::fade.animate(1.0, 1000, Easing.QuadInOut)
    }

    fun dragged(mouseEvent: MouseEvent) {
        for (i in facultyList.indices) {
            if (mouseEvent.position in rectangle(i)) {
                mouseEvent.cancelPropagation()
            }
        }
    }

    fun buttonDown(mouseEvent: MouseEvent) {
        fadeOut()
        for (i in facultyList.indices) {
            if (mouseEvent.position in rectangle(i)) {
                mouseEvent.cancelPropagation()
                model.states[i].visible = !model.states[i].visible
            }
        }
    }

    fun buttonUp(mouseEvent: MouseEvent) {
        dummy = 0.0
        ::dummy.cancel()
        ::dummy.animate(1.0, 3000).completed.listen {
            fadeIn()
        }
    }



    private fun rectangle(index: Int): Rectangle {
        return Rectangle(20.0, drawer.height/2.0 + 120.0 * (index - 4), 100.0, 100.0)
    }

    val font1 = loadFont("data/fonts/RobotoCondensed-Regular.ttf", 38.0)
    val font2 = loadFont("data/fonts/RobotoCondensed-Bold.ttf", 30.0)

    fun draw() {
        updateAnimation()

        drawer.isolated {
            drawer.defaults()
            facultyList.forEachIndexed { index, (name, color) ->
                drawer.stroke = color
                drawer.fill = color.shade(this@FacultyFilter.model.states[index].fade)

                val rf = rectangle(index).rounded()
                drawer.roundedRectangle(rf)


                drawer.fill = ColorRGBa.WHITE
                drawer.stroke = null
                drawer.fontMap = font1


                val text = name.substring(name.indexOf("(")+1,name.indexOf(")")).uppercase()
                val textWidth = text.sumOf { font1.characterWidth(it) }
                drawer.text(text, rf.corner.x + (rf.width - textWidth) / 2.0 - 4.0, rf.center.y + 12.5)

                val tw = this@FacultyFilter.model.states[index].fade * fade
                drawer.fontMap = font2
                drawer.text(name.take(name.indexOf("(") - 1).uppercase().take((name.length * tw).toInt()), rf.corner.x + rf.width + 10.0, rf.center.y + 10.0)
            }
        }
    }
}

fun Rectangle.rounded(radius: Double = 3.0): RoundedRectangle {
    return RoundedRectangle(corner, width, height, radius)
}