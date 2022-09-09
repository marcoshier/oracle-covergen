package sketches

import com.github.doyaaaaaken.kotlincsv.client.CsvReader
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import components.*
import extensions.QuaternionCamera
import org.openrndr.application
import org.openrndr.extensions.Screenshots
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.math.*
import org.openrndr.shape.Rectangle
import org.openrndr.shape.bounds
import org.openrndr.shape.map
import java.io.File

fun main() {
    application {

        val prod = System.getProperty("prod") != null

        configure {
            if (!prod) {
                width = ((1920 / 2) * 3) / 2 + 1080 / 2
                height = (1920) / 2
            } else {
                width = ((1920 / 2) * 3)  + 1080
                height = (1920)
                position = IntVector2(0, 0)
                hideWindowDecorations = true
                hideCursor = true
            }

        }
        program {
            val dataModel = DataModel()

            extend(Screenshots())

            val camera = extend(QuaternionCamera())

            val guides = SphericalGuides(drawer)
            val pointCloud = PointCloud(drawer, dataModel.points)
            val selector = SelectorWidget(drawer)
            val touchPoints = TouchPoints(drawer)
            val smallScreenView = ViewBox(drawer, Vector2(0.0, 0.0), 2880, 1920) {
                guides.draw()
                pointCloud.draw()
                selector.draw()
                touchPoints.draw()
            }

            val details = Details(drawer, dataModel.data)
            val bigScreenView = ViewBox(drawer, Vector2(2880.0, 0.0), 1080, 1920) { details.draw() }

            val minimap = Minimap(drawer)
            val minimapView = ViewBox(drawer, Vector2(0.0, height - 128.0), 128, 128) { minimap.draw() }


            mouse.buttonDown.listen {
                touchPoints.buttonDown(it)
            }

            mouse.buttonUp.listen {
                touchPoints.buttonUp(it)
            }

            mouse.dragged.listen {
                touchPoints.dragged(it)
            }



            camera.orientationChanged.listen {
                dataModel.lookAt = (it.matrix.matrix44.inversed * Vector4(0.0, 0.0, -10.0, 1.0)).xyz
                minimap.orientation = it
            }
            camera.zoomOutStarted.listen {
                selector.fadeOut()
                pointCloud.fadeOut()
                guides.fadeIn()
                // this is a bit of a hack to make sure the active points is emptied
                dataModel.activePoints = emptyList()
                details.fadeOut()
            }
            camera.zoomInFinished.listen {
                details.fadeIn()
                selector.fadeIn()
                pointCloud.fadeIn()
                guides.fadeOut()
                // this is a bit of a hack to make sure active points are updated, it doesn't work though?
                camera.orientationChanged.trigger(camera.orientation)
            }
            dataModel.activePointsChanged.listen {
                details.updateActive(it.oldPoints, it.newPoints)
            }

            extend {
                smallScreenView.draw()
                bigScreenView.draw()
                minimapView.draw()
            }
        }
    }
}