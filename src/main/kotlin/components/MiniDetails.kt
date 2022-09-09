package components

import org.openrndr.animatable.Animatable
import org.openrndr.animatable.easing.Easing
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.isolated
import org.openrndr.draw.loadFont
import org.openrndr.math.Polar
import org.openrndr.shape.LineSegment

class MiniDetails(val drawer: Drawer, dataModel: DataModel) : Animatable(){

    var fade: Double = 1.0

    fun fadeIn() {
        cancel()
        ::fade.animate(1.0, 500, Easing.QuadInOut)
    }

    fun fadeOut() {
        cancel()
        ::fade.animate(0.0, 500, Easing.QuadInOut)
    }


    var points = listOf<Int>()

    fun updateActive(oldPoints: List<Int>, newPoints: List<Int>) {
        points = newPoints
    }

    val lineSegments = (0 until 180).map {
        val start = Polar(it * -2.0, 204.0).cartesian
        val end = Polar(it * -2.0, 240.0).cartesian

        LineSegment(start, end)
    }

    fun draw() {
        updateAnimation()
        drawer.isolated {
            drawer.defaults()
            drawer.translate(drawer.bounds.center)
            drawer.stroke = ColorRGBa.WHITE
            drawer.strokeWeight = 2.0
            drawer.lineSegments(lineSegments.takeLast((points.size * fade).toInt()))

            if (points.size > 0) {
                drawer.fill = ColorRGBa.WHITE.opacify(fade)
                drawer.fontMap = loadFont("data/fonts/default.otf", 32.0)
                drawer.text("${(points.size*fade).toInt()}", 204.0, 0.0)
            }

        }
    }


}