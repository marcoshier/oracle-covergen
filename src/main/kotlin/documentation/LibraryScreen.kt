package documentation

import animatedCover.Coverlay
import animatedCover.Section
import classes.Entry
import com.github.doyaaaaaken.kotlincsv.client.CsvReader
import com.google.gson.Gson
import components.*
import documentation.resources.IdleSmall
import extensions.QuaternionCamera
import org.openrndr.Application
import org.openrndr.Program
import org.openrndr.animatable.Animatable
import org.openrndr.animatable.easing.Easing
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.kdtree.kdTree
import org.openrndr.extra.noise.Random
import org.openrndr.extra.noise.uniform
import org.openrndr.extra.proxyprogram.proxyApplication
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.ffmpeg.h264
import org.openrndr.internal.colorBufferLoader
import org.openrndr.math.*
import org.openrndr.shape.Rectangle
import org.openrndr.shape.bounds
import org.openrndr.shape.map
import sketches.Extendables
import java.io.File
import java.io.FileReader
import kotlin.math.sin


fun libraryScreen(parent: Application? = null): Program = proxyApplication(parent) {
    program {
        this.width = 3960
        this.height = 1920

        val dataModel = DataModel()

        val extendables = Extendables()

        val facultyFilterModel = FacultyFilterModel(dataModel)
        val dateFilterModel = DateFilterModel(dataModel.years)
        val facultyFilter = FacultyFilter(drawer, facultyFilterModel)

        // signs that we need to split things up
        val idleState = IdleState(0.0)
        dataModel.dateFilter = dateFilterModel
        dataModel.facultyFilter = facultyFilterModel

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
            //facultyFilter.draw()
            //dateFilter.draw()
            idleSmall.draw()
        }



        val details = Details(drawer, dataModel, extendables)

        val bigScreenView = ViewBox(drawer, Vector2(2880.0, 0.0), 1080, 1920) {
            details.draw(seconds)
            idleBig.draw()
        }

        val minimap = Minimap(drawer)
        val minimapView = ViewBox(drawer, Vector2(14.0, height - 64.0), 128, 128) {
            //minimap.draw()
        }


        mouse.buttonDown.listen {
            touchPoints.buttonDown(it)
            facultyFilter.buttonDown(it)
            zoomLock.buttonDown(it)
            idleState.exitIdle()
            dateFilter.buttonDown(it)
            minimap.buttonDown(it)
        }

        mouse.buttonUp.listen {
            touchPoints.buttonUp(it)
            dateFilter.buttonUp(it)
            facultyFilter.buttonUp(it)
            idleState.startTimer()
        }

        mouse.dragged.listen {
            touchPoints.dragged(it)
            facultyFilter.dragged(it)
            dateFilter.dragged(it)
            idleState.startTimer()
        }

        val qCamera = extend(QuaternionCamera())
        val idleController = IdleController(qCamera, dataModel)

        qCamera.orientationChanged.listen {
            dataModel.lookAt = (it.matrix.matrix44.inversed * Vector4(0.0, 0.0, -10.0, 1.0)).xyz
            minimap.orientation = it
        }
        qCamera.zoomLockStarted.listen {
            zoomLock.fadeIn()
            //minimap.fadeIn()
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

        var c = ColorRGBa.RED
        var newPoints: () -> Unit by userProperties

        dataModel.activePointsChanged.listen {
            newPoints = {  }
            details.updateActive(it.oldPoints, it.newPoints)
            miniDetails.updateActive(it.oldPoints, it.newPoints)
        }
        dataModel.heroPointChanged.listen {
            details.heroPointChanged(it)
        }

        zoomLock.zoomUnlockRequested.listen {
            qCamera.unlockZoom()
            //minimap.fadeOut()
        }

        idleState.idleModeStarted.listen {
            idleSmall.fadeIn()
            idleBig.fadeIn()

            facultyFilterModel.reset()
            dateFilterModel.reset()
            idleController.start()

            zoomLock.zoomUnlock()
        }
        idleState.idleModeEnded.listen {
            idleSmall.fadeOut()
            idleBig.fadeOut()
            idleController.end()
        }

        facultyFilterModel.filterChanged.listen {
            dataModel.filterChanged()
        }

        dateFilterModel.filterChanged.listen {
            dataModel.filterChanged()
        }

        minimap.minimapTouched.listen {
            qCamera.instantZoom()
        }


        idleState.startTimer()

        val quat = Quaternion.fromAngles(-95.0, 0.0, 15.0)
        qCamera.orientation = quat
        qCamera.orientationChanged.trigger(quat)
        extend {
            //g.visible = false
            //qCamera.enabled = false

            idleState.update()
            facultyFilterModel.update()
            dateFilterModel.update()
            idleController.update()

            minimapView.draw()
            smallScreenView.draw()
            bigScreenView.draw()
        }

    }

}