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
        configure {
            width = 1920
            height = 1080
        }
        program {
             val points = csvReader().readAllWithHeader(File("offline-data/graph/graph-tsne-d-100-i-100-p25-v2.csv")).map {
                 Vector2(it["x"]!!.toDouble(), it["y"]!!.toDouble())
             }
            val bounds = points.bounds
            val llbounds = Rectangle(-180.0, 0.0, 360.0, 180.0)
            val latlon = points.map { it.map(bounds, llbounds) }

            val positions = latlon.map { Spherical(it.x, it.y, 10.0).cartesian }
            val dataModel = DataModel(positions)

            extend(Screenshots())

            val camera = extend(QuaternionCamera())

            val guides = SphericalGuides(drawer)
            val pointCloud = PointCloud(drawer, positions)

            val selector = SelectorWidget(drawer)
            val details = Details(drawer, dataModel)

            val minimap = Minimap(drawer)
            val minimapView = ViewBox(drawer, Vector2(0.0, height - 128.0), 128, 128) { minimap.draw() }


            camera.orientationChanged.listen {
                if (!camera.zooming) {
                    dataModel.lookAt = (it.matrix.matrix44.inversed * Vector4(0.0, 0.0, -10.0, 1.0)).xyz
                }
                minimap.orientation = it
            }
            camera.zoomOutStarted.listen {
                selector.fadeOut()
                pointCloud.fadeOut()
                guides.fadeIn()
                // this is a bit of a hack to make sure the active points is emptied
                dataModel.activePoints = emptyList()
            }
            camera.zoomInFinished.listen {
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
                guides.draw()
                pointCloud.draw()
                selector.draw()
                details.draw()
                minimapView.draw()
            }
        }
    }
}