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
import textSandbox.Coverlay
import textSandbox.Section
import java.io.File
import java.io.FileReader

class ActivePointsChangedEvent(val oldPoints: Set<Int>, val newPoints: Set<Int>)

class DataModel {
    val skipPoints = 142082

    fun createPoints(): List<Vector3> {
        val pointsData = csvReader().readAllWithHeader(File("offline-data/graph/graph-tsne-d-100-i-100-p25-v2.csv")).drop(skipPoints).map {
            Vector2(it["x"]!!.toDouble(), it["y"]!!.toDouble())
        }
        val bounds = pointsData.bounds
        val llbounds = Rectangle(-180.0, 0.0, 360.0, 180.0)
        val latlon = pointsData.map { it.map(bounds, llbounds) }

        return latlon.map { Spherical(it.x, it.y, 10.0).cartesian }
    }
    val points = createPoints()

/*    fun generateOverlays(): List<Coverlay> {
        val articleData = Gson().fromJson(FileReader(File("data/mapped-v2r1.json")),Array<Entry>::class.java)
        val entries = articleData.drop(skipPoints).map {
            listOf(it.ogdata["Title"], it.ogdata["Author"], it.ogdata["Faculty"], it.ogdata["Department"], it.ogdata["Date"]) as List<String>
        }
        return  entries.mapIndexed { i, it ->
            val initialFrame = Rectangle(0.0, 0.0, 540.0, 960.0)
            val c = Coverlay(initialFrame, it).apply {
                val s = Section(initialFrame)
                subdivide(s)
            }
            c
        }
    }
    val overlays = generateOverlays()*/

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
                activePointsChanged.trigger(ActivePointsChangedEvent(field.toSet(), value.toSet()))
                field = value
            }
        }

    var selectionRadius = 0.22

    fun findActivePoints(): List<Int> {
        return kdtree.findAllInRadius(lookAt, selectionRadius).sortedBy { it.distanceTo(lookAt) }.map {
            pointIndices[it] ?: error("point not found")
        }
    }
}