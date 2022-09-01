package tools

import com.google.gson.Gson
import org.openrndr.math.map
import org.openrndr.math.mix
import java.io.File

fun main() {
    val jsons = File("data/new/parameters/").listFiles().filter { it.isFile && it.extension == "json" }.toMutableList()

    for(j in 0..50) {

        val r = (Math.random() * jsons.size).toInt()
        val r2 = (Math.random() * jsons.size).toInt()

        val a = jsons[r]
        val b = jsons[r2]

        interpolate(a, b)
    }
}



fun interpolate(a: File, b: File) {

    val folderName = "${a.nameWithoutExtension}_${b.nameWithoutExtension}"
    File("data/new/interpolated/$folderName").mkdirs()

    val aMap = jsonToNormalizedVector(a.readText())
    val bMap = jsonToNormalizedVector(b.readText())

    println(aMap)

    for(j in 1 .. 30) {
        val interpolated = (aMap zip bMap).map {(first, second) ->
            mix(first, second, j / 30.0)
        }

        val interpolatedJson = Gson().toJson(normalizedVectorToMap(a.readText(), interpolated))

        File("data/new/interpolated/$folderName/", "$j.json").writeText(interpolatedJson)
    }
}