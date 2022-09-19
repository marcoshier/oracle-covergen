package sketches

import org.openrndr.application
import org.openrndr.color.ColorRGBa

fun main() {
    application {
        program {
            val proxyProgram = theProxyApp(application)
            val randomizeColor: (() -> Unit) by proxyProgram.userProperties
            extend {
                drawer.clear(ColorRGBa.PINK)
                drawer.rectangle(300.0, 300.0, 100.0, 100.0)
                // this randomizes the color of proxyProgram
                randomizeColor()
                proxyProgram.drawImpl()
                drawer.circle(mouse.position, 20.0)
            }
        }
    }
}

