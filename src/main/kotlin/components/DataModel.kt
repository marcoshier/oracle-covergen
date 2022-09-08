package components

import org.openrndr.events.Event
import org.openrndr.extra.kdtree.kdTree
import org.openrndr.math.Vector3

class ActivePointsChangedEvent(val oldPoints: Set<Int>, val newPoints: Set<Int>)

class DataModel(val points: List<Vector3>) {

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
        return kdtree.findAllInRadius(lookAt, selectionRadius).map {
            pointIndices[it] ?: error("point not found")
        }.sorted()
    }
}