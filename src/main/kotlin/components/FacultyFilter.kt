package components

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

    var fade = 0.0

    fun fadeOut() {
        cancel()
        ::fade.animate(0.0, 500, Easing.QuadInOut)
    }

    fun fadeIn() {
        cancel()
        ::fade.animate(1.0, 500, Easing.QuadInOut)
    }

    fun dragged(mouseEvent: MouseEvent) {
        for (i in faculties.indices) {
            if (mouseEvent.position in rectangle(i)) {
                mouseEvent.cancelPropagation()
            }
        }
    }

    fun buttonDown(mouseEvent: MouseEvent) {
        for (i in faculties.indices) {
            if (mouseEvent.position in rectangle(i)) {
                mouseEvent.cancelPropagation()
                model.states[i].visible = !model.states[i].visible
            }
        }
    }

    fun buttonUp(mouseEvent: MouseEvent) {
        fadeIn()
        println("clicked")
        for(i in faculties.indices) {
            if(mouseEvent.position in rectangle(i)) {
                mouseEvent.cancelPropagation()
                model.states[i].visible = true
            } else {
                model.states[i].visible = false
            }
        }
    }

    val faculties = listOf(
        Pair("Architecture and the Built Environment", ColorRGBa.fromHex("2D5BFF")),
        Pair("Aerospace Engineering", ColorRGBa.fromHex("A5A5A5")),
        Pair("Applied Sciences", ColorRGBa.fromHex("C197FB")),
        Pair("Civil Engineering and Geosciences", ColorRGBa.fromHex("E1A400")),
        Pair("Electrical Engineering, Mathematics & Computer Science", ColorRGBa.fromHex("19CC78")),
        Pair("Industrial Design Engineering", ColorRGBa.fromHex("E54949")),
        Pair("Mechanical Maritime and Materials Engineering", ColorRGBa.fromHex("00A8B4")),
        Pair("Technology, Policy and Management", ColorRGBa.fromHex("FFAD8F")),
    )

    private fun rectangle(index: Int): Rectangle {
        return Rectangle(80.0, drawer.height/2.0 + 120.0 * (index - 4), 100.0, 100.0)
    }

    val font1 = loadFont("data/fonts/RobotoCondensed-Bold.ttf", 36.0)
    val font2 = loadFont("data/fonts/RobotoCondensed-Bold.ttf", 30.0)

    fun draw() {
        updateAnimation()

        drawer.isolated {
            drawer.defaults()
            faculties.forEachIndexed { index, (name, color) ->
                drawer.stroke = color
                drawer.fill = color.shade(this@FacultyFilter.model.states[index].fade)

                val rf = rectangle(index).rounded()
                drawer.roundedRectangle(rf)


                drawer.fill = ColorRGBa.WHITE
                drawer.stroke = null
                drawer.fontMap = font1

                val text = name.partition { it.isUpperCase() }.first
                val textWidth = text.sumOf { font1.characterWidth(it) }
                drawer.text(text, rf.corner.x + (rf.width - textWidth) / 2.0 - 4.0, rf.center.y + 10.0)

                val tw = this@FacultyFilter.model.states[index].fade
                drawer.fontMap = font2
                drawer.text(name.uppercase().take((name.length * tw).toInt()), rf.corner.x + rf.width + 10.0, rf.center.y + 10.0)
            }
        }
    }
}

fun Rectangle.rounded(radius: Double = 3.0): RoundedRectangle {
    return RoundedRectangle(corner, width, height, radius)
}