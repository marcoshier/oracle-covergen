package tools

import com.github.doyaaaaaken.kotlincsv.client.CsvReader
import com.google.gson.Gson
import java.io.File

fun main() {

    val json = File("data/new/parameters/3A0b1144f0-757f-421f-bc35-5f080841a3ab.json")
    val csvFiles = File("data/xy-parameters/").walk().filter { it.isFile }.toList()

    for(file in csvFiles) {

        val reader = CsvReader().readAll(file)[0].map { it.toDouble() }
        val n = normalizedVectorToMap(json.readText(), reader)


        File("data/xyNew/", "${file.nameWithoutExtension}.json").writeText(Gson().toJson(n))

    }


}