package sketches

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.draw.renderTarget

fun main() {
    application {
        program {
            val proxyProgram = theProxyApp(application)
            val randomizeColor: (() -> Unit) by proxyProgram.userProperties


            val rt = renderTarget(540, 960) {
                colorBuffer()
                depthBuffer()
            }

            extend {
                drawer.clear(ColorRGBa.PINK)
                drawer.rectangle(300.0, 300.0, 100.0, 100.0)
                // this randomizes the color of proxyProgram
                randomizeColor()

                drawer.defaults()
                drawer.isolatedWithTarget(rt) {
                    drawer.clear(ColorRGBa.TRANSPARENT)
                    proxyProgram.drawImpl()
                }

                drawer.circle(mouse.position, 20.0)
            }
        }
    }
}

