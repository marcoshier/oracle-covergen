package components

import org.openrndr.MouseEvent
import org.openrndr.animatable.Animatable
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.isolated
import org.openrndr.draw.loadFont
import org.openrndr.extra.shapes.roundedRectangle
import org.openrndr.shape.LineSegment
import org.openrndr.shape.Rectangle

class DateFilter(val drawer: Drawer): Animatable(){

    lateinit var rect: Rectangle
    lateinit var rail: LineSegment
    inner class Selector() {
    }
    val selectors = listOf(0.4, 0.6)

    fun dragged(mouseEvent: MouseEvent) {
        if (mouseEvent.position in rect) {
            val closest = selectors.minBy { rail.position(it).distanceTo(mouseEvent.position)}
        }
    }

    fun buttonDown(mouseEvent: MouseEvent) {
    }

    fun buttonUp(mouseEvent: MouseEvent) {
    }


    val font1 = loadFont("data/fonts/RobotoCondensed-Bold.ttf", 64.0)
    val font2 = loadFont("data/fonts/RobotoCondensed-Bold.ttf", 64.0)



    fun draw() {
        drawer.isolated {
            drawer.defaults()

            rect = Rectangle(width - 60.0, height / 2.0 - 400.0, 40.0, 800.0)
            rail = LineSegment(rect.center.x, rect.y, rect.center.x, rect.y + rect.height)

            drawer.stroke = null
            drawer.fill = ColorRGBa.WHITE.opacify(0.5)
            drawer.roundedRectangle(rect.rounded(20.0))

            drawer.text("2022", rect.x, rect.y - font1.height)
            drawer.text("1880", rect.x, rect.y + rect.height + font1.height)

            drawer.stroke = ColorRGBa.WHITE
            drawer.lineSegment(rail)
            drawer.circle(rail.position(0.5), 20.0)

        }


    }
}