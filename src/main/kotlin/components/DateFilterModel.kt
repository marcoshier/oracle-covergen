package components

import org.openrndr.animatable.Animatable
import org.openrndr.animatable.easing.Easing
import org.openrndr.events.Event
import javax.swing.JSpinner.DateEditor

class DateState(year: Double): Animatable() {
    val stateChanged = Event<Unit>()

    var animatedYear = year
    var year = year
        set(value) {
            if (value != field) {
                field = value

                cancel()
                ::animatedYear.animate(year, 500, Easing.QuadInOut)

                stateChanged.trigger(Unit)
            }
        }


}

class DateFilterModel {

    val states:List<DateState> = listOf(DateState(1880.0), DateState(2022.0))

    fun update() {
        for (state in states) {
            state.updateAnimation()
        }
    }
}