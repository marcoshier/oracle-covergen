package components

import animatedCover.Coverlay
import animatedCover.Section
import org.openrndr.animatable.Animatable
import org.openrndr.animatable.PropertyAnimationKey
import org.openrndr.animatable.easing.Easing
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.imageFit.imageFit
import org.openrndr.internal.colorBufferLoader
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import sketches.Extendables
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.bounds
import org.openrndr.shape.map



class Details(val drawer: Drawer, val dataModel: DataModel, val extendables: Extendables) {

    val articleData = dataModel.data

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

    inner class Cover(val index: Int, var proxy:ColorBufferProxy) : Animatable() {
        var width = 0.0
        var height = 0.0
        var x = 40.0
        var y = 40.0

        var clippedX = 40.0
        var clippedY = 40.0

        var removing = false
        var dead = false
        var dummy = 0.0

        var image: ColorBuffer? = null
        var coverlay: Coverlay? = null
        var coverlayOpacity = 0.0
        var zoom = 0.0


        fun updateClippedCoordinates(clipRectangle: Rectangle, clipContour: ShapeContour) {
            val test = Vector2(x, y)
            if (clipRectangle.contains(test)) {
                val clippedCoord = clipContour.nearest(test)
                clippedX = clippedCoord.position.x
                clippedY = clippedCoord.position.y
            }
        }

        fun zoomIn(predelay:Long = 0L){
            ::zoom.cancel()
            ::coverlayOpacity.cancel()
            ::zoom.animate(1.0, 800, Easing.CubicInOut, predelayInMs = predelay).completed.listen {

                coverlay = Coverlay(drawer, proxy, articleData[index].toList().filter { it != "" }.plus(index.toString()), index).apply {
                    subdivide(Section(coverlayFrame,  0))
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

    val covers = mutableMapOf<Int, Cover>()

    fun updateActive(oldPoints: List<Int>, newPoints: List<Int>) {

        val removed = oldPoints subtract newPoints
        val added = newPoints subtract oldPoints

        val latentBounds = newPoints.map { dataModel.latentPoints[it] }.bounds
        val drawBounds =Rectangle(0.0, 0.0, 1080.0, 1920.0)

        for (i in removed) {
            covers[i]?.let { c ->
                c.dead = false
                c.removing = true

                c.apply {
                    c::width.cancel()
                    c::height.cancel()
                    c::dummy.cancel()
                    c::width.animate(1.0, 1500, Easing.CubicInOut)
                    c::height.animate(0.0, 1500, Easing.CubicInOut)
                    c::dummy.animate(1.0, 1500).completed.listen {
                        c.removing = false
                        c.dead = true
                        c.proxy.cancel()
                        covers.remove(i)
                    }
                }
            }
        }


        for ((index, i) in newPoints.withIndex()) {

            val position = dataModel.latentPoints[i].map(latentBounds, drawBounds)

            val cover = covers.getOrPut(i) { Cover(i, colorBufferLoader.loadFromUrl("file:offline-data/covers/png/${skipPoints + i}.png", queue = false)) }
            cover.dead = false

            cover.apply {
                cover::x.cancel()
                cover::y.cancel()
                val dx = 500L
                val dy = 500L
                cover::x.animate(position.x, dx, Easing.QuadInOut)
                cover::y.animate(position.y, dy, Easing.QuadInOut)
            }
        }

        for (i in added) {

            val cover = covers.getOrPut(i) { Cover(i, colorBufferLoader.loadFromUrl("file:offline-data/covers/png/${skipPoints + i}.png", queue = false)) }

            cover.dead = false
            cover.removing = false


            cover.proxy!!.events.loaded.listen {


                cover.image = cover.proxy?.colorBuffer
                cover.width = 50.0
                cover.apply {
                    cover::x.cancel()
                    cover::y.cancel()
                    val position = dataModel.latentPoints[i].map(latentBounds, drawBounds)
                    cover.x = position.x
                    cover.y = position.y
                    cover::height.cancel()
                    cover::height.animate(50.0, 500, Easing.CubicInOut)
                }
            }

        }

    }

    fun heroPointChanged(event: HeroPointChangedEvent) {
        if (event.newPoint != null) {
            covers[event.newPoint]?.proxy?.priority = 0
            covers[event.newPoint]?.zoomIn(if (event.oldPoint == null) 0L else 1000L)
        }

        if (event.oldPoint != null) {
            covers[event.oldPoint]?.zoomOut()
        }
    }

    fun draw(seconds: Double) {
        fade.updateAnimation()

        val clipRectangle = Rectangle(200.0, 400.0, 1080.0-400.0, 1920.0-800.0).offsetEdges(50.0)
        val clipContour = clipRectangle.contour

        for (cover in covers.values.map { it }) {
            cover.updateAnimation()
            cover.updateClippedCoordinates(clipRectangle, clipContour)
        }

        drawer.isolated {
            drawer.defaults()
            drawer.fontMap = loadFont("data/fonts/RobotoCondensed-Regular.ttf", 16.0)
            var line = 0

            for (k in dataModel.activePoints) {
                drawer.text("$k - ${dataModel.data[k].title}", 40.0, line * 20.0 + 40.0)
                line++
            }
        }

        drawer.isolated {
            drawer.defaults()
            drawer.fill = ColorRGBa.TRANSPARENT
            drawer.depthWrite = false
            drawer.depthTestPass = DepthTestPass.ALWAYS

            for (cover in covers.values.sortedBy { it.zoom }) {
                val minimizedRect = Rectangle(cover.x - cover.width / 2.0, cover.y, cover.width, cover.height)
                val dynamicRect =  coverlayFrame * cover.zoom + minimizedRect * (1.0 - cover.zoom)
                drawer.rectangle(dynamicRect)

                cover.proxy.touch()
                val cb = cover.proxy.colorBuffer

                if(cb != null) {
                    drawer.imageFit(cb, dynamicRect)
                }

                if(cover.coverlay != null) {
                    cover.coverlay!!.draw(cover.coverlayOpacity)
                }
            }
        }
    }
}
