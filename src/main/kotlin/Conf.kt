import com.natpryce.konfig.*
import java.io.File

class Conf {
    private val inputFile by stringType

    private val config = ConfigurationProperties.systemProperties() overriding
            EnvironmentVariables() overriding
            ConfigurationMap(
                    "inputFile" to "input.mp4"
            )

    val inputFileName: String
        get() {
            if (File(config[inputFile]).canRead()) {
                return config[inputFile]
            }
            return "https://archive.org/download/mov-bbb/mov_bbb.mp4"
        }
}