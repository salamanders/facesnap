import mu.KotlinLogging
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.OpenCVFrameConverter
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.MatOfRect
import org.opencv.imgproc.Imgproc
import java.awt.Rectangle


private val logger = KotlinLogging.logger {}
private val conf = Conf()


fun main() {
    nu.pattern.OpenCV.loadShared() // Required first by org.openpnp
    System.loadLibrary(org.opencv.core.Core.NATIVE_LIBRARY_NAME)
    logger.info { "Step 1: Video " }

    val allFrames = sequence<org.opencv.core.Mat> {
        //
        val openCvConverter = OpenCVFrameConverter.ToMat()
        FFmpegFrameGrabber(conf.inputFileName).use { grabber ->
            grabber.start()
            while (true) {
                val frame = grabber.grabImage() ?: break
                val copyOfFrame = frame.clone()!!
                val openCvCoreMat = openCvConverter.convert(copyOfFrame)!!
                yield(org.opencv.core.Mat(openCvCoreMat.address()))
            }
            grabber.stop()
        }
    }
    val allClassifiers = conf.getClassifiers()
    require(allClassifiers.isNotEmpty())

    val detections = MatOfRect()
    val grayFrame = Mat()
    val grayRotatedFrame = Mat()

    val frameToClassifiersToLocations = mutableMapOf<Int, MutableMap<String, List<Rectangle>>>()
    frameToClassifiersToLocations.readFromFile("locations")
    if (frameToClassifiersToLocations.isNotEmpty()) {
        println("Restored from cache: ${frameToClassifiersToLocations.size}")
    }

    allFrames.forEachIndexed { frameNumber, mat ->
        if (frameToClassifiersToLocations.containsKey(frameNumber)) {
            println("$frameNumber skipped (cached)")
        } else {

            if ((frameNumber and (frameNumber - 1)) == 0) {
                println("Read frame $frameNumber")
                frameToClassifiersToLocations.saveToFile("locations")
                System.gc()
            }

            frameToClassifiersToLocations[frameNumber] = mutableMapOf()

            Imgproc.cvtColor(mat, grayFrame, Imgproc.COLOR_BGR2GRAY)
            Imgproc.equalizeHist(grayFrame, grayFrame)
            Core.rotate(grayFrame, grayRotatedFrame, Core.ROTATE_180)

            allClassifiers.forEach { detectorName, faceDetector ->
                faceDetector.detectMultiScale(grayFrame, detections)
                detections.toList().filterNotNull().map { cvRect ->
                    java.awt.Rectangle(cvRect.x, cvRect.y, cvRect.width, cvRect.height)
                }.let { rects ->
                    if (rects.isNotEmpty()) {
                        println("Frame $frameNumber $detectorName detected faces: ${rects.size} upside-up")
                        frameToClassifiersToLocations[frameNumber]!![detectorName] = rects
                    }
                }

                faceDetector.detectMultiScale(grayRotatedFrame, detections)
                detections.toList().filterNotNull().map { cvRect ->
                    java.awt.Rectangle(cvRect.x, cvRect.y, cvRect.width, cvRect.height)
                }.let { rects ->
                    if (rects.isNotEmpty()) {
                        println("Frame $frameNumber $detectorName detected faces: ${rects.size} upside-down")
                        frameToClassifiersToLocations[frameNumber]!![detectorName + "_180"] = rects
                    }
                }
            }
        }
    }
    frameToClassifiersToLocations.saveToFile("locations")
}