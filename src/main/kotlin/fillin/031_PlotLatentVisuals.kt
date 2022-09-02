package fillin

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.math.Vector2
import org.openrndr.shape.bounds
import java.io.File

fun main() = application {

    configure {
        width = 800
        height = 800
    }
    program {
        val points = mutableListOf<Vector2>()

        File("offline-data/resolved/cover-latent.csv").reader().forEachLine {
            val v = it.split(",").map { it.toDouble() }


            points.add(Vector2(v[0], v[1]))
        }

        println("number of covers ${points.size}")
        val b = points.bounds
        println(b)
        val mappedPoints = points.map { (it - b.corner) * 150.0 }
        val mb = mappedPoints.bounds

        extend {
            drawer.circles(mappedPoints, 5.0)
            drawer.fill = null
            drawer.stroke = ColorRGBa.PINK
            drawer.rectangle(mb)


        }
    }
}