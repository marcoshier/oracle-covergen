package components

import org.openrndr.animatable.Animatable
import org.openrndr.animatable.easing.Easing
import org.openrndr.color.ColorRGBa
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

    val facultyNames = listOf(
        "Architecture and The Built Environment",
        "Aerospace Engineering",
        "Applied Sciences",
        "Civil Engineering and Geosciences",
        "Electrical Engineering, Mathematics and Computer Science",
        "Industrial Design Engineering",
        "Mechanical, Maritime and Materials Engineering",
        "Technology, Policy and Management"
    )
    var facultyColors = listOf(
        ColorRGBa.fromHex("2D5BFF"),
        ColorRGBa.fromHex("A5A5A5"),
        ColorRGBa.fromHex("C197FB"),
        ColorRGBa.fromHex("E1A400"),
        ColorRGBa.fromHex("19CC78"),
        ColorRGBa.fromHex("00A8B4"),
        ColorRGBa.fromHex("E54949"),
        ColorRGBa.fromHex("FFAD8F")
    )
    var facultyList = facultyNames zip facultyColors

    val states:List<FilterState> = facultyNames.map { FilterState() }

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