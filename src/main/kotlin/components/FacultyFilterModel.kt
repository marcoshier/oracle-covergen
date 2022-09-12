package components

import org.openrndr.animatable.Animatable
import org.openrndr.animatable.easing.Easing

class FilterState() : Animatable() {

    var visible: Boolean = true
    set(value) {
        if (value != field) {
            field = value

            cancel()
            if (value) {
                ::fade.animate(1.0, 100, Easing.QuadInOut)
            }

            if (!value) {
                ::fade.animate(0.0, 100, Easing.QuadInOut)
            }
        }
    }

    var fade: Double = 1.0
}

class FacultyFilterModel {
    val states = (0 until 8).map { FilterState() }



    fun update() {
        for (state in states) {
            state.updateAnimation()
        }

    }
}