import com.natpryce.konfig.*
import org.bytedeco.javacpp.avutil
import org.opencv.objdetect.CascadeClassifier
import java.io.File

class Conf {
    init {
        avutil.av_log_set_level(avutil.AV_LOG_QUIET) // ffmpeg gets loud per frame otherwise
    }

    private val inputFile by stringType

    private val config = ConfigurationProperties.systemProperties() overriding
            EnvironmentVariables() overriding
            ConfigurationMap(
                    "inputFile" to "input.mp4"
            )

    val inputFileName: String
        get() = config[inputFile]

    fun getClassifiers(): Map<String, CascadeClassifier> = File("./classifiers/").walkTopDown().filter { it.isFile }
            //.filter { it.name.endsWith("xml")}
            .map { it.nameWithoutExtension to CascadeClassifier(it.absolutePath) }.toMap()

}