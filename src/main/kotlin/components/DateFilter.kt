package components

import org.openrndr.MouseEvent
import org.openrndr.animatable.Animatable
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.LineCap
import org.openrndr.draw.isolated
import org.openrndr.draw.loadFont
import org.openrndr.extra.shapes.roundedRectangle
import org.openrndr.math.map
import org.openrndr.shape.LineSegment
import org.openrndr.shape.Rectangle

class DateFilter(val drawer: Drawer): Animatable(){

    lateinit var rect: Rectangle
    lateinit var rail: LineSegment
    inner class Selector(var pos: Double = 0.5) {
        fun draw() {
            drawer.stroke = null
            drawer.fill = ColorRGBa.WHITE
            drawer.circle(rail.position(pos), 25.0)
        }
    }
    val selectors = listOf(Selector(0.3), Selector(0.8))
    var closestSelector: Selector? = null

    fun dragged(mouseEvent: MouseEvent) {
        if (mouseEvent.position in rect && closestSelector != null) {
            val mappedPosition = map(0.0, 1.0, rect.y, rect.y + rect.height, mouseEvent.position.y)

            closestSelector!!.pos = if(closestSelector == selectors[0]) {
                mappedPosition.coerceIn(0.0, selectors[1].pos - 0.1)
            } else {
                mappedPosition.coerceIn(selectors[0].pos + 0.1, 1.0)
            }
        }
    }


    fun buttonDown(mouseEvent: MouseEvent) {
        if (mouseEvent.position in rect) {
            mouseEvent.cancelPropagation()
            closestSelector = selectors.minBy { rail.position(it.pos).distanceTo(mouseEvent.position)}
        }
    }

    fun buttonUp(mouseEvent: MouseEvent) {
    }


    val font1 = loadFont("data/fonts/RobotoCondensed-Bold.ttf", 38.0)

    fun draw() {
        drawer.isolated {
            drawer.defaults()

            rect = Rectangle(width - 60.0, height / 2.0 - 400.0, 40.0, 800.0)
            rail = LineSegment(rect.center.x, rect.y, rect.center.x, rect.y + rect.height)

            drawer.stroke = null
            drawer.fill = ColorRGBa.WHITE.opacify(0.35)
            drawer.roundedRectangle(rect.rounded(20.0))

            drawer.fontMap = font1
            drawer.text("2022", rect.x - 15.0, rect.y - font1.height)
            drawer.text("1880", rect.x - 15.0, rect.y + rect.height + font1.height)

            drawer.stroke = ColorRGBa.WHITE
            drawer.lineSegment(rail)

            selectors.forEach { it.draw() }

            drawer.stroke = ColorRGBa.WHITE
            drawer.strokeWeight = 40.0
            drawer.lineCap = LineCap.ROUND
            drawer.lineSegment(rail.position(selectors[0].pos), rail.position(selectors[1].pos))

        }


    }
}