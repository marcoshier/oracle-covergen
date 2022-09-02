package fillin

import java.io.File

fun main() {

    val latentProtovisuals = File("offline-data/resolved/proto-latent.csv").readLines().map {
        it.split(",").map { it.toDouble() }.toDoubleArray()
    }

    File("offline-data/resolved/cover-latent.csv").bufferedWriter().use { writer ->
        File("offline-data/resolved/prototype-weights.csv").reader().forEachLine {
            val weights = it.split(",").map { it.toDouble() }.toDoubleArray()
            var w = 0.0
            var sx = 0.0
            var sy = 0.0
            for (i in 0 until weights.size) {
                sx += weights[i] * latentProtovisuals[i][0]
                sy += weights[i] * latentProtovisuals[i][1]
                w += weights[i]
            }
            sx /= w
            sy /= w
            if (sx != sx || sy != sy) {
                sx = 0.0
                sy = 0.0
            }
            //writer.newprintln("$sx,$sy")
            writer.write("$sx,$sy")
            writer.newLine()
        }
    }
}