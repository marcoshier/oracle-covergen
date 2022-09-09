package components

import org.openrndr.animatable.Animatable
import org.openrndr.animatable.easing.Easing
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.isolated

class SelectorWidget(val drawer: Drawer) : Animatable() {
    var opacity = 1.0

    var radius = 200.0


    fun fadeIn() {
        this::opacity.animate(1.0, 500, Easing.CubicInOut)
    }

    fun fadeOut() {
        this::opacity.animate(0.0, 500, Easing.CubicInOut)
    }

    fun draw() {
        updateAnimation()
        drawer.isolated {
            defaults()
            drawer.fill = null
            drawer.stroke = ColorRGBa.WHITE.opacify(opacity)
            drawer.strokeWeight = 3.0
            drawer.circle(drawer.bounds.center, radius)
        }
    }
}