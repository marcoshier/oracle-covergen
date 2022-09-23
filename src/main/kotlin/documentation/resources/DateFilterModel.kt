package documentation.resources

import org.openrndr.animatable.Animatable
import org.openrndr.animatable.easing.Easing
import org.openrndr.events.Event
import java.lang.Double.max
import java.lang.Double.min
import javax.swing.JSpinner.DateEditor

class DateState(year: Double): Animatable() {
    val stateChanged = Event<Unit>()

    var animatedYear = year
    var year = year
        set(value) {
            if (value != field) {


                cancel()
                if (value > field)
                    animatedYear = value-1.0
                if (value < field) {
                    animatedYear = value+1.0
                }
                ::animatedYear.animate(value, 500, Easing.QuadInOut)

                field = value

                stateChanged.trigger(Unit)
            }
        }


}

class DateFilterModel(val articleYears: List<Float>) {

    val filterChanged = Event<Unit>()

    val states:List<DateState> = listOf(DateState(1880.0), DateState(2022.0))

    init {
        states.forEach {
            it.stateChanged.listen {
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
        val low = min(states[0].year, states[1].year)
        val high = max(states[0].year, states[1].year)
        return articleYears[pointIndex] in (low .. high)
    }

    fun range(): List<Double> {
        val low = min(states[0].year, states[1].year)
        val high = max(states[0].year, states[1].year)
        return listOf(low, high)
    }

    fun reset() {
        states[0].year = 1880.0
        states[1].year = 2022.0
    }

}