package sketches

import components.*
import extensions.QuaternionCamera
import org.openrndr.application
import org.openrndr.extra.camera.Orbital
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.timeoperators.LFO
import org.openrndr.extra.timeoperators.TimeOperators
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

            val extendables = Extendables()

            val facultyFilterModel = FacultyFilterModel(dataModel)
            val dateFilterModel = DateFilterModel(dataModel.years)
            val facultyFilter = FacultyFilter(drawer, facultyFilterModel)
            val idleState = IdleState(5.0)
            dataModel.dateFilter = dateFilterModel
            val dateFilter = DateFilter(drawer, dateFilterModel)
            val guides = SphericalGuides(drawer)
            val pointCloud = PointCloud(drawer, dataModel, facultyFilterModel, dateFilterModel)
            val selector = SelectorWidget(drawer)
            val zoomLock = ZoomLockWidget(drawer)
            val miniDetails = MiniDetails(drawer, dataModel)
            val touchPoints = TouchPoints(drawer)
            val idleSmall = IdleSmall(drawer)
            val idleBig = IdleBig(drawer)
            val smallScreenView = ViewBox(drawer, Vector2(0.0, 0.0), 2880, 1920) {
                guides.draw()
                pointCloud.draw()
                selector.draw()
                zoomLock.draw()
                miniDetails.draw()
                touchPoints.draw()
                facultyFilter.draw()
                dateFilter.draw()
                idleSmall.draw()
            }


            val details = Details(drawer, dataModel, extendables)

            val bigScreenView = ViewBox(drawer, Vector2(2880.0, 0.0), 1080, 1920) {
                details.draw(seconds)
                idleBig.draw()
            }

            val minimap = Minimap(drawer)
            val minimapView = ViewBox(drawer, Vector2(0.0, height - 128.0), 128, 128) { minimap.draw() }


            mouse.buttonDown.listen {
                touchPoints.buttonDown(it)
                facultyFilter.buttonDown(it)
                zoomLock.buttonDown(it)
                idleState.exitIdle()
            }

            mouse.buttonUp.listen {
                touchPoints.buttonUp(it)
                dateFilter.buttonDown(it)
                dateFilter.buttonUp(it)
               // facultyFilter.buttonUp(it)
                idleState.startTimer()
            }

            mouse.dragged.listen {
                touchPoints.dragged(it)
                facultyFilter.dragged(it)
                dateFilter.dragged(it)
                //idleMode.exitIdle()
            }

            val qCamera = extend(QuaternionCamera())

            qCamera.orientationChanged.listen {
                dataModel.lookAt = (it.matrix.matrix44.inversed * Vector4(0.0, 0.0, -10.0, 1.0)).xyz
                minimap.orientation = it
            }
            qCamera.zoomLockStarted.listen {
                zoomLock.fadeIn()
            }

            qCamera.zoomOutStarted.listen {
                selector.fadeOut()
                miniDetails.fadeOut()
                pointCloud.fadeOut()
                facultyFilter.fadeOut()
                // this is a bit of a hack to make sure the active points is emptied
                dataModel.muteActivePoints()
                details.fadeOut()
            }

            qCamera.zoomInFinished.listen {
                dataModel.unmuteActivePoints()
                details.fadeIn()
                selector.fadeIn()
                miniDetails.fadeIn()
                pointCloud.fadeIn()
                //facultyFilter.fadeIn()
                // this is a bit of a hack to make sure active points are updated, it doesn't work though?
                qCamera.orientationChanged.trigger(qCamera.orientation)
            }

            dataModel.activePointsChanged.listen {
                details.updateActive(it.oldPoints, it.newPoints)
                miniDetails.updateActive(it.oldPoints, it.newPoints)
            }
            dataModel.heroPointChanged.listen {
                details.heroPointChanged(it)
            }

            zoomLock.zoomUnlockRequested.listen {
                qCamera.unlockZoom()
            }

            idleState.idleModeStarted.listen {
                idleSmall.fadeIn()
                idleBig.fadeIn()
            }
            idleState.idleModeEnded.listen {
                idleSmall.fadeOut()
                idleBig.fadeOut()
            }
            //val g = extend(extendables.gui)
            //extend(TimeOperators()) { track(extendables.lfo) }
            //extend(extendables.orb)

            extend {
                //g.visible = false

                idleState.update()
                facultyFilterModel.update()
                dateFilterModel.update()

                smallScreenView.draw()
                bigScreenView.draw()
                minimapView.draw()
            }
        }
    }
}


class Extendables {
    var lfo = LFO()
    var orb = Orbital().apply {
        eye = Vector3(0.0, 0.0, 0.01)
        dampingFactor = 0.0
        near = 0.5
        far = 5000.0
        userInteraction = false
    }
    var gui = GUI()
}
