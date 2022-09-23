package documentation

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.math.Vector2
import org.openrndr.shape.bounds
import org.openrndr.shape.map
import java.io.File

fun main() = application {
    configure {
        width = 1000
        height = 1000
    }
    program {
        val points = mutableListOf<Vector2>()

        File("offline-data/resolved/proto-latent.csv").reader().forEachLine {
            val v = it.split(",").map { it.toDouble() }
            points.add(Vector2(v[0], v[1]))
        }
        val b = points.bounds
        val mappedPoints = points.map(b, drawer.bounds)
        val mb = mappedPoints.bounds

        extend {
            drawer.circles(mappedPoints, 1.0)
            drawer.fill = null
            drawer.stroke = ColorRGBa.PINK
            drawer.rectangle(mb)


        }
    }
}