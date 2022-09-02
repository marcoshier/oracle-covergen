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
    return acos(sum) / (Math.PI*0.5)

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


    val prototypeIndices = File("offline-data/resolved/proto-row-idx.csv").readLines().map {
        it.toInt()
    }

    embedding.forEach(DoubleArray::normalize)
    println(embedding.size)

    val prototypes = prototypeIndices.map { embedding[it] }
    println(prototypes.size)

    println("finding distances")
    File("offline-data/resolved/prototype-weights.csv").bufferedWriter().use { writer ->
        embedding.forEach { e ->
            val weights = prototypes.map { p -> exp(-distance(e, p)*1.0) }
            val sorted = prototypes.map { p -> exp(-distance(e, p)*1.0) }.sortedDescending()
            val t = sorted[2]

            val line = prototypes.mapIndexed { index, p -> weights[index].let { if (it > t) it else 0.0 } }.toList().joinToString(", ")

            writer.write(line)
            writer.newLine()

        }
    }
    println("done")

}