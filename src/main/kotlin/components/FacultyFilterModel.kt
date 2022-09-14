package components

import org.openrndr.animatable.Animatable
import org.openrndr.animatable.easing.Easing
import org.openrndr.events.Event

class FilterState(val facultyName: String) : Animatable() {

    val stateChanged = Event<Unit>()
    var visible: Boolean = true
    set(value) {
        if (value != field) {
            field = value


            cancel()
            if (value) {
                ::fade.animate(1.0, 500, Easing.QuadInOut)
            }

            if (!value) {
                ::fade.animate(0.0, 500, Easing.QuadInOut)
            }
            stateChanged.trigger(Unit)
        }
    }

    var fade: Double = 1.0
}

class FacultyFilterModel(dataModel: DataModel) {

    val facultyList = dataModel.facultyToColor
    val articleFaculties = dataModel.facultyIndexes

    val states:List<FilterState> = facultyList.map { FilterState(it.first) }

    val filterChanged = Event<Unit>()

    init {
        states.forEach {
            it.stateChanged.listen {
                if (states.none { it.visible }) {
                    states.forEach {
                        it.visible = true
                    }
                }
                filterChanged.trigger(Unit)
            }
        }
    }

    fun update() {
        for (state in states) {
            state.updateAnimation()
        }
    }

    fun filter(pointIndex: Int) : Boolean {
        val visible = (0 until 8).filter { states[it].visible }
        return articleFaculties[pointIndex] in visible
    }
}