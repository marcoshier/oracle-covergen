package documentation

import fillin.TFLITE_MINIMAL_CHECK
import fillin.dimensions
import org.bytedeco.javacpp.Pointer
import org.bytedeco.tensorflowlite.BuiltinOpResolver
import org.bytedeco.tensorflowlite.FlatBufferModel
import org.bytedeco.tensorflowlite.Interpreter
import org.bytedeco.tensorflowlite.InterpreterBuilder
import org.bytedeco.tensorflowlite.global.tensorflowlite
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorType
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.tint
import org.openrndr.extensions.Screenshots
import org.openrndr.math.Vector2
import org.openrndr.math.map
import java.io.File
import kotlin.math.sqrt

val res = 1

fun main() = application {
    configure {
        width = 1000 * res
        height = 1000 * res
        hideWindowDecorations = true
    }
    program {

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

        fun outputNormalizedValues(pos: Vector2): List<Double> {
            println(model)
            val inputTensor = interpreter.typed_input_tensor_float(0)
            inputTensor.put(0, pos[0].toFloat())
            inputTensor.put(1, pos[1].toFloat())

            TFLITE_MINIMAL_CHECK(interpreter.Invoke() == tensorflowlite.kTfLiteOk)

            val outputTensor = interpreter.typed_output_tensor_float(0)
            val normalized = (0 until dimensions).map { outputTensor.get(it.toLong()) }

            return normalized.map { it.toDouble() }
        }


        val cb = colorBuffer(width, height, type = ColorType.FLOAT32)
        val shad = cb.shadow
        shad.download()

        val extremes = findExtremes()

        val minX = extremes.first[0]
        val minY = extremes.second[0]
        val maxX = extremes.first[1]
        val maxY = extremes.second[1]

        for(unmappedX in 0 until width) {
            for(unmappedY in 0 until height) {

                val x = map(0.0, width * 1.0, minX, maxX, unmappedX.toDouble())
                val y = map(0.0, height * 1.0, minY, maxY, unmappedY.toDouble())
                val e = 0.01

                val center = outputNormalizedValues(Vector2(x * 1.0, y * 1.0))
                val north = outputNormalizedValues(Vector2(x * 1.0, (y - e) * 1.0))
                val east = outputNormalizedValues(Vector2((x + e) * 1.0, y * 1.0))
                val south = outputNormalizedValues(Vector2(x * 1.0, (y + e) * 1.0))
                val west = outputNormalizedValues(Vector2((x - e) * 1.0, y * 1.0))

                val lapl = center.times(4).minus(west).minus(east).minus(south).minus(north)

                var sq = sqrt(lapl.sumOf { it * it })
                shad[unmappedX, unmappedY] = ColorRGBa(sq, sq, sq)

                println("x $x - y $y - sq $sq")

            }
        }
        shad.upload()

        extend(Screenshots()) {
            name = "latentSpace.png"
            contentScale = 4.0
        }

        extend {

            drawer.drawStyle.colorMatrix = tint(ColorRGBa.WHITE.shade(mouse.position.y / 50))

            drawer.image(cb)

        }
    }
}

fun List<Double>.times(multiplier: Int): List<Double> {
    return this.map { it * multiplier }
}


fun List<Double>.minus(other: List<Double>): List<Double> {
    return this.zip(other).map { it.first - it.second }
}