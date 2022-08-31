package tools

import org.openrndr.application
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.loadImage
import org.openrndr.extensions.Screenshots
import java.io.File

fun main() = application {
    configure {
        width = 1080 / 2
        height = 1920 / 2
    }
    program {

        val p = extend(Screenshots()) {
            contentScale = 8.0

        }

        p.trigger()

        extend {

            for(x in 0 until 30) {
                for(y in 0 until 30) {

                    val image = loadImage("screenshots/$x-$y.png")
                    drawer.image(image, x * image.width / 30.0, y * image.height / 30.0, image.width / 30.0, image.height / 30.0)

                }
            }

        }
    }
}