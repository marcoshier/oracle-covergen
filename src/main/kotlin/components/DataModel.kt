package components

import classes.Entry
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.google.gson.Gson
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

class ActivePointsChangedEvent(val oldPoints: List<Int>, val newPoints: List<Int>)

const val skipPoints = 142082

class ArticleData(val title: String, val author:String, val faculty:String, val department: String, val date:String) {

    fun toList():List<String> {
        return listOf(title, author, faculty, department, date)
    }
}

class DataModel {

    private fun loadPoints(): List<Vector3> {
        val pointsData = csvReader().readAllWithHeader(File("offline-data/graph/graph-tsne-d-100-i-100-p25-v2.csv")).drop(skipPoints).map {
            Vector2(it["x"]!!.toDouble(), it["y"]!!.toDouble())
        }
        val bounds = pointsData.bounds
        val llbounds = Rectangle(-180.0, 0.0, 360.0, 180.0)
        val latlon = pointsData.map { it.map(bounds, llbounds) }

        return latlon.map { Spherical(it.x, it.y, 10.0).cartesian }
    }
    val points = loadPoints()


    private fun loadArticleData(): List<ArticleData> {
        val articleData = Gson().fromJson(FileReader(File("offline-data/graph/mapped-v2r1.json")),Array<Entry>::class.java)
        val entries = articleData.drop(skipPoints).map {
            ArticleData(it.ogdata["Title"] as String, it.ogdata["Author"] as String, it.ogdata["Faculty"] as String, it.ogdata["Department"] as String, it.ogdata["Date"] as String)
        }
        println("${entries[0]}")
        return entries
    }
    val data = loadArticleData()



    /**
     * The kd-tree for the points
     */
    val kdtree = points.kdTree()
    val pointIndices = points.indices.map { Pair(points[it], it) }.associate { it }

    val activePointsChanged = Event<ActivePointsChangedEvent>()

    var lookAt = Vector3(0.0, 0.0, -10.0)
        set(value) {
            if (field != value) {
                activePoints = findActivePoints()
                field = value
            }
        }

    var activePoints: List<Int> = emptyList()
        set(value) {
            if (field != value) {
                activePointsChanged.trigger(ActivePointsChangedEvent(field, value))
                field = value
            }
        }

    var selectionRadius = 0.24

    fun findActivePoints(): List<Int> {
        return kdtree.findAllInRadius(lookAt, selectionRadius).sortedBy { it.distanceTo(lookAt) }.map {
            pointIndices[it] ?: error("point not found")
        }
    }
}