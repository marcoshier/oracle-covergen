package documentation.resources.coverlayResources

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.openrndr.events.Event
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.gui.addTo
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.extra.parameters.IntParameter
import org.openrndr.extra.parameters.XYParameter
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3


class Sliders(val gui: GUI) {

    inner class FXSliders {
        @DoubleParameter("FX Amount", 0.0, 1.0)
        var fxAmount = 0.0

        // PERTURB

        @DoubleParameter("Perturb Radius", 0.0, 10.0)
        var radius = 1.0

        @DoubleParameter("Perturb Scale", 0.0, 10.0)
        var scale = 1.0

        @DoubleParameter("Perturb Velocity", 0.0, 0.3)
        var velocity = 0.15

        @DoubleParameter("Perturb Decay", 0.0, 10.0)
        var decay = 1.0

        @DoubleParameter("Perturb Gain", 0.0, 0.224, precision = 3)
        var gain = 1.0


        // POISSON

        @IntParameter("Poisson Active", 0, 1)
        var poisson = 0


        // CELLS

        @IntParameter("Cells Amount X", 1, 50)
        var cellsX = 1

        @IntParameter("Cells Amount Y", 1, 50)
        var cellsY = 1

        @DoubleParameter("Cells Scale", 0.0, 1.0)
        var cellsScale = 0.0

        // FLUID DISTORT

        @DoubleParameter("Fluid Distort", 0.9, 1.0, precision = 3)
        var fd = 0.0
    }
    inner class ColorSliders {
        @IntParameter("Main Hue", 0, 7)
        var centerHue = 0

        @DoubleParameter("Contrast Reversal", 0.0, 1.0)
        var contrastReversal = 0.0
    }
    inner class CameraSliders {
        @DoubleParameter("Dimensions", 2.0, 3.0)
        var dimensions = 1.0

        @DoubleParameter("Camera Y Angle", 0.0, 1.0)
        var yAngle = 0.0

    }
    inner class EcosystemSliders  {

        @DoubleParameter("Complexity", 0.05, 1.0)
        var complexity = 0.05

        @DoubleParameter("Angle", 0.0, 360.0)
        var angle = 0.0

        @DoubleParameter("Concentricity", 0.0, 1.0)
        var concentricity = 0.0

        @DoubleParameter("Random angle", 0.0, 1.0)
        var randomAngle = 0.0

        @XYParameter("Attractor position")
        var attractorPos = Vector2(270.0, 480.0)

        @DoubleParameter("Rotation movement", 0.0, 1.0)
        var rotMovement = 0.5

    }
    inner class StructureSliders {

        @DoubleParameter("Height", 5.0, 1200.0)
        var height = 640.0

        @IntParameter("Height Segments", 10, 250)
        var heightSegments = 100


        @DoubleParameter("Width", 5.0, 400.0)
        var width = 540.0

        @IntParameter("Width / Rotation Segments", 10, 100)
        var rotationSegments = 100


        @DoubleParameter("Noise", 0.0, 1.0)
        var noise = 0.5

        @DoubleParameter("Noise Scale", 0.0, 0.1)
        var noiseScale = 0.1

        @DoubleParameter("Noise Frequency", 0.0, 0.5)
        var noiseFrequency = 0.005


    }
    inner class VertebraeSliders {
        @DoubleParameter("Visibility", 0.05, 1.0)
        var visibility = 0.5

        @DoubleParameter("Contour noise", 0.1, 1.0)
        var cNoise = 0.05

        @DoubleParameter("Contour noise frequency", 0.0, 0.02)
        var cNoiseFreq = 0.01

        @DoubleParameter("Contour offset", 0.0, 0.1)
        var cOffset = 0.01


        @DoubleParameter("Wave amount", 0.0, 1.0)
        var waveAmount = 1.0

        @IntParameter("Waveform", 0, 3)
        var waveform = 0

        @DoubleParameter("Wave frequency", 0.1, 50.0)
        var waveFrequency = 1.0

        @DoubleParameter("Wave phase", 0.01, 1.0)
        var wavePhase = 0.05


    }
    inner class CellSliders {

        @DoubleParameter("Cells amount", 0.0, 1.0)
        var corners = 0.0

        @IntParameter("Cell type", 0, 3)
        var cellType = 0

    }
    inner class ColumnContours {

        @DoubleParameter("Thickness", 0.0, 20.0)
        var thickness = 0.5

        @DoubleParameter("Pen pressure", 0.0, 1.0)
        var penPressure = 1.0

        @DoubleParameter("Turbulence Amount", 0.0, 1.0)
        var turbulenceAmount = 0.5

        @DoubleParameter("Turbulence Scale", 0.001, 0.08)
        var turbulenceScale = 0.001

    }
    inner class RowContours {

        @DoubleParameter("Thickness", 0.0, 20.0)
        var thickness = 0.5

        @DoubleParameter("Pen pressure", 0.0, 1.0)
        var penPressure = 1.0

        @DoubleParameter("Turbulence Amount", 0.0, 1.0)
        var turbulenceAmount = 0.5

        @DoubleParameter("Turbulence Scale", 0.001, 0.08)
        var turbulenceScale = 0.001

    }


    val fxSliders = FXSliders()
    val colorSliders = ColorSliders()
    val cameraSliders = CameraSliders()
    val ecosystemSliders = EcosystemSliders()
    val structureSliders = StructureSliders()
    val vertebraeSliders = VertebraeSliders()
    val cellSliders = CellSliders()
    val columnContours = ColumnContours()
    val rowContours = RowContours()

    fun update(json: String) {
        gui.loadParameters(json)
        println("yes")
    }


    fun addToGui() {
        fxSliders.addTo(gui, "FX")
        colorSliders.addTo(gui, "Color Settings")
        cameraSliders.addTo(gui, "Camera Settings")
        ecosystemSliders.addTo(gui, "Ecosystem Settings")
        structureSliders.addTo(gui, "Structure")
        vertebraeSliders.addTo(gui, "Vertebrae")
        cellSliders.addTo(gui, "Cells")
        columnContours.addTo(gui, "ThickLine / Vertical")
        rowContours.addTo(gui, "Contour / Horizontal")
    }

}

fun GUI.loadParameters(json: String) {
    val typeToken = object : TypeToken<Map<String, Map<String, GUI.ParameterValue>>>() {}
    val labeledValues: Map<String, Map<String, GUI.ParameterValue>> = Gson().fromJson(json, typeToken.type)

    fromObject(labeledValues)
}