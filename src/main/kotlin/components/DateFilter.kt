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

    inner class Selector(var pos: Double = 0.5, val index: Int) {
        var year = model.states[index].year

        fun draw() {
            year = model.states[index].year

            val center = rail.position(pos)

            drawer.stroke = null
            drawer.fill = ColorRGBa.WHITE
            drawer.text("${year.toInt()}", center.x - 100.0, center.y)
            drawer.circle(center, 25.0)
        }
    }
    val selectors = listOf(Selector(1.0, 0), Selector(0.0, 1))
    var closestSelector: Selector? = null

    var range = listOf(selectors[0].year, selectors[1].year)

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

        val years = model.articleYears


        drawer.isolated {
            drawer.defaults()


            range = listOf(selectors[0].year, selectors[1].year)

            rect = Rectangle(width - 60.0, height / 2.0 - 400.0, 40.0, 800.0)
            rail = LineSegment(rect.center.x, rect.y, rect.center.x, rect.y + rect.height)

            //drawer.roundedRectangle(rect.rounded(20.0))

            drawer.fontMap = font1
            drawer.text("2022", rect.x - 50.0, rect.y - font1.height)
            drawer.text("1880", rect.x - 50.0, rect.y + rect.height + font1.height)

            drawer.stroke = ColorRGBa.WHITE
            drawer.lineSegment(rail)

            selectors.forEach { it.draw() }

            drawer.stroke = ColorRGBa.WHITE.opacify(0.2)
            drawer.strokeWeight = 40.0
            drawer.lineCap = LineCap.ROUND
            drawer.lineSegment(rail.position(selectors[0].pos), rail.position(selectors[1].pos))

            drawer.stroke = ColorRGBa.WHITE.opacify(1.0)
            drawer.fill = null
            drawer.strokeWeight = 1.0
            for(j in 0 until  142) {
                val size = years.filter { it == 2022 - j.toFloat() }.size / 85.0
                drawer.lineSegment(rect.x + 10.0 - size, rect.y + (j / 143.0) * rect.height, rect.x + rect.width + size - 10.0, rect.y + (j / 143.0) * rect.height)
            }
        }


    }
}