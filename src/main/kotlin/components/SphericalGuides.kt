package components

import org.openrndr.animatable.Animatable
import org.openrndr.animatable.easing.Easing
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.isolated
import org.openrndr.extra.color.presets.DARK_GRAY
import org.openrndr.extra.color.presets.HOT_PINK
import org.openrndr.extra.color.presets.LIGHT_SLATE_GRAY
import org.openrndr.math.Spherical
import org.openrndr.math.Vector3
import org.openrndr.math.transforms.buildTransform
import org.openrndr.shape.Path3D

class SphericalGuides(val drawer: Drawer) : Animatable() {

    var opacity = 0.0

    fun fadeIn() {
        this::opacity.animate(1.0, 500, Easing.CubicInOut)
    }

    fun fadeOut() {
        this::opacity.animate(0.0, 500, Easing.CubicInOut)
    }

    var circles = (1 until 10).map { y->
        Path3D.fromPoints( (0 until 36).map { x->  Spherical(x*10.0, y*18.0, 40.0).cartesian}, closed=true)
    } + (1 until 10).map { y->
        Path3D.fromPoints( (0 until 36).map { x->  Spherical(x*10.0, y*18.0, 40.0).cartesian}, closed=true).transform(
            buildTransform {
                rotate(Vector3.UNIT_X, 90.0)
            }
        )
    }

    fun draw() {
        updateAnimation()
        drawer.isolated {
            for (path in circles) {
                drawer.stroke = ColorRGBa.LIGHT_SLATE_GRAY.shade(opacity)
                drawer.path(path)
            }
        }
    }
}