package fillin

import com.google.gson.Gson
import org.bytedeco.javacpp.Pointer
import org.bytedeco.tensorflowlite.BuiltinOpResolver
import org.bytedeco.tensorflowlite.FlatBufferModel
import org.bytedeco.tensorflowlite.Interpreter
import org.bytedeco.tensorflowlite.InterpreterBuilder
import org.bytedeco.tensorflowlite.global.tensorflowlite.kTfLiteOk
import tools.normalizedVectorToMap
import java.io.File


fun main() {
    File("offline-data/resolved/json").mkdirs()

    val templateJson = File("offline-data/cover-jsons/3A0b1144f0-757f-421f-bc35-5f080841a3ab.json").readText()
    var line = 0
    File("offline-data/resolved/cover-normalized.csv").reader().forEachLine {
        val values = it.split(",").map { it.toDouble() }
        val map = normalizedVectorToMap(templateJson,values)
        File("offline-data/resolved/json/${String.format("%06d", line)}.json").writeText(Gson().toJson(map))

        line++
    }
}