                                                                  package sketches

import extensions.Camera2D
import org.openrndr.Program
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.fx.Post
import org.openrndr.extra.fx.blur.BoxBlur
import org.openrndr.extra.proxyprogram.proxyApplication


fun main() {
    application {

        program {

            val proxyProgram : Program
            proxyApplication {
                proxyProgram = program {
                    backgroundColor = null

                    mouse.buttonDown.listen {
                        println("mouse down")
                    }
                    mouse.dragged.listen {
                        println("such a drag")
                    }

                    extend(Post()) {
                        val blur = BoxBlur()
                        post { i, o -> blur.apply(i, o) }
                    }
                    extend(Camera2D())
                    this.extend {
                        drawer.rectangle(10.0, 10.0, 200.0, 200.0)
                    }
                }
            }
            println(proxyProgram)

            extend {
                drawer.clear(ColorRGBa.PINK)
                drawer.rectangle(300.0, 300.0, 100.0, 100.0)
                proxyProgram.drawImpl()
            }
        }

    }
}