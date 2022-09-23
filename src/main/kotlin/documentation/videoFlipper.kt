package documentation

import org.openrndr.application
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.ffmpeg.VideoPlayerConfiguration
import org.openrndr.ffmpeg.loadVideo

fun main() = application {
    configure {
        width = 960
        height = 540
    }
    program {
        val vc = VideoPlayerConfiguration().apply {
            allowFrameSkipping = false
        }
        val vid = loadVideo("video/noGUI - Copy.mp4", configuration = vc).apply {
            play()
        }

        extend(ScreenRecorder()) {
            contentScale = 2.0
        }

        extend {

            drawer.translate(0.0, 270.0)
            drawer.rotate(-90.0)
            drawer.translate(-vid.width / 2.0, 0.0)

            vid.draw(drawer)
        }
    }
}