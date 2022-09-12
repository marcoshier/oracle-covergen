package components

import org.openrndr.animatable.Animatable
import org.openrndr.animatable.AnimationEvent
import org.openrndr.animatable.PropertyAnimationKey
import org.openrndr.animatable.easing.Easing
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.imageFit.imageFit
import org.openrndr.internal.colorBufferLoader
import org.openrndr.shape.Rectangle
import org.w3c.dom.css.Rect
import textSandbox.Coverlay
import textSandbox.Section
import kotlin.math.abs


class Details(val drawer: Drawer, val data: List<List<String>>) {

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


    inner class SimpleCover : Animatable() {
        var width = 0.0
        var height = 0.0
        var x = 40.0
        var y = 40.0
        var removing = false
        var dead = false
        var dummy = 0.0
        var proxy: ColorBufferProxy? = null
        var image: ColorBuffer? = null
        var zoom = 0.0

        fun zoomIn() {
            ::zoom.animate(1.0, 800, Easing.CubicInOut)
        }

        fun zoomOut(): PropertyAnimationKey<Double> {
            val duration = if(zoom != 0.0) 1000L else 0
            println("ZOOM OUT $duration")
            return ::zoom.animate(0.0, duration, Easing.CubicInOut)
        }
    }
    inner class MainCover: Animatable() {

        var coverlay: Coverlay? = null
        var frame = Rectangle(100.0, 100.0, 1080.0 - 200.0, 1920.0 - 200.0)
        var opacity = 0.0
        var dummy = 0.0

        private fun reveal(cover: SimpleCover): PropertyAnimationKey<Double> {
            cover.zoomIn()
            return ::opacity.animate(1.0, 1200, predelayInMs = 800)
        }

        private fun unreveal(): PropertyAnimationKey<Double> {
            val duration = if(coverlay != null) 1000L else 0
            if(coverlay != null) {
                coverlay!!.fold()
            }
            return ::opacity.animate(0.0, duration)
        }

        var index = -1
            set(value) {
                dummy = 0.0
                println("canceling")
                cancel()
                ::dummy.animate(1.0, 500).completed.listen {
                    if (value != -1 && field != value) {
                        println("unrevealing 1")
                        unreveal().completed.listen {
                            val check = if(field == - 1) value else field
                            require(check != -1)
                            covers[check]!!.zoomOut().completed.listen {
                                field = value
                                val coverlayData = data[field].filter { it != "" }.plus(field.toString())
                                coverlay = Coverlay(drawer, covers[field]!!.image, coverlayData).apply {
                                    subdivide(Section(frame))
                                }

                                println("revealing 1")
                                reveal(covers[field]!!).completed.listen {
                                    println("revealed 1")
                                    coverlay!!.unfold()
                                }
                            }
                        }
                    } else if (value == -1 && coverlay != null) {
                        println("unrevealing 2")
                        unreveal().completed.listen {
                            println("unrevealed 2")
                            covers[field]!!.zoomOut()
                            coverlay = null
                            dummy = 0.0
                        }
                    }
                }

            }
    }


    val covers = mutableMapOf<Int, SimpleCover>()
    val mainCover = MainCover()

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

            val simpleCover = covers.getOrPut(i) { SimpleCover() }
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

            val simpleCover = covers.getOrPut(i) { SimpleCover() }
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

        if(newPoints.isNotEmpty()) {
            mainCover.index = newPoints[0]
        } else {
            mainCover.index = -1
        }

        covers.values.removeIf { it.dead }
    }


    val font = loadFont("data/fonts/IBMPlexSans-Medium.otf", 24.0)

    fun draw() {
        fade.updateAnimation()
        mainCover.updateAnimation()

        if (fade.opacity < 0.5) {
            return
        }

        for (cover in covers.values.map { it }) {
            cover.proxy!!.events.loaded.deliver()
            cover.updateAnimation()
        }


        drawer.isolated {

            drawer.defaults()
            drawer.fill = ColorRGBa.GREEN
            drawer.fontMap = font

            for (cover in covers.values) {
                val minimizedRect = Rectangle(cover.x - cover.width / 2.0, cover.y, cover.width, cover.height)
                val dynamicRect =  mainCover.frame * cover.zoom + minimizedRect * (1.0 - cover.zoom)
                drawer.rectangle(dynamicRect)

                val cb = cover.proxy?.colorBuffer
                if(cb != null) {
                    drawer.imageFit(cb, dynamicRect)
                }
            }

            if(mainCover.coverlay != null) {
                mainCover.coverlay!!.draw(mainCover.opacity)
            }


//            drawer.text("hallo dan?", 40.0, 40.0)
//            for ((index, i) in this@Details.model.activePoints.withIndex()) {
//                drawer.text("$i", 40.0, 40.0 + index * 20.0)
//            }
        }
    }
}
