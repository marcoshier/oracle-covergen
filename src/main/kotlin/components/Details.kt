package components

import org.openrndr.animatable.Animatable
import org.openrndr.animatable.easing.Easing
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.isolated
import org.openrndr.draw.loadFont
import kotlin.math.abs


class Details(val drawer: Drawer, val model: DataModel) {
    class Fade : Animatable() {
        var opacity = 0.0
    }
    val fade = Fade()

    fun fadeIn() {
        fade.apply {
            ::opacity.animate(1.0, 500, Easing.CubicInOut)
        }
    }

    fun fadeOut() {
        fade.apply {
            ::opacity.animate(0.0, 500, Easing.CubicInOut)
        }
    }

    class Cover : Animatable() {
        var width = 0.0
        var height = 0.0
        var x = 40.0
        var y = 40.0
        var removing = false
        var dead = false
        var dummy = 0.0
    }

    val covers = mutableMapOf<Int, Cover>()

    fun updateActive(oldPoints: Set<Int>, newPoints: Set<Int>) {


        println("oldPoints: ${oldPoints.size}, newPoints: ${newPoints.size}")

        val removed = oldPoints subtract newPoints
        val added = newPoints subtract oldPoints


        println("removing ${removed.size} covers")
        println("adding ${added.size} covers")

        for (i in removed) {
            covers[i]?.let { c ->

                c.dead = false
                //c.removing = true


                c.apply {
                    c::width.cancel()
                    c::height.cancel()
                    c::dummy.cancel()
                    //c::width.animate(0.0, 1500, Easing.CubicInOut)
                    c::height.animate(0.0, 1500, Easing.CubicInOut)
                    c::dummy.animate(1.0, 1500).completed.listen {
                        c.dead = true
                    }
                }
            }
        }
        for ((index, i) in newPoints.withIndex()) {
            val ax = (index % 10) * 60.0 + 40.0
            val ay = (index / 10) * 60.0 + 40.0
            val cover = covers.getOrPut(i) { Cover() }
            cover.dead = false

                cover.apply {
                    cover::x.cancel()
                    cover::y.cancel()
                    val d = if (i in added) 1.0 else 1.0
                    val dx = (abs(ax - cover.x) * d).toLong()
                    val dy = (abs(ay - cover.y) * d).toLong()

                    cover::x.animate(ax, dx, Easing.QuadInOut)
                    cover::x.complete()
                    cover::y.animate(ay, dy, Easing.QuadInOut)
                }


        }

        for (i in added) {
            val cover = covers.getOrPut(i) { Cover() }

            cover.dead = false
            cover.removing = false

            cover.width = 50.0
            cover.apply {
                cover::height.cancel()
                cover::height.animate(50.0, 500, Easing.CubicInOut)
            }

        }
        covers.values.removeIf { it.dead }
    }


    val font = loadFont("data/fonts/IBMPlexSans-Medium.otf", 24.0)

    fun draw() {
        fade.updateAnimation()

        if (fade.opacity < 0.5) {
            return
        }


        for (cover in covers.values) {
            cover.updateAnimation()
        }


        drawer.isolated {
            drawer.defaults()
            drawer.fill = ColorRGBa.GREEN
            drawer.fontMap = font

            for (cover in covers.values) {
                drawer.rectangle(cover.x - cover.width / 2.0, cover.y, cover.width, cover.height)
            }

//            drawer.text("hallo dan?", 40.0, 40.0)
//            for ((index, i) in this@Details.model.activePoints.withIndex()) {
//                drawer.text("$i", 40.0, 40.0 + index * 20.0)
//            }
        }
    }
}
