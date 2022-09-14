package components.animatedCover

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.gui.addTo
import org.openrndr.extra.noise.Random
import org.openrndr.extra.noise.simplex
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.extra.parameters.XYParameter
import org.openrndr.extra.timeoperators.LFO
import org.openrndr.extra.camera.Orbital
import org.openrndr.math.*
import org.openrndr.shape.Rectangle
import kotlin.math.atan2
import kotlin.math.sin

class Ecosystem (gui: GUI, val frame: Rectangle,val lfo: LFO,val orb: Orbital){

    private val cameraSliders = object {
        @DoubleParameter("Dimensions", 2.0, 3.0)
        var dimensions = 1.0

        @DoubleParameter("Camera Y Angle", 0.0, 1.0)
        var yAngle = 0.0

    }.addTo(gui, "Camera Settings")
    private val ecosystemSliders = object {

        @DoubleParameter("Complexity", 0.05, 1.0)
        var complexity = 0.05

        @DoubleParameter("Angle", 0.0, 360.0)
        var angle = 0.0

        @DoubleParameter("Concentricity", 0.0, 1.0)
        var concentricity = 0.0

        @DoubleParameter("Random angle", 0.0, 1.0)
        var randomAngle = 0.0

        @XYParameter("Attractor position")
        var attractorPos = frame.center

        @DoubleParameter("Rotation movement", 0.0, 1.0)
        var rotMovement = 0.5

    }.addTo(gui, "Ecosystem Settings")


    var structure = Structure(gui)

    private val dry = renderTarget(frame.width.toInt(), frame.height.toInt(), multisample = BufferMultisample.SampleCount(32)) {
        colorBuffer()
        depthBuffer()
    }
    val dryResolved = colorBuffer(frame.width.toInt(), frame.height.toInt())


    fun draw(drawer: Drawer, t: Double, palette: List<List<ColorRGBa>>){
        Random.resetState()

        val maxNStructures = 159
        val nStructures = (smoothstep(0.6, 1.0, ecosystemSliders.complexity) * maxNStructures + 1).toInt()
        val complexity = (1.0 / nStructures).clamp(0.1, 1.0)
        val positions = (0 until nStructures).map {
            val center = ecosystemSliders.attractorPos * frame.center + frame.center
            val pos = Random.point(frame.offsetEdges(40.0)).mix(center, (it.toDouble() / nStructures.toDouble()) * ecosystemSliders.concentricity)
            val angle = Random.double(130.0)
            pos to angle
        }

        dry.clearColor(0, ColorRGBa.TRANSPARENT)

        drawer.rotate(Vector3.UNIT_X, 180.0)
        drawer.translate(-frame.center)

        if(nStructures > 1) {
            drawer.depthWrite = false
            drawer.depthTestPass = DepthTestPass.ALWAYS
        }

        for((pos, angle) in positions) {

            val fixedPos = if(positions.size == 1) frame.center else pos

            drawer.isolatedWithTarget(dry) {


                if(positions.size == 1) {
                    drawer.clear(ColorRGBa.TRANSPARENT)
                }
                dry.clearDepth(1.0)

                val z = if (nStructures == 1) sin(fixedPos.x) * frame.height else sin(fixedPos.x) * 0.25 - frame.height / 2.0

                drawer.translate(fixedPos.x, fixedPos.y, z)

                if(nStructures > 1) { // ecosystem movement
                    val attractor =  ecosystemSliders.attractorPos * Vector2(frame.width, frame.height) + frame.center
                    val angleToCenter = Math.toDegrees(atan2(fixedPos.y - attractor.y, fixedPos.x -  attractor.x))
                    val randomAngle =  angle * ecosystemSliders.randomAngle
                    val rotationMovement = (simplex(335, fixedPos.x * 0.004, fixedPos.y * 0.004 + t * 0.05) * 0.5 + 0.5) * ecosystemSliders.rotMovement


                    drawer.rotate(angleToCenter * ecosystemSliders.concentricity)
                    drawer.rotate(randomAngle + rotationMovement * 120.0)
                }
                drawer.rotate(ecosystemSliders.angle)
                drawer.scale(complexity * 1.1 + 0.075)

                val pushback = map(5.0, 1200.0, 960.0, 600.0, structure.height)

                orb.camera.panTo(Vector3(0.0, -pushback * cameraSliders.yAngle , 180.0 * cameraSliders.yAngle))
                orb.camera.rotateTo(180.0, cameraSliders.yAngle * 90.0 + 90.0)

                structure.draw(drawer, t, lfo, complexity, cameraSliders.dimensions, palette)

            }
        }
        dry.colorBuffer(0).copyTo(dryResolved)

        drawer.defaults()
    }
}