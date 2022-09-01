package fillin

import org.bytedeco.javacpp.Pointer
import org.bytedeco.tensorflowlite.BuiltinOpResolver
import org.bytedeco.tensorflowlite.FlatBufferModel
import org.bytedeco.tensorflowlite.Interpreter
import org.bytedeco.tensorflowlite.InterpreterBuilder
import org.bytedeco.tensorflowlite.global.tensorflowlite.PrintInterpreterState
import org.bytedeco.tensorflowlite.global.tensorflowlite.kTfLiteOk


fun TFLITE_MINIMAL_CHECK(x: Boolean) {
    if (!x) {
        System.err.print("Error at ")
        Thread.dumpStack()
        System.exit(1)
    }
}

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

    val inputTensor = interpreter.typed_input_tensor_float(0)

    inputTensor.put(0, 0.5f)
    inputTensor.put(1, 0.5f)

    TFLITE_MINIMAL_CHECK(interpreter.Invoke() == kTfLiteOk);

    val outputTensor = interpreter.typed_output_tensor_float(0)
    for (i in 0 until 45) {
        val v = outputTensor.get(i.toLong())
        println(v)
    }

}