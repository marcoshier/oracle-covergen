package components

import org.openrndr.animatable.Animatable
import org.openrndr.animatable.easing.Easing
import org.openrndr.events.Event

class FilterState(val facultyName: String) : Animatable() {

    val stateChanged = Event<Unit>()
    var visible: Boolean = true
    set(value) {
        println(facultyName)
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

    var facultyList = dataModel.facultyToColor
    val states:List<FilterState> = facultyList.map { FilterState(it.first) }

    init {
        states.forEach {
            it.stateChanged.listen {
                if (states.none { it.visible }) {
                    states.forEach {
                        it.visible = true
                    }
                }
            }
        }
    }

    fun update() {
        for (state in states) {
            state.updateAnimation()
        }
    }
}