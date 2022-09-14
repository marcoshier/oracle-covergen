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


class DateFilter(val drawer: Drawer, val model: DateFilterModel): Animatable(){

    var rect = Rectangle(((1920 / 2) * 3) - 60.0, drawer.height / 2.0 - 400.0, 40.0, 800.0)
    var rail = LineSegment(rect.center.x, rect.y, rect.center.x, rect.y + rect.height)

    val font1 = loadFont("data/fonts/RobotoCondensed-Bold.ttf", 38.0)

    inner class Selector(var pos: Double = 0.5) {
        var year = 0.0

        init {
            year = map(0.0, 1.0, 2022.0, 1880.0, pos)
        }

        fun draw() {

            year = map(0.0, 1.0, 2022.0, 1880.0, pos)

            val center = rail.position(pos)

            drawer.stroke = null
            drawer.fill = ColorRGBa.WHITE
            drawer.text(year.toInt().toString(), center.x - 100.0, center.y)
            drawer.circle(center, 25.0)
        }
    }
    val selectors = listOf(Selector(0.0), Selector(1.0))
    var closestSelector: Selector? = null

    var range = listOf(selectors[0].year, selectors[1].year).sorted()

    fun dragged(mouseEvent: MouseEvent) {
        if (mouseEvent.position in rect.offsetEdges(80.0)) {
            mouseEvent.cancelPropagation()

            println("draggin $closestSelector")
            val mappedPosition = map(rect.y, rect.y + rect.height, 0.0, 1.0, mouseEvent.position.y)

            closestSelector?.pos = if(closestSelector == selectors[0]) {
                mappedPosition.coerceIn(0.0, selectors[1].pos - 0.025)
            } else {
                mappedPosition.coerceIn(selectors[0].pos + 0.025, 1.0)
            }
            model.states[0].year = selectors.minBy { it.year }.year
            model.states[1].year = selectors.maxBy { it.year }.year
        }
    }

    fun buttonDown(mouseEvent: MouseEvent) {
        if (mouseEvent.position in rect.offsetEdges(80.0)) {
            mouseEvent.cancelPropagation()

            closestSelector = selectors.minBy { rail.position(it.pos).distanceTo(mouseEvent.position)}
            println("buttondown $closestSelector")

            model.states[0].year = selectors.minBy { it.year }.year
            model.states[1].year = selectors.maxBy { it.year }.year
        }
    }

    fun buttonUp(mouseEvent: MouseEvent) {
        if (mouseEvent.position in rect) {
            mouseEvent.cancelPropagation()

        }
        closestSelector = null
    }


    fun draw() {




        drawer.isolated {
            drawer.defaults()


            range = listOf(selectors[0].year, selectors[1].year).sorted()

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