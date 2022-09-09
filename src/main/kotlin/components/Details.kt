package components

import classes.Entry
import com.google.gson.Gson
import org.openrndr.animatable.Animatable
import org.openrndr.animatable.easing.Easing
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.imageFit.imageFit
import org.openrndr.internal.ColorBufferLoader
import org.openrndr.internal.colorBufferLoader
import org.openrndr.shape.Rectangle
import textSandbox.Coverlay
import textSandbox.Section
import java.io.File
import java.io.FileReader
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



    inner class Cover : Animatable() {
        var width = 0.0
        var height = 0.0
        var x = 40.0
        var y = 40.0
        var removing = false
        var dead = false
        var dummy = 0.0
        var proxy: ColorBufferProxy? = null
        var coverlay: Coverlay? = null
        var mainCoverZoom = 0.0

        fun reveal(data: List<String>) {
            ::mainCoverZoom.cancel()
            ::mainCoverZoom.animate(1.0, 1400, Easing.QuadInOut).completed.listen {
                val initialFrame = drawer.bounds.offsetEdges(-80.0)
                coverlay = Coverlay(initialFrame, data).apply {
                    subdivide(Section(initialFrame))
                    unfold()
                }
            }
        }
    }

    val covers = mutableMapOf<Int, Cover>()

    fun updateActive(oldPoints: List<Int>, newPoints: List<Int>) {

        println("oldPoints: ${oldPoints.size}, newPoints: ${newPoints.size}")

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
                        c.coverlay = null
                        c.removing = false
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
                cover::y.complete()
            }


        }

        for (i in added) {

            val cover = covers.getOrPut(i) { Cover() }
            cover.proxy = colorBufferLoader.loadFromUrl("file:offline-data/generated/png/${skipPoints + i}.png")

            cover.dead = false
            cover.removing = false

            cover.proxy!!.events.loaded.listen {
                cover.width = 50.0
                cover.apply {
                    cover::height.cancel()
                    cover::height.animate(50.0, 500, Easing.CubicInOut)
                }
            }

        }

        // update main cover
        if(newPoints.isNotEmpty() && oldPoints.isNotEmpty()) {
            val mainCover = covers[newPoints[0]]
            val data = data[newPoints[0]]

            if(oldPoints[0] != newPoints[0]) {
                if(mainCover!!.coverlay != null) {
                    mainCover.apply {
                        ::mainCoverZoom.animate(0.0, 1400, Easing.QuadInOut).completed.listen {
                            mainCover.coverlay = null
                            mainCover.reveal(data)
                        }
                    }
                } else {
                    mainCover.reveal(data)
                }
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


        for (cover in covers.values.map { it }) {
            cover.proxy!!.events.loaded.deliver()
            cover.updateAnimation()
        }


        drawer.isolated {

            drawer.defaults()
            drawer.fill = ColorRGBa.GREEN
            drawer.fontMap = font

            for (cover in covers.values) {
                val rect = Rectangle(cover.x - cover.width / 2.0, cover.y, cover.width, cover.height).scaledBy(5.0 * cover.mainCoverZoom)
                drawer.rectangle(rect)


                val cb = cover.proxy?.colorBuffer
                if(cb != null) {
                    drawer.imageFit(cb, rect)
                }

                if(cover.coverlay != null) {
                    cover.coverlay!!.draw(drawer, cb)
                }
            }

//            drawer.text("hallo dan?", 40.0, 40.0)
//            for ((index, i) in this@Details.model.activePoints.withIndex()) {
//                drawer.text("$i", 40.0, 40.0 + index * 20.0)
//            }
        }
    }
}
