package documentation

import documentation.resources.ArticleData
import documentation.resources.DataModel
import documentation.resources.coverlayProxy
import org.openrndr.Program
import org.openrndr.animatable.Animatable
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.draw.renderTarget
import java.io.File
import javax.xml.crypto.Data

fun main() = application {
    configure {
        width = 1706
        height = 960
    }
    program {

        var data = DataModel().data
        var jsons = (0..2).flatMap { x -> (0..29).map { y-> File("data/xyNew/$x-$y.json").readText() } }

        val covers = (0..1).map { Pair(coverlayProxy(application), renderTarget(540, 960) {
            colorBuffer()
            depthBuffer()
        }) }
        var passSliderValues: (json: String, data: ArticleData) -> Unit by userProperties

        class Controller: Animatable() {
            var index0 = 0
            var index1 = 1

            var first = 0.0
            var second = 0.0

            init {
                passSliderValues(jsons[0], data[0])
                ::first.animate(1.0, 5000)
            }
        }

        extend {
            drawer.clear(ColorRGBa.GRAY)

            covers.forEach { (program, rt)  ->
                drawer.isolatedWithTarget(rt) {
                    drawer.clear(ColorRGBa.TRANSPARENT)
                    program.drawImpl()
                }
            }

            drawer.image(covers[0].second.colorBuffer(0), (width / 2 - 540.0) / 2.0, 0.0, 540.0, 960.0)
            drawer.image(covers[1].second.colorBuffer(0), width / 2 + ((width / 2 - 540.0) / 2.0), 0.0, 540.0, 960.0)

        }
    }
}