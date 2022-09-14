package sketches

import components.*
import extensions.QuaternionCamera
import org.openrndr.application
import org.openrndr.extensions.Screenshots
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.math.*

fun main() {
    application {
        val prod = System.getProperty("prod") != null

        configure {
            if (!prod) {
                width = ((1920 / 2) * 3) / 2 + 1080 / 2
                //position = IntVector2(1400, -2100)
                height = (1920) / 2
                hideWindowDecorations = false
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

            val facultyFilterModel = FacultyFilterModel(dataModel)
            val dateFilterModel = DateFilterModel()
            val facultyFilter = FacultyFilter(drawer, facultyFilterModel)
            val dateFilter = DateFilter(drawer, dateFilterModel)
            val guides = SphericalGuides(drawer)
            val pointCloud = PointCloud(drawer, dataModel, facultyFilterModel, dateFilterModel)
            val selector = SelectorWidget(drawer)
            val zoomLock = ZoomLockWidget(drawer)
            val miniDetails = MiniDetails(drawer, dataModel)
            val touchPoints = TouchPoints(drawer)
            val smallScreenView = ViewBox(drawer, Vector2(0.0, 0.0), 2880, 1920) {
                guides.draw()
                pointCloud.draw()
                selector.draw()
                zoomLock.draw()
                miniDetails.draw()
                touchPoints.draw()
                facultyFilter.draw()
                dateFilter.draw()
            }

            val details = Details(drawer, dataModel.data, dataModel)

            val bigScreenView = ViewBox(drawer, Vector2(2880.0, 0.0), 1080, 1920) { details.draw() }

            val minimap = Minimap(drawer)
            val minimapView = ViewBox(drawer, Vector2(0.0, height - 128.0), 128, 128) { minimap.draw() }


            mouse.buttonDown.listen {
                touchPoints.buttonDown(it)
                facultyFilter.buttonDown(it)
                zoomLock.buttonDown(it)
            }

            mouse.buttonUp.listen {
                touchPoints.buttonUp(it)
                dateFilter.buttonDown(it)
                dateFilter.buttonUp(it)
               // facultyFilter.buttonUp(it)
            }

            mouse.dragged.listen {
                touchPoints.dragged(it)
                facultyFilter.dragged(it)
                dateFilter.dragged(it)
            }

            val camera = extend(QuaternionCamera())

            camera.orientationChanged.listen {
                dataModel.lookAt = (it.matrix.matrix44.inversed * Vector4(0.0, 0.0, -10.0, 1.0)).xyz
                minimap.orientation = it
            }
            camera.zoomLockStarted.listen {
                zoomLock.fadeIn()
            }

            camera.zoomOutStarted.listen {
                selector.fadeOut()
                miniDetails.fadeOut()
                pointCloud.fadeOut()
                facultyFilter.fadeOut()
                // this is a bit of a hack to make sure the active points is emptied
                dataModel.activePoints = emptyList()
                details.fadeOut()
            }
            camera.zoomInFinished.listen {
                details.fadeIn()
                selector.fadeIn()
                miniDetails.fadeIn()
                pointCloud.fadeIn()
                //facultyFilter.fadeIn()
                // this is a bit of a hack to make sure active points are updated, it doesn't work though?
                camera.orientationChanged.trigger(camera.orientation)
            }
            dataModel.activePointsChanged.listen {
                details.updateActive(it.oldPoints, it.newPoints)
                miniDetails.updateActive(it.oldPoints, it.newPoints)
            }
            dataModel.heroPointChanged.listen {
                details.heroPointChanged(it)
            }

            zoomLock.zoomUnlockRequested.listen {
                camera.unlockZoom()
            }

            extend {
                facultyFilterModel.update()
                dateFilterModel.update()

                smallScreenView.draw()
                bigScreenView.draw()
                minimapView.draw()
            }
        }
    }
}