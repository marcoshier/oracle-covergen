package components

import org.openrndr.animatable.Animatable
import org.openrndr.draw.Drawer
import org.openrndr.events.Event

class IdleState(val timeBeforeIdle: Double): Animatable() {

    val idleModeStarted = Event<Unit>()
    val idleModeEnded = Event<Unit>()
    var timer = 0.0

    fun enterIdle() {
        idleModeStarted.trigger(Unit)
    }

    fun exitIdle() {
        idleModeEnded.trigger(Unit)
    }

    fun startTimer() {
        timer = 0.0
        cancel()
        ::timer.animate(1.0, timeBeforeIdle.toLong() * 1000).completed.listen {
            enterIdle()
        }
    }

    fun update() {
        updateAnimation()
    }

}