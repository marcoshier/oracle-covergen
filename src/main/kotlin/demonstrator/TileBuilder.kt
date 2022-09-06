package demonstrator

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.MagnifyingFilter
import org.openrndr.draw.MinifyingFilter
import org.openrndr.draw.loadImage
import org.openrndr.draw.renderTarget
import org.openrndr.extra.imageFit.FitMethod
import org.openrndr.extra.imageFit.imageFit
import org.openrndr.math.Vector2
import java.io.File
import kotlin.math.ceil

/**
 * tile builder
 *
 * Like TileBuilder01 but creates tilings for merged sets
 */
fun main() {
    val tilingSize = 4096
    val tileSize = 128
    val tileDir = File("data/tiles-merged-$tileSize-v2").apply {
        if (!this.exists()) {
            this.mkdirs()
        }
    }
    val imageDirs =
        listOf(
            File("data/generated"),
        )

    if (!tileDir.exists()) {
        tileDir.mkdirs()
    }
    println(" yooo")
    val images = imageDirs.flatMap {
        it.listFiles().filter {
            (it.extension == "jpg" || it.extension=="png") && it.isFile
        }.sortedBy { it.name }
    }
    println("ehh")


    application {
        program {
            val tilesPerTiling = (tilingSize/tileSize) * (tilingSize/tileSize)
            val sets = images.windowed(tilesPerTiling, step = tilesPerTiling, partialWindows = true).withIndex()
            val tilesPerDimension = (tilingSize/tileSize)

            val tilingTarget = renderTarget(tilingSize, tilingSize) {
                colorBuffer()
            }

            for ((setIndex, set) in sets) {
                val tilingFile = File(tileDir, "tiling-${String.format("%04d", setIndex)}.png")
                println("set: ${setIndex}")
                if (!tilingFile.exists()) {
                    drawer.withTarget(tilingTarget) {
                        drawer.ortho(tilingTarget)
                        drawer.clear(ColorRGBa.TRANSPARENT)
                        for ((imageIndex, imageFile) in set.withIndex()) {
                            val x = (imageIndex % tilesPerDimension) * tileSize.toDouble()
                            val y = (imageIndex / tilesPerDimension) * tileSize.toDouble()
                            val image = loadImage(imageFile)
                            image.filterMin = MinifyingFilter.LINEAR_MIPMAP_LINEAR
                            image.filterMag = MagnifyingFilter.LINEAR
                            drawer.imageFit(image, x+2.0, y+2.0, tileSize.toDouble()-4.0, tileSize.toDouble()-4.0, fitMethod = FitMethod.Contain, verticalPosition = 0.0, horizontalPosition = 0.0)
                            image.destroy()
                        }
                    }
                    tilingTarget.colorBuffer(0).saveToFile(tilingFile)
                }
            }
            println("done!")
        }
    }
}