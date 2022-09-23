/*
package textSandbox

import org.openrndr.KEY_SPACEBAR
import org.openrndr.application
import org.openrndr.math.IntVector2

fun main() = application {
    configure {
        width = 1080
        height = 1920
        position = IntVector2(2000, -2120)
    }
    program {
        val testData = listOf("Houtbouwmythes ontkracht: het onderscheid tussen fabels en feiten", "van der Lugt, P. (TU Delft Environmental Technology and Design)", "Faculty name for example", "Department thing", "24-09-18")
        val c = Coverlay(drawer, data = testData).apply {
            subdivide(Section(drawer.bounds.offsetEdges(-100.0)))
            unfold()
        }

        var folded = false
        keyboard.keyUp.listen {
            if(it.key == KEY_SPACEBAR) {
                if (!folded) {
                    c.fold()
                    folded = true
                } else {
                    c.unfold()
                    folded = false
                }
            }
        }
        extend {

            c.draw(1.0)

        }
    }
}*/
