package tools

import java.io.File

fun main() {
    val output = File("cover-data.csv")
    val writer = output.bufferedWriter()
    val files = File("offline-data/cover-jsons").listFiles().filter { !it.isHidden && it.extension == "json" }
    for (file in files) {
        val v = jsonToNormalizedVector(file.readText())
        println(v.size)
        writer.write(v.joinToString(","))
        writer.newLine()
    }
    writer.close()

}