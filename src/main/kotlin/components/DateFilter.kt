package components

import org.openrndr.MouseEvent
import org.openrndr.animatable.Animatable
import org.openrndr.animatable.easing.Easing
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.LineCap
import org.openrndr.draw.isolated
import org.openrndr.draw.loadFont
import org.openrndr.extra.shapes.roundedRectangle
import org.openrndr.math.Vector2
import org.openrndr.math.map
import org.openrndr.math.mix
import org.openrndr.shape.LineSegment
import org.openrndr.shape.Rectangle


class DateFilter(val drawer: Drawer, val model: DateFilterModel): Animatable(){

    var fade = 0.0

    var rect = Rectangle(((1920 / 2) * 3) - 60.0, drawer.height / 2.0 - 400.0, 40.0, 800.0)
    var rail = LineSegment(rect.center.x, rect.y, rect.center.x, rect.y + rect.height)

    val font1 = loadFont("data/fonts/RobotoCondensed-Bold.ttf", 38.0)

    inner class Selector(var state: DateState) {

        var pos: Double
            get() {
                return state.year.map(2020.0, 1880.0,0.0, 1.0)
            }
            set(value) {
                state.year = value.map(0.0, 1.0, 2020.0, 1880.0)
            }


        fun draw() {
            val center = rail.position(pos)
            drawer.stroke = null
            drawer.fill = ColorRGBa.WHITE
            drawer.text("${state.year.toInt()}", center.x - 120.0, center.y)
            drawer.circle(center, 25.0)
        }
    }

    val selectors = model.states.map { Selector(it) }
    var closestSelector: Selector? = null

    fun dragged(mouseEvent: MouseEvent) {
        if (mouseEvent.position in rect.offsetEdges(80.0)) {
            mouseEvent.cancelPropagation()

            println("draggin $closestSelector")
            val mappedPosition = map(rect.y, rect.y + rect.height, 0.0, 1.0, mouseEvent.position.y)

            closestSelector?.pos = mappedPosition.coerceIn(0.0, 1.0)
        }
    }

    fun buttonDown(mouseEvent: MouseEvent) {
        if (mouseEvent.position in rect.offsetEdges(80.0)) {
            mouseEvent.cancelPropagation()

            closestSelector = selectors.minBy { rail.position(it.pos).distanceTo(mouseEvent.position)}

            cancel()
            ::fade.animate(1.0, 400, Easing.QuadInOut)
            println("buttondown $closestSelector")
        }
    }

    fun buttonUp(mouseEvent: MouseEvent) {
        if (mouseEvent.position in rect) {
            mouseEvent.cancelPropagation()

            cancel()
            ::fade.animate(0.0, 400, Easing.QuadInOut, 3000)
        }
        closestSelector = null
    }


    fun draw() {

        val years = model.articleYears


        drawer.isolated {
            drawer.defaults()

            rect = Rectangle(width - 60.0, height / 2.0 - 400.0, 40.0, 800.0)
            rail = LineSegment(rect.center.x, rect.y, rect.center.x, rect.y + rect.height)

            drawer.stroke = null
            drawer.fill = ColorRGBa.WHITE.opacify(0.35)
            //drawer.roundedRectangle(rect.rounded(20.0))

            drawer.fontMap = font1
            drawer.text("2022", rect.x - 15.0, rect.y - font1.height)
            drawer.text("1880", rect.x - 15.0, rect.y + rect.height + font1.height)

            drawer.stroke = ColorRGBa.WHITE
            drawer.lineSegment(rail)

            selectors.forEach { it.draw() }

            drawer.stroke = ColorRGBa.WHITE.opacify(0.1)
            drawer.strokeWeight = 40.0
            drawer.lineCap = LineCap.ROUND
            drawer.lineSegment(rail.position(selectors[0].pos), rail.position(selectors[1].pos))

            drawer.fill = null
            drawer.stroke = ColorRGBa.WHITE.opacify(mix(0.1, 1.0, fade))
            drawer.strokeWeight = mix(11.0, 1.0, fade)
            drawer.lineCap = LineCap.BUTT


            for(j in 0 until 143) {
                val size = (years.filter { it == (2022 - j).toFloat() }.size / 85.0)
                val animSize = mix(size, rect.width / 2.0, 1.0 - fade)
                drawer.lineSegment(rect.center.x - animSize, rect.y + (j / 143.0) * rect.height, rect.center.x + animSize,rect.y + (j / 143.0) * rect.height)
            }

        }


    }
}