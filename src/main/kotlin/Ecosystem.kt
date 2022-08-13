import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.gui.addTo
import org.openrndr.extra.noise.Random
import org.openrndr.extra.palette.Palette
import org.openrndr.extra.palette.PaletteStudio
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.extra.parameters.IntParameter
import org.openrndr.extra.timeoperators.LFO
import org.openrndr.extras.camera.Orbital
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.shape.Rectangle

class Ecosystem (gui: GUI, frame: Rectangle){ // probably dont need this anymore

    private val cameraSliders = object {
        @DoubleParameter("Dimensions", 2.0, 3.0)
        var dimensions = 1.0

        @DoubleParameter("PanX", 0.0, 540.0)
        var panX = 270.0

        @DoubleParameter("PanY", 0.0, 960.0)
        var panY = 480.0

        @DoubleParameter("PanZ", -540.0, 5320.0)
        var panZ = 0.0

        @DoubleParameter("Zoom", -540.0, 540.0)
        var zoom = 50.0

        @DoubleParameter("Rotate Z", -180.0, 180.0)
        var rotateZ = 0.0
    }.addTo(gui, "Camera Settings")

    val orb = Orbital().apply {
        eye = Vector3(0.0, 0.0, 1500.0)
        dampingFactor = 0.0
        near = 0.5
        far = 5000.0
        userInteraction = false
    }
    var structure = Structure(gui)
    var lfo = LFO()

    val dry = renderTarget(frame.width.toInt(), frame.height.toInt(), multisample = BufferMultisample.SampleCount(8)) {
        colorBuffer()
        depthBuffer()
    }
    val dryResolved = colorBuffer(frame.width.toInt(), frame.height.toInt())

    fun draw(drawer: Drawer, seconds: Double, palette: List<List<ColorRGBa>>){

        drawer.isolatedWithTarget(dry) {

            drawer.clear(ColorRGBa.TRANSPARENT)

            orb.camera.panTo(Vector3(0.0, height / 2.0, width / 2.0))
            orb.camera.zoomTo(cameraSliders.zoom)

            structure.draw(drawer, seconds, lfo, cameraSliders.dimensions, palette)
        }
        dry.colorBuffer(0).copyTo(dryResolved)


    }
}