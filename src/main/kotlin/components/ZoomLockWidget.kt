package components

import org.openrndr.MouseEvent
import org.openrndr.animatable.Animatable
import org.openrndr.animatable.easing.Easing
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.isolated
import org.openrndr.events.Event
import org.openrndr.math.Vector2

class ZoomLockWidget(val drawer: Drawer) : Animatable() {

    var fade = 0.0
    var zoomUnlockRequested = Event<Unit>()


    fun fadeIn() {
        cancel()
        ::fade.animate(1.0, 500, Easing.QuadInOut)
    }

    fun fadeOut() {
        cancel()
        ::fade.animate(0.0, 500, Easing.QuadInOut)

    }

    fun zoomUnlock() {
        fadeOut()
        zoomUnlockRequested.trigger(Unit)
    }

    fun buttonDown(event: MouseEvent) {
        if (fade > 0.0) {
            if (event.position.distanceTo(Vector2(2880/2.0, 1920.0/2.0)) < 100.0) {
                zoomUnlock()
            }
        }
    }

    fun draw() {
        updateAnimation()
        if (fade > 0.0) {
            drawer.isolated {
                drawer.defaults()
                drawer.fill = ColorRGBa.WHITE.opacify(fade)
                drawer.stroke = null
                drawer.circle(drawer.bounds.center, 30.0)
            }
        }
    }
}