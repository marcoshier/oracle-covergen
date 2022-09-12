package components

import org.openrndr.animatable.Animatable
import org.openrndr.animatable.PropertyAnimationKey
import org.openrndr.animatable.easing.Easing
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.imageFit.imageFit
import org.openrndr.internal.colorBufferLoader
import org.openrndr.shape.Rectangle
import textSandbox.Coverlay
import textSandbox.Section
import kotlin.math.abs


class Details(val drawer: Drawer, val data: List<ArticleData>) {

    class Fade : Animatable() {
        var opacity = 0.0
        var dummy = 0.0
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


    var coverlayFrame = Rectangle(175.0, 350.0, 1080.0 - 350.0, 1920.0 - 700.0)

    inner class SimpleCover(val index: Int) : Animatable() {
        var width = 0.0
        var height = 0.0
        var x = 40.0
        var y = 40.0
        var removing = false
        var dead = false
        var dummy = 0.0
        var proxy: ColorBufferProxy? = null

        var image: ColorBuffer? = null
        var coverlay: Coverlay? = null
        var coverlayOpacity = 0.0
        var zoom = 0.0

        fun zoomIn() {
            ::zoom.cancel()
            ::coverlayOpacity.cancel()
            ::zoom.animate(1.0, 800, Easing.CubicInOut).completed.listen {

                coverlay = Coverlay(drawer, image, data[index].toList().filter { it != "" }.plus(index.toString())).apply {
                    subdivide(Section(coverlayFrame))
                }

                ::coverlayOpacity.animate(1.0, 1000).completed.listen {
                    if(coverlay != null) {
                        coverlay!!.unfold()
                    }
                }
            }
        }

        fun zoomOut(): PropertyAnimationKey<Double> {
            ::zoom.cancel()
            ::coverlayOpacity.cancel()
            if(coverlay != null) {
                coverlay!!.fold()
                ::coverlayOpacity.animate(0.0, 400).completed.listen {
                    coverlay = null
                }
            }
            return ::zoom.animate(0.0, 800, Easing.CubicInOut, 400)
        }
    }


    val covers = mutableMapOf<Int, SimpleCover>()
    var activeCover: SimpleCover? = null
        set(value) {
            if(field != value) {
                if(field != null && value != null) {
                    println("something $field to start with and something to end with $value")
                    field!!.zoomOut().completed.listen {
                        field = value
                        field!!.zoomIn()
                    }
                } else if (field == null && value != null){
                    println("nothing $field to start with and something to end with  $value")
                    field = value
                    field!!.zoomIn()
                } else if(field != null && value == null) {
                    println("something to start with and nothing to end with")
                    field!!.zoomOut()
                    field = null
                } else {
                    println("nothing to start and nothing to end with")
                }
            }
        }

    fun updateActive(oldPoints: List<Int>, newPoints: List<Int>) {

        //println("oldPoints: ${oldPoints.size}, newPoints: ${newPoints.size}")

        val removed = oldPoints subtract newPoints
        val added = newPoints subtract oldPoints

        for (i in removed) {
            covers[i]?.let { c ->

                c.dead = false
                c.removing = true

                c.apply {
                    c.cancel()
                    c::width.cancel()
                    c::height.cancel()
                    c::dummy.cancel()
                    c::width.animate(0.0, 1500, Easing.CubicInOut)
                    c::height.animate(0.0, 1500, Easing.CubicInOut)
                    c::dummy.animate(1.0, 1500).completed.listen {
                        c.removing = false
                        c.dead = true
                    }
                }
            }
        }

        for ((index, i) in newPoints.withIndex()) {

            val ax = (index % 10) * 60.0 + 40.0
            val ay = (index / 10) * 60.0 + 40.0

            val simpleCover = covers.getOrPut(i) { SimpleCover(index) }
            simpleCover.dead = false

            simpleCover.apply {
                simpleCover::x.cancel()
                simpleCover::y.cancel()
                val d = if (i in added) 1.0 else 1.0
                val dx = (abs(ax - simpleCover.x) * d).toLong()
                val dy = (abs(ay - simpleCover.y) * d).toLong()

                simpleCover::x.animate(ax, dx, Easing.QuadInOut)
                simpleCover::x.complete()
                simpleCover::y.animate(ay, dy, Easing.QuadInOut)
                simpleCover::y.complete()
            }


        }

        for (i in added) {

            val simpleCover = covers.getOrPut(i) { SimpleCover(i) }
            simpleCover.proxy = colorBufferLoader.loadFromUrl("file:offline-data/covers/png/${skipPoints + i}.png")

            simpleCover.dead = false
            simpleCover.removing = false

            simpleCover.proxy!!.events.loaded.listen {
                simpleCover.image = simpleCover.proxy?.colorBuffer
                simpleCover.width = 50.0
                simpleCover.apply {
                    simpleCover::height.cancel()
                    simpleCover::height.animate(50.0, 500, Easing.CubicInOut)
                }
            }

        }

        covers.values.removeIf { it.dead }


        fade.apply {
            dummy = 0.0
            ::dummy.cancel()
            ::dummy.animate(1.0, 500).completed.listen {
                if(newPoints.isNotEmpty()) {
                    activeCover = covers[newPoints[0]]
                    println("newcover exists $activeCover")
                } else {
                    activeCover = null
                    println("no newcovers $activeCover")
                }
            }
        }

    }


    val font = loadFont("data/fonts/IBMPlexSans-Medium.otf", 24.0)

    fun draw() {
        fade.updateAnimation()

        if (fade.opacity < 0.5) {
            return
        }

        for (cover in covers.values) {
            cover.proxy!!.events.loaded.deliver()
            cover.updateAnimation()
        }
        activeCover?.updateAnimation()


        drawer.isolated {

            drawer.defaults()
            drawer.fill = ColorRGBa.GREEN
            drawer.fontMap = font

            for (cover in covers.values) {
                val minimizedRect = Rectangle(cover.x - cover.width / 2.0, cover.y, cover.width, cover.height)
                val dynamicRect =  coverlayFrame * cover.zoom + minimizedRect * (1.0 - cover.zoom)
                drawer.rectangle(dynamicRect)

                val cb = cover.proxy?.colorBuffer
                if(cb != null) {
                    drawer.imageFit(cb, dynamicRect)
                }


                if(cover.coverlay != null) {
                    cover.coverlay!!.draw(cover.coverlayOpacity)
                }
            }



//            drawer.text("hallo dan?", 40.0, 40.0)
//            for ((index, i) in this@Details.model.activePoints.withIndex()) {
//                drawer.text("$i", 40.0, 40.0 + index * 20.0)
//            }
        }
    }
}
