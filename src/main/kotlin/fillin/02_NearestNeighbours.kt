package fillin

import java.io.File
import kotlin.math.acos
import kotlin.math.exp
import kotlin.math.sqrt

/**
 * For every node in the embedding we find the nearest protovisual nodes
 */

fun distance(a: DoubleArray, b: DoubleArray): Double {
    require(a.size == b.size)
    var sum = 0.0
    for (i in 0 until a.size) {
        val d = a[i] * b[i]
        sum += d
    }
    return acos(sum) / Math.PI

}

fun DoubleArray.normalize() {

    var sum = 0.0
    for (i in 0 until size) {
        val d = this[i]
        sum += d * d
    }
    var l = sqrt(sum)
    for (i in 0 until size) {
        this[i] /= l
    }
}


fun main() {

    val embedding = mutableListOf<DoubleArray>()

    var index = 0
    File("offline-data/graph/graph-embedding-i-100-v2.csv").reader().forEachLine {
        if (index > 0) {
            embedding.add(it.split(",").asSequence().drop(1).map { it.toDouble() }.toList().toDoubleArray())
        }
        index++
    }


    val prototypeIndices = File("offline-data/resolved/prototypes.csv").readLines().map {
        it.toInt()
    }

    embedding.forEach(DoubleArray::normalize)
    println(embedding.size)

    val prototypes = prototypeIndices.map { embedding[it] }
    println(prototypes.size)

    println("finding distances")
    File("offline-data/resolved/prototype-distances.csv").bufferedWriter().use { writer ->
        embedding.forEach { e ->
            val line = prototypes.map { p -> exp(-distance(e, p)*10.0) }.toList().joinToString(", ")

            writer.write(line)
            writer.newLine()

        }
    }
    println("done")

}