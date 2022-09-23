package documentation.resources

import classes.Entry
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.google.gson.Gson
import components.DateFilterModel
import components.FacultyFilterModel
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.openrndr.animatable.Animatable
import org.openrndr.color.ColorRGBa
import org.openrndr.events.Event
import org.openrndr.extra.kdtree.kdTree
import org.openrndr.math.Spherical
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.shape.Rectangle
import org.openrndr.shape.bounds
import org.openrndr.shape.map
import java.io.File
import java.io.FileReader
import java.nio.file.Paths
import kotlin.io.path.bufferedReader

class ActivePointsChangedEvent(val oldPoints: List<Int>, val newPoints: List<Int>)

class HeroPointChangedEvent(val oldPoint: Int?, val newPoint: Int?)

const val skipPoints = 142082

class ArticleData(val title: String, val author:String, val faculty:String, val department: String, val date:String) {


    fun toList():List<String> {
        return listOf(title, author, faculty, department, date)
    }
}

class DataModel: Animatable() {
    lateinit var dateFilter: documentation.resources.DateFilterModel
    lateinit var facultyFilter: documentation.resources.FacultyFilterModel

    private fun loadLatentPoints() : List<Vector2> {
        val pointsData = csvReader().readAllWithHeader(File("offline-data/graph/cover-latent.csv")).drop(skipPoints).map {
            Vector2(it["x"]!!.toDouble(), it["y"]!!.toDouble())
        }
        return pointsData
    }

    private fun loadPoints(): List<Vector3> {
        val pointsData = csvReader().readAllWithHeader(File("offline-data/graph/corrected.csv")).drop(skipPoints).map {
            Vector2(it["x"]!!.toDouble(), it["y"]!!.toDouble())
        }
        val bounds = pointsData.bounds
        val llbounds = Rectangle(-180.0, 0.0, 360.0, 180.0)
        val latlon = pointsData.map { it.map(bounds, llbounds) }


        return latlon.map { Spherical(it.x, it.y, 10.0).cartesian }
    }
    val points = loadPoints()
    val latentPoints = loadLatentPoints()
    private var muted = false

    private fun loadArticleData(): List<ArticleData> {
        val entries = Gson().fromJson(FileReader(File("offline-data/graph/mapped-v2r1.json")), Array<Entry>::class.java)
            .drop(skipPoints).map {
            ArticleData(
                it.ogdata["Title"] as String,
                it.ogdata["Author"] as String,
                it.ogdata["Faculty"] as String,
                it.ogdata["Department"] as String,
                it.ogdata["Date"] as String
            )
        }
        require(points.size == entries.size)
        return entries
    }
    val data = loadArticleData()

    val facultyNames = listOf(
        "Architecture and the Built Environment (ABE)",
        "Aerospace Engineering (AE)",
        "Applied Sciences (AS)",
        "Civil Engineering and Geosciences (CEG)",
        "Electrical Engineering, Mathematics & Computer Science (EEMCS)",
        "Industrial Design Engineering (IDE)",
        "Mechanical, Maritime and Materials Engineering (3mE)",
        "Technology, Policy and Management (TPM)",
        "Unknown Faculty (?)"
    )
    var facultyColors = listOf(
        ColorRGBa.fromHex("2D5BFF"),
        ColorRGBa.fromHex("FF9254"),
        ColorRGBa.fromHex("C197FB"),
        ColorRGBa.fromHex("E1A400"),
        ColorRGBa.fromHex("19CC78"),
        ColorRGBa.fromHex("00A8B4"),
        ColorRGBa.fromHex("E54949"),
        ColorRGBa.fromHex("FFAD8F"),
        ColorRGBa.fromHex("A5A5A5")
    )
    var facultyToColor = facultyNames zip facultyColors

    private fun loadFacultyIndexes(): List<Int> {
        val lookUp = mutableMapOf<String, String>() // List<

        val allText = Paths.get("offline-data/faculty-corrections.csv").bufferedReader()
        CSVParser(allText, CSVFormat.newFormat(';')).onEach {
            lookUp[it.get(0)] = it.get(1)
        }

        val correctedFaculties = data.map {
            val correctedFaculty = lookUp[it.faculty.lowercase()]
            correctedFaculty
        }

        val indexes = correctedFaculties.mapIndexed { i, it ->
            if(it != null) {
                facultyNames.indexOf(it)
            } else {
                8
            }
        }

        return indexes
    }
    val facultyIndexes = loadFacultyIndexes()

    val years = data.map {
        val year = it.date.split("-").first()
        if(year.length != 4) {
            0f
        } else {
            year.toFloat()
        }}

    val animatedCoverParams = File("data/xyNew/").walk().filter { it.isFile && it.extension == "json" }.toList()

    /**
     * The kd-tree for the points
     */
    val kdtree = points.kdTree()
    val pointIndices = points.indices.map { Pair(points[it], it) }.associate { it }

    val activePointsChanged = Event<ActivePointsChangedEvent>()
    val heroPointChanged = Event<HeroPointChangedEvent>()

    var lookAt = Vector3(0.0, 0.0, -10.0)
        set(value) {
            if (field != value) {
                activePoints = findActivePoints()
                field = value
            }
        }

    var heroPoint: Int? = null
    set(value) {
        if (field != value) {
            heroPointChanged.trigger(HeroPointChangedEvent(field, value))
            field = value
        }
    }
    var activePoints: List<Int> = emptyList()
        set(value) {
            if (field != value) {
                activePointsChanged.trigger(ActivePointsChangedEvent(field, value))
                heroPoint = value.firstOrNull()
                field = value
            }
        }


    var selectionRadius = 0.24

    fun findActivePoints(): List<Int> {
        require(data.size == points.size)
        return kdtree.findAllInRadius(lookAt, selectionRadius).sortedBy { it.distanceTo(lookAt) }.map {
            pointIndices[it] ?: error("point not found")
        }.filter {
            dateFilter.filter(it) && facultyFilter.filter(it)
        }

    }
}