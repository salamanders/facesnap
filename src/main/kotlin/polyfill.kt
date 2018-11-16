import com.google.gson.GsonBuilder
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.security.MessageDigest
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream


/** If you pass in a file name, the label is a hash of the file.  Or pass in a generic string label */
fun labelToFile(fileOrLabel: String): File {
    val f = File(fileOrLabel)

    // Max of 10MB for hashing, which should be plenty but still fast.
    val hash = if (f.canRead()) {
        val byteArray = ByteArray(Math.min(f.length(), 1024 * 1024 * 10L).toInt())
        File(fileOrLabel).inputStream().use {
            it.read(byteArray)
        }
        "_" + MessageDigest.getInstance("MD5").digest(byteArray).joinToString("") { byte ->
            String.format("%02X", byte)
        }.substring(0, 10)
    } else {
        ""
    }
    return File("$fileOrLabel$hash.ser.gz")
}


internal val gson = GsonBuilder().setPrettyPrinting().create()!!

fun <K, V> MutableMap<K, V>.saveToFile(label: String) {
    val file = labelToFile(label)
    OutputStreamWriter(GZIPOutputStream(file.outputStream())).use {
        it.write(gson.toJson(this))
    }
    println("Cached to file: ${file.name}")
}

fun <K, V> MutableMap<K, V>.readFromFile(label: String) {
    val file = labelToFile(label)
    if (file.canRead()) {
        InputStreamReader(GZIPInputStream(file.inputStream())).use {
            val read = gson.fromJson(it.readText(), Map::class.java)
            println("Read cached ${file.name} size:${read.size}...")
            @Suppress("UNCHECKED_CAST")
            this.putAll(read as Map<out K, V>)
        }
    }
}

fun BufferedImage.deepCopy(): BufferedImage {
    val cm = colorModel!!
    val isAlphaPremultiplied = cm.isAlphaPremultiplied
    val raster = copyData(null)!!
    return BufferedImage(cm, raster, isAlphaPremultiplied, null)
}


fun BufferedImage.rotate90cw(): BufferedImage {
    val rotatedImage = BufferedImage(height, width, type)
    (rotatedImage.graphics as Graphics2D).let { g2d: Graphics2D ->
        g2d.rotate(Math.toRadians(90.0))
        g2d.drawImage(this, 0, -rotatedImage.width, null)
        g2d.dispose()
    }
    return rotatedImage
}