package fillin

import org.bytedeco.javacpp.Pointer
import org.bytedeco.tensorflowlite.BuiltinOpResolver
import org.bytedeco.tensorflowlite.FlatBufferModel
import org.bytedeco.tensorflowlite.Interpreter
import org.bytedeco.tensorflowlite.InterpreterBuilder
import org.bytedeco.tensorflowlite.global.tensorflowlite.kTfLiteOk
import java.io.File


fun main() {
    val model: FlatBufferModel = FlatBufferModel.BuildFromFile("data/models/decoder.tflite")
    TFLITE_MINIMAL_CHECK(model != null && !model.isNull())
    val resolver = BuiltinOpResolver()
    val builder = InterpreterBuilder(model, resolver)
    val interpreter = Interpreter(null as Pointer?)
    builder.apply(interpreter)
    TFLITE_MINIMAL_CHECK(interpreter != null && !interpreter.isNull())

    TFLITE_MINIMAL_CHECK(interpreter.AllocateTensors() == kTfLiteOk);
    System.out.println("=== Pre-invoke Interpreter State ===");
    //PrintInterpreterState(interpreter);

    File("offline-data/resolved/cover-normalized.csv").bufferedWriter().use { writer ->

        File("offline-data/resolved/cover-latent.csv").reader().forEachLine {

            val values = it.split(",").map { it.toDouble() }

            val inputTensor = interpreter.typed_input_tensor_float(0)
            inputTensor.put(0, values[0].toFloat())
            inputTensor.put(1, values[1].toFloat())

            TFLITE_MINIMAL_CHECK(interpreter.Invoke() == kTfLiteOk);

            val outputTensor = interpreter.typed_output_tensor_float(0)
            val normalized = (0 until dimensions).map { outputTensor.get(it.toLong()) }
            for (i in 0 until dimensions) {
                val v = outputTensor.get(i.toLong())
            }
            writer.write(normalized.joinToString(","))
            writer.newLine()

        }
    }

}