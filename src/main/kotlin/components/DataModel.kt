package components

import org.openrndr.events.Event
import org.openrndr.extra.kdtree.kdTree
import org.openrndr.math.Vector3

class ActivePointsChangedEvent(val oldPoints: Set<Int>, val newPoints: Set<Int>)

class DataModel(val points: List<Vector3>) {

    /**
     * The kd-tree for the points
     */
    // this should be a kdtree over Pair<Vector3, Int>, where the Int is the index of the point in the points list
    val kdtree = points.kdTree()

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

    var selectionRadius = 0.2

    fun findActivePoints(): List<Int> {
        return kdtree.findAllInRadius(lookAt, selectionRadius).map {
            points.indexOf(it)
        }.sorted()
    }


}