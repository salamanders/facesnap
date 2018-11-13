package src.main.kotlin

import mu.KotlinLogging
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.OpenCVFrameConverter
import org.opencv.core.Core
import org.opencv.core.MatOfRect
import org.opencv.objdetect.CascadeClassifier


private val logger = KotlinLogging.logger {}
private val conf = Conf()


fun main() {
    println(System.getProperty("java.version"))
    nu.pattern.OpenCV.loadShared() // from org.openpnp
    System.loadLibrary(org.opencv.core.Core.NATIVE_LIBRARY_NAME)
    logger.info { "Step 1: Video " }

    val mats = sequence {
        // val converter: FrameConverter<BufferedImage> = Java2DFrameConverter()
        val openCvConverter = OpenCVFrameConverter.ToMat()
        FFmpegFrameGrabber(conf.inputFileName).use { grabber ->
            grabber.start()
            while (true) {
                val frame = grabber.grabImage() ?: break
                val openCvCoreMat = openCvConverter.convert(frame)!!
                val mat = org.opencv.core.Mat(openCvCoreMat.address())
                yield(mat)
            }
            grabber.stop()
        }
    }

    val face = CascadeClassifier("haarcascade_frontalface_alt_tree.xml")
    val faceDetections = MatOfRect()

    mats.forEachIndexed { idx, mat ->

        face.detectMultiScale(mat, faceDetections)
        val rects = faceDetections.toList().filterNotNull().toTypedArray()

        Core.rotate(mat, mat, Core.ROTATE_180)
        face.detectMultiScale(mat, faceDetections)
        val rects2 = faceDetections.toList().filterNotNull().toTypedArray()

        if (rects.isNotEmpty() || rects2.isNotEmpty()) {
            println("Frame $idx detected faces: ${rects.size} upside-up, ${rects2.size} upside-down.")
        }
    }
}