package sketches

import org.openrndr.Application
import org.openrndr.Program
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.noise.uniform
import org.openrndr.extra.proxyprogram.proxyApplication

fun theProxyApp(parent: Application? = null): Program = proxyApplication(parent) {
    program {


        var c = ColorRGBa.RED
        var randomizeColor: () -> Unit by userProperties
        randomizeColor = {
            c = ColorRGBa(Double.uniform(), Double.uniform(), Double.uniform())
        }

        val gui = GUI()
        extend(gui)
        if (parent != null) {
            backgroundColor = null
        }
        extend {
            println(drawer.bounds)
            drawer.fill = c
            drawer.circle(mouse.position, 50.0)
        }
    }
}

fun main() = run { theProxyApp(); Unit }
