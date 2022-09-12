package components

import org.openrndr.animatable.Animatable
import org.openrndr.animatable.easing.Easing
import org.openrndr.events.Event

class FilterState() : Animatable() {

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

class FacultyFilterModel {
    val states:List<FilterState> = (0 until 8).map { FilterState() }


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