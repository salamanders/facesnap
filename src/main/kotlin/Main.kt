import mu.KotlinLogging
import net.tzolov.cv.mtcnn.FaceAnnotation
import net.tzolov.cv.mtcnn.MtcnnService
import org.bytedeco.javacpp.avutil
import org.bytedeco.javacv.FFmpegFrameFilter
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.FrameConverter
import org.bytedeco.javacv.Java2DFrameConverter
import java.awt.image.BufferedImage

private val logger = KotlinLogging.logger {}
private val conf = Conf()


fun main() {
    //nu.pattern.OpenCV.loadShared() // Required first by org.openpnp
    //System.loadLibrary(org.opencv.core.Core.NATIVE_LIBRARY_NAME)
    avutil.av_log_set_level(avutil.AV_LOG_QUIET) // ffmpeg gets loud per frame otherwise
    logger.info { "Step 1: Video " }

    val allFrames = sequence {
        println("Reading all frames from ${conf.inputFileName}")
        val converter: FrameConverter<BufferedImage> = Java2DFrameConverter()
        FFmpegFrameGrabber(conf.inputFileName).use { grabber ->
            grabber.start()

            val filter = FFmpegFrameFilter("scale=640:-1", grabber.imageWidth, grabber.imageHeight)
            filter.pixelFormat = grabber.pixelFormat
            filter.start()
            while (true) {
                filter.push(grabber.grabImage() ?: break)
                yield(converter.convert(filter.pull()).deepCopy())
            }
            grabber.stop()
        }
        println("Finished all frames!")
    }

    val mtcnnService = MtcnnService(30, 0.709, doubleArrayOf(0.6, 0.7, 0.7))

    val frameRotationToAnnotations = mutableMapOf<Pair<Int, Int>, List<FaceAnnotation>>()
    frameRotationToAnnotations.readFromFile("annotations")
    if (frameRotationToAnnotations.isNotEmpty()) {
        println("Restored from cache: ${frameRotationToAnnotations.size}")
    }

    allFrames.forEachIndexed { frameNumber, image ->
        if ((frameNumber and (frameNumber - 1)) == 0) {
            println("Read frame $frameNumber")
            frameRotationToAnnotations.saveToFile("annotations")
        }

        var rotatedImage = image
        (0..270 step 90).forEach { degreesRotated ->
            rotatedImage = if (degreesRotated == 0) rotatedImage else rotatedImage.rotate90cw()
            val faces = mtcnnService.faceDetection(rotatedImage)!!.toList().filterNotNull()
            frameRotationToAnnotations[Pair(frameNumber, degreesRotated)] = faces
            println("Frame $frameNumber Rotation $degreesRotated faces: ${faces.size}")
        }
    }
}