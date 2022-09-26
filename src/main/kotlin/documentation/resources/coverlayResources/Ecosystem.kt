package documentation.resources.coverlayResources

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.noise.Random
import org.openrndr.extra.noise.simplex
import org.openrndr.extra.timeoperators.LFO
import org.openrndr.extra.camera.Orbital
import org.openrndr.math.*
import org.openrndr.shape.Rectangle
import kotlin.math.atan2
import kotlin.math.sin

class Ecosystem (val drawer: Drawer, val sliders: Sliders, val frame: Rectangle, val lfo: LFO, val orb: Orbital){

    private val maxNStructures = 159

    var structure = Structure(drawer, sliders)

    private val dry = renderTarget(frame.width.toInt(), frame.height.toInt(), multisample = BufferMultisample.SampleCount(32)) {
        colorBuffer()
        depthBuffer()
    }
    val dryResolved = colorBuffer(frame.width.toInt(), frame.height.toInt())

    fun draw(t: Double, palette: List<List<ColorRGBa>>){
        Random.resetState()

        val nStructures = (smoothstep(0.6, 1.0, sliders.ecosystemSliders.complexity) * maxNStructures + 1).toInt()
        val complexity = (1.0 / nStructures).clamp(0.1, 1.0)
        val positions = (0 until nStructures).map {
            val center = sliders.ecosystemSliders.attractorPos * frame.center + frame.center
            val pos = Random.point(frame.offsetEdges(40.0)).mix(center, (it.toDouble() / nStructures.toDouble()) * sliders.ecosystemSliders.concentricity)
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
                    val attractor =  sliders.ecosystemSliders.attractorPos * Vector2(frame.width, frame.height) + frame.center
                    val angleToCenter = Math.toDegrees(atan2(fixedPos.y - attractor.y, fixedPos.x -  attractor.x))
                    val randomAngle =  angle * sliders.ecosystemSliders.randomAngle
                    val rotationMovement = (simplex(335, fixedPos.x * 0.004, fixedPos.y * 0.004 + t * 0.05) * 0.5 + 0.5) * sliders.ecosystemSliders.rotMovement


                    drawer.rotate(angleToCenter * sliders.ecosystemSliders.concentricity)
                    drawer.rotate(randomAngle + rotationMovement * 120.0)
                }
                drawer.rotate(sliders.ecosystemSliders.angle)
                drawer.scale(complexity * 1.1 + 0.075)

                val pushback = map(5.0, 1200.0, 960.0, 600.0, structure.height)

                orb.camera.panTo(Vector3(0.0, -pushback * sliders.cameraSliders.yAngle , 180.0 * sliders.cameraSliders.yAngle))
                orb.camera.rotateTo(180.0, sliders.cameraSliders.yAngle * 90.0 + 90.0)

                structure.draw(t, lfo, complexity, palette)

            }
        }

        dry.colorBuffer(0).copyTo(dryResolved)
        drawer.defaults()
    }
}