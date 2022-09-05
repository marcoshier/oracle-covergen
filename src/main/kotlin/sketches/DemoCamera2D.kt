package sketches

import extensions.Camera2D
import org.openrndr.application

fun main() {
    application {
        program {
        extend(Camera2D())
            extend {
                drawer.circle(100.0, 100.0, 100.0)
            }
        }
    }
}