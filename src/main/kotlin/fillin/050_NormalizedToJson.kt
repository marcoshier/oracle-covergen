 package fillin

import com.google.gson.Gson
import tools.normalizedVectorToMap
import java.io.File


fun main() {
    File("offline-data/resolved/json").mkdirs()

    val templateJson = File("data/new-protovisuals/parameters/3A0e9e3202-5671-4bfe-bf87-4d4abd619438.json").readText()
    var line = 0
    File("offline-data/resolved/cover-normalized.csv").reader().forEachLine {
        val values = it.split(",").map { it.toDouble() }
        val map = normalizedVectorToMap(templateJson, values)
        println(map)
        //File("offline-data/resolved/json/${String.format("%06d", line)}.json").writeText(Gson().toJson(map))
        line++
    }
}