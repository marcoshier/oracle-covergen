package documentation

import animatedCover.opacify
import com.google.gson.Gson
import documentation.resources.ArticleData
import documentation.resources.DataModel
import documentation.resources.coverlayProxy
import documentation.resources.coverlayResources.labels
import fillin.TFLITE_MINIMAL_CHECK
import fillin.dimensions
import org.bytedeco.javacpp.Pointer
import org.bytedeco.tensorflowlite.BuiltinOpResolver
import org.bytedeco.tensorflowlite.FlatBufferModel
import org.bytedeco.tensorflowlite.Interpreter
import org.bytedeco.tensorflowlite.InterpreterBuilder
import org.bytedeco.tensorflowlite.global.tensorflowlite
import org.openrndr.ExtensionStage
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.shapes.grid
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.math.IntVector2
import org.openrndr.math.Vector2
import org.openrndr.math.map
import org.openrndr.panel.controlManager
import org.openrndr.panel.elements.bind
import org.openrndr.panel.elements.xyPad
import org.openrndr.panel.style.*
import org.openrndr.shape.LineSegment
import org.openrndr.shape.Rectangle
import tools.normalizedVectorToMap
import java.io.File
import kotlin.math.max
import kotlin.math.min

fun main() = application {
    configure {
        width = 1000
        height = 1000
        //position = IntVector2(1200, -1700)
    }

    class State {
        var pos = Vector2.ZERO
    }

    program {

        val data = DataModel().data

        val templateJson = File("data/template.json").readText()
        val model: FlatBufferModel = FlatBufferModel.BuildFromFile("data/models/decoder.tflite")
        fun interpreter(): Interpreter {
            TFLITE_MINIMAL_CHECK(model != null && !model.isNull())
            val resolver = BuiltinOpResolver()
            val builder = InterpreterBuilder(model, resolver)
            val interpreter = Interpreter(null as Pointer?)
            builder.apply(interpreter)
            TFLITE_MINIMAL_CHECK(interpreter != null && !interpreter.isNull())

            TFLITE_MINIMAL_CHECK(interpreter.AllocateTensors() == tensorflowlite.kTfLiteOk)
            System.out.println("=== Pre-invoke Interpreter State ===");
            //PrintInterpreterState(interpreter);
            return interpreter
        }
        val interpreter = interpreter()


        val proxyProgram = coverlayProxy(application)
        var coverData: (json: String, data: ArticleData?) -> Unit by proxyProgram.userProperties
        val rt = renderTarget(563, 1000) {
            colorBuffer()
            depthBuffer()
        }

        val extremes = findExtremes().also { println(it) }
        val programState = State()

        fun outputNormalizedValues(pos: Vector2): List<Float> {
            println(model)
            val inputTensor = interpreter.typed_input_tensor_float(0)
            inputTensor.put(0, pos[0].toFloat())
            inputTensor.put(1, pos[1].toFloat())

            TFLITE_MINIMAL_CHECK(interpreter.Invoke() == tensorflowlite.kTfLiteOk)

            val outputTensor = interpreter.typed_output_tensor_float(0)
            val normalized = (0 until dimensions).map { outputTensor.get(it.toLong()) }

            return normalized
        }

        var normalizedSliderValues = outputNormalizedValues(programState.pos).map { it.toDouble() }

        val cm = controlManager {

            layout {
                xyPad {

                    minX = extremes.first[0]
                    minY = extremes.second[0]
                    maxX = extremes.first[1]
                    maxY = extremes.second[1]
                    precision = 3

                    style = styleSheet {
                        marginTop = 600.px
                        marginLeft = 50.px
                        this.width = 350.px
                        this.height = 350.px
                        background = Color.RGBa(ColorRGBa.BLACK)
                    }

                    bind(programState::pos)

                    events.valueChanged.listen {
                        normalizedSliderValues = outputNormalizedValues(it.newValue).map { it.toDouble() }
                        val map = normalizedVectorToMap(templateJson, normalizedSliderValues)

                        val json = Gson().toJson(map)

                        coverData(json, null)
                    }
                }
            }
        }

        //extend(ScreenRecorder())

        val slidersSpace = Rectangle(50.0, 50.0, 350.0, 430.0)
        val slidersGrid = slidersSpace.grid(6, 8).flatten()

        class Slider(var frame: Rectangle, var label: String) {

            init {
                frame = frame.offsetEdges(-5.0)
            }

            var pos = 0.5
            var rail = LineSegment(frame.x, frame.y + frame.height - 10.0, frame.x + frame.width, frame.y + frame.height - 10.0)

            fun update(value: Double) {
                pos = value
            }

            val font = loadFont("data/fonts/default.otf", 9.0)

            fun draw() {

                drawer.fill = ColorRGBa.WHITE
                drawer.fontMap = font
                drawer.text(label.uppercase(), frame.x, frame.y + 14.0)

                drawer.circle(rail.position(pos), 3.0)

                drawer.stroke = ColorRGBa.WHITE
                drawer.lineSegment(rail)
            }
        }
        val sliders = slidersGrid.take(47).mapIndexed { i, it -> Slider(it, labels[i]) }

        /*
                val latentGrid = loadImage("data/latentSpace.jpg")

                extend(ScreenRecorder())
                extend(stage = ExtensionStage.AFTER_DRAW) {
                    drawer.defaults()
                    drawer.drawStyle.blendMode = BlendMode.ADD
                    drawer.image(latentGrid, 50.0, 600.0, 350.0, 350.0)
                    drawer.drawStyle.blendMode = BlendMode.BLEND
                }*/
        extend(cm)
        extend {
            drawer.defaults()


            drawer.stroke = ColorRGBa.WHITE
            drawer.fill = ColorRGBa.BLACK

            normalizedSliderValues.forEachIndexed { index, it ->
                sliders[index].update(it)
            }

            sliders.forEach {
                it.draw()
            }

            //

            drawer.isolatedWithTarget(rt) {
                drawer.clear(ColorRGBa.TRANSPARENT)
                proxyProgram.drawImpl()
            }

            drawer.translate(width - 563.0, 0.0)
            drawer.image(rt.colorBuffer(0))


        }
    }
}




fun findExtremes(): Pair<Vector2, Vector2>{

    var minValue0 = 0.0
    var minValue1 = 0.0

    var maxValue0 = 0.0
    var maxValue1 = 0.0


    File("offline-data/graph/cover-latent.csv").reader().forEachLine { string ->

        if(!string.contains("x")) {
            val v = string.split(",").map { it.toDouble() }

            minValue0 = min(v[0], minValue0)
            minValue1 = min(v[1], minValue1)

            maxValue0 = max(v[0], maxValue0)
            maxValue1 = max(v[1], maxValue1)
        }
    }

    return Pair(Vector2(minValue0, maxValue0), Vector2(minValue1, maxValue1))
}
