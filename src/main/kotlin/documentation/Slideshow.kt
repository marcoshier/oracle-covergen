package documentation

import com.google.gson.Gson
import documentation.resources.ArticleData
import documentation.resources.DataModel
import documentation.resources.coverlayProxy
import fillin.TFLITE_MINIMAL_CHECK
import fillin.dimensions
import org.bytedeco.javacpp.Pointer
import org.bytedeco.tensorflowlite.BuiltinOpResolver
import org.bytedeco.tensorflowlite.FlatBufferModel
import org.bytedeco.tensorflowlite.Interpreter
import org.bytedeco.tensorflowlite.InterpreterBuilder
import org.bytedeco.tensorflowlite.global.tensorflowlite
import org.openrndr.Program
import org.openrndr.animatable.Animatable
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.draw.renderTarget
import org.openrndr.extra.noise.Random
import org.openrndr.extra.noise.uniform
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import tools.normalizedVectorToMap
import java.io.File
import javax.xml.crypto.Data

fun main() = application {
    configure {
        width = 540
        height = 960
    }
    program {

        val data = DataModel().data
        val jsons = File("data/new-protovisuals/parameters/").listFiles().filter { it.isFile }.toList().map { it.readText() }


        val cover = coverlayProxy(application)
        var coverData: (json: String, data: List<String>?) -> Unit by cover.userProperties
        val rt = renderTarget(540, 960) {
            colorBuffer()
            depthBuffer()
        }


        class Controller: Animatable() {
            var index = 0
            var dummy = 0.0

            fun start() {
                if(index < jsons.size) {
                    index++
                    dummy = 0.0
                    ::dummy.animate(1.0, 5000).completed.listen {
                        coverData(jsons[index], data[index].toList().filter { it != "" }.plus(index.toString()))
                        start()
                    }
                } else {
                    application.exit()
                }
            }
        }

        val controller = Controller()
        controller.start()

        extend {
            controller.updateAnimation()
            drawer.defaults()

            drawer.isolatedWithTarget(rt) {
                drawer.clear(ColorRGBa.TRANSPARENT)
                cover.drawImpl()
            }

            drawer.image(rt.colorBuffer(0))

        }
    }
}