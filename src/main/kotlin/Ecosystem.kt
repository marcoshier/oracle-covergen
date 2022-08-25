import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.gui.addTo
import org.openrndr.extra.noise.Random
import org.openrndr.extra.noise.simplex
import org.openrndr.extra.noise.uniformRing
import org.openrndr.extra.palette.Palette
import org.openrndr.extra.palette.PaletteStudio
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.extra.parameters.IntParameter
import org.openrndr.extra.parameters.XYParameter
import org.openrndr.extra.timeoperators.LFO
import org.openrndr.extra.camera.Orbital
import org.openrndr.math.Polar
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.clamp
import org.openrndr.math.transforms.frustum
import org.openrndr.math.transforms.transform
import org.openrndr.shape.Rectangle
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.sin

class Ecosystem (gui: GUI, val frame: Rectangle){

    private val cameraSliders = object {
        @DoubleParameter("Dimensions", 2.0, 3.0)
        var dimensions = 1.0

        @DoubleParameter("Camera Y Angle", 0.0, 180.0)
        var yAngle = 90.0

        @DoubleParameter("Camera X Angle", 0.0, 180.0)
        var xAngle = 180.0

    }.addTo(gui, "Camera Settings")
    private val ecosystemSliders = object {

        @IntParameter("N Structures", 1, 120)
        var nStructures = 1

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


    val orb = Orbital().apply {
        eye = Vector3(0.0, 0.0, 0.01)
        dampingFactor = 0.0
        near = 0.5
        far = 5000.0
        userInteraction = false
    }
    var structure = Structure(gui)
    var lfo = LFO()

    private val dry = renderTarget(frame.width.toInt(), frame.height.toInt()) {
        colorBuffer()
        depthBuffer()
    }
    val dryResolved = colorBuffer(frame.width.toInt(), frame.height.toInt())



    fun draw(drawer: Drawer, seconds: Double, palette: List<List<ColorRGBa>>){

        // the higher the nStructures, the lower the complexity
        val complexity = (1.0 / ecosystemSliders.nStructures).clamp(0.05, 1.0)

        val positions = (0 until ecosystemSliders.nStructures).map {
            val pos = Random.point(frame.offsetEdges(40.0)).mix(frame.center, it.toDouble() / ecosystemSliders.nStructures.toDouble())
            val angle = Random.double(130.0)
            pos to angle
        }

        dry.clearColor(0, ColorRGBa.TRANSPARENT)

        drawer.rotate(Vector3.UNIT_X, 180.0)
        drawer.translate(-frame.center)


        for((pos, angle) in positions) {

            val fixedPos = if(positions.size == 1) frame.center else pos

            drawer.isolatedWithTarget(dry) {


                if(positions.size == 1) {
                    drawer.clear(ColorRGBa.TRANSPARENT)
                }
                dry.clearDepth(1.0)


                drawer.translate(fixedPos.x, fixedPos.y, sin(fixedPos.x) * frame.height)

                if(ecosystemSliders.nStructures > 1) { // ecosystem movement
                    val attractor =  ecosystemSliders.attractorPos * Vector2(frame.width, frame.height) + frame.center
                    val angleToCenter = Math.toDegrees(atan2(fixedPos.y - attractor.y, fixedPos.x -  attractor.x))
                    val randomAngle =  angle * ecosystemSliders.randomAngle
                    val rotationMovement = (simplex(335, fixedPos.x * 0.004, fixedPos.y * 0.004 + seconds * 0.05) * 0.5 + 0.5) * ecosystemSliders.rotMovement


                    drawer.rotate(angleToCenter * ecosystemSliders.concentricity)
                    drawer.rotate(randomAngle + rotationMovement * 60.0)
                }
                drawer.rotate(ecosystemSliders.angle)
                drawer.scale(complexity * 1.1 + 0.075)

                orb.camera.rotateTo(cameraSliders.xAngle, cameraSliders.yAngle)

                structure.draw(drawer, seconds, lfo, complexity, cameraSliders.dimensions, palette)

            }
        }
        dry.colorBuffer(0).copyTo(dryResolved)


    }
}