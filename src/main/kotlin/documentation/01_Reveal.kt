package documentation

import components.*
import documentation.resources.DataModel
import documentation.resources.FacultyFilterModel
import documentation.resources.PointCloud
import extensions.QuaternionCamera
import org.openrndr.animatable.easing.Easing
import org.openrndr.application
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.ffmpeg.h264
import org.openrndr.math.Quaternion
import org.openrndr.math.Vector2
import org.openrndr.math.Vector4
import org.openrndr.math.map

fun main() = application {
    configure {
        width = 1920
        height = 1080
    }
    program {
        val dm = DataModel()
        val dateFilterModel = documentation.resources.DateFilterModel(dm.years)
        val facultyFilterModel = FacultyFilterModel(dm)

        val pc = PointCloud(drawer, dm, FacultyFilterModel(dm), DateFilterModel(dm.years))
        val vb =  ViewBox(drawer, Vector2(0.0, 0.0), 1920 * 2, 1080 * 2) {
            pc.draw()
        }
        dm.dateFilter = dateFilterModel
        dm.facultyFilter = facultyFilterModel

        val qc = extend(QuaternionCamera()) {
            maxZoomOut = 125.0
            zoom.apply {
                ::dragChargeIncrement.animate(0.01, 13000, Easing.QuadInOut, 3000)
            }
        }
        pc.apply {
            ::radius.animate(5000.0, 14000, Easing.CubicInOut)
        }

        qc.orientationChanged.listen {
            dm.lookAt = (it.matrix.matrix44.inversed * Vector4(0.0, 0.0, -10.0, 1.0)).xyz
        }

        extend(ScreenRecorder()) {
            name = "Reveal"
            maximumDuration = 16.0
            h264 {
                constantRateFactor = 13
            }
        }

        extend {

            val acc = map(0.0, 0.01, 2005.0, 0.05, qc.zoom.dragChargeIncrement)
            val quat = Quaternion.fromAngles(-95.0 - acc * (seconds * qc.zoom.dragChargeIncrement), 0.0, 15.0)
            qc.orientation = quat
            qc.orientationChanged.trigger(quat)

            vb.draw()

        }
    }
}