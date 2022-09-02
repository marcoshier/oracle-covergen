package tools

import fillin.protoJsonPath
import java.io.File

fun main() {
    val output = File("offline-data/resolved/proto-normalized.csv")
    val writer = output.bufferedWriter()
    val files = File(protoJsonPath).listFiles().filter { !it.isHidden && it.extension == "json" }
    for (file in files) {
        val v = jsonToNormalizedVector(file.readText())
        println(v.size)
        writer.write(v.joinToString(","))
        writer.newLine()
    }
    println("${files.size} files processed")
    writer.close()

}