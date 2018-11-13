package src.main.kotlin

import mu.KotlinLogging
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.OpenCVFrameConverter
import org.opencv.core.MatOfRect
import org.opencv.objdetect.CascadeClassifier


private val logger = KotlinLogging.logger {}
private val conf = Conf()


fun main() {
    println(System.getProperty("java.version"))
    //nu.pattern.OpenCV.loadShared()
    System.loadLibrary(org.opencv.core.Core.NATIVE_LIBRARY_NAME)
    logger.info { "Step 1: Video " }

    val mats = sequence {
        // val converter: FrameConverter<BufferedImage> = Java2DFrameConverter()
        val opencvConverter = OpenCVFrameConverter.ToMat()
        FFmpegFrameGrabber(conf.inputFileName).use { grabber ->
            grabber.start()
            while (true) {
                yield(opencvConverter.convert(grabber.grabImage() ?: break))
            }
            grabber.stop()
        }
    }

    val face = CascadeClassifier("lib/opencv/sources/data/haarcascades/haarcascade_frontalface_alt.xml")
    val faceDetections = MatOfRect()

    mats.forEachIndexed { idx, mat ->
        //face.detectMultiScale(mat, faceDetections)
        //val rects = faceDetections.toList().filterNotNull().toTypedArray()
        //println("Frame $idx detected ${rects.size} faces")
    }
}