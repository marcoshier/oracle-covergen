package tsnefix

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.math.Vector2
import org.openrndr.shape.bounds
import org.openrndr.shape.map
import java.io.File

fun main() = application {

    configure {
        width = 800
        height = 800
    }
    program {
        val points = csvReader().readAllWithHeader(File("corrected.csv")).map {
            Vector2(it["x"]!!.toDouble(), it["y"]!!.toDouble())
        }
        println("number of protovisuals ${points.size}")
        val b = points.bounds
        val mappedPoints = points.map { it.map(b, drawer.bounds)}
        val mb = mappedPoints.bounds

        extend {
            drawer.circles(mappedPoints, 2.0)
            drawer.fill = null
            drawer.stroke = ColorRGBa.PINK
            drawer.rectangle(mb)


        }
    }
}