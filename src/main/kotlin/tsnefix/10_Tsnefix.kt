package tsnefix

import classes.Entry
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.google.gson.Gson
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.openrndr.math.Polar
import org.openrndr.math.Vector2
import java.io.File
import java.io.FileReader
import java.nio.file.Paths
import kotlin.io.path.bufferedReader

fun main() {
    val entries = Gson().fromJson(FileReader(File("offline-data/graph/mapped-v2r1.json")),Array<Entry>::class.java).toList()

    val points = csvReader().readAllWithHeader(File("offline-data/graph/graph-tsne-d-100-i-100-p25-v2.csv")).toList().map {
        Vector2(it["x"]!!.toDouble(), it["y"]!!.toDouble())
    }

    val allText = Paths.get("offline-data/faculty-corrections.csv").bufferedReader()
    val corrections = CSVParser(allText, CSVFormat.newFormat(';')).associate {
        Pair(it.get(0), it.get(1))
    }

    require(points.size == entries.size) {
        println("${entries.size} ${points.size}")
    }

    val facultyNames = listOf(
        "Architecture and the Built Environment",
        "Aerospace Engineering (AE)",
        "Applied Sciences (AS)",
        "Civil Engineering and Geosciences (CEG)",
        "Electrical Engineering, Mathematics & Computer Science (EEMCS)",
        "Industrial Design Engineering (IDE)",
        "Mechanical, Maritime and Materials Engineering (3mE)",
        "Technology, Policy and Management (TPM)"
    )

    val outFile = File("corrected.csv")
    val outWriter = outFile.bufferedWriter()

    outWriter.write("x,y")
    outWriter.newLine()
    var adjusted = 0
    for (i in points.indices) {
        val entry = entries[i]
        val point = points[i]

        val faculty = corrections[(entry.ogdata["Faculty"] as? String)?.lowercase()]

        val index = facultyNames.indexOf(faculty)


        var fp =
        if (index != -1) {
            adjusted++
            point + Polar(index * (360.0 / 8.0), 15.0).cartesian

        } else {
            point
        }
        outWriter.write("${fp.x},${fp.y}")
        outWriter.newLine()
    }
    outWriter.close()

    println("adjusted $adjusted")
}