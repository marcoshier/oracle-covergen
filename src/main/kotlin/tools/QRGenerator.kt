package tools

import classes.Entry
import com.google.gson.Gson
import com.google.zxing.BarcodeFormat
import org.openrndr.application
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.yield
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.MagnifyingFilter
import org.openrndr.draw.colorBuffer
import org.openrndr.extensions.Screenshots
import org.openrndr.launch
import java.io.File
import java.io.FileReader

fun main() = application {
    configure {
        width = 41
        height = 41
        hideWindowDecorations = true
    }
    program {


        val skipPoints = 200945
        val articleData = Gson().fromJson(FileReader(File("data/mapped-v2r1.json")),Array<Entry>::class.java).drop(skipPoints)
        val entries = articleData.map {it.ogdata["To reference this document use:"] as String }


        fun getQRCodeImage(barcodeText: String): ColorBuffer {
            val qrCodeWriter = QRCodeWriter()
            val bitMatrix = qrCodeWriter.encode(barcodeText, BarcodeFormat.QR_CODE, 30, 30)
            val cb = colorBuffer(bitMatrix.width, bitMatrix.height)
            cb.filterMag = MagnifyingFilter.NEAREST
            val shad = cb.shadow


            for (y in 0 until bitMatrix.width) {
                for (x in 0 until bitMatrix.height) {
                    shad[x, y] = if (bitMatrix[x, y]) ColorRGBa.BLACK else ColorRGBa.WHITE
                }
            }

            shad.upload()
            return cb
        }

        val s = extend(Screenshots())

        var currentIndex = -1

        launch {
                for((i, entry) in entries.withIndex()) {
                    currentIndex++
                    s.name = "data/qrs/${String.format("%06d", i + skipPoints)}.png"
                    s.trigger()


                    for (z in 0 until 3) {
                        yield()
                    }
                }

        }


        extend {

            val img = getQRCodeImage(entries[currentIndex])
            drawer.image(img)

        }
    }
}