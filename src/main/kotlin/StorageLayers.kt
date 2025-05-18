import org.yaml.snakeyaml.Yaml
import java.io.File

interface StorageLayer<T> {
    fun save(obj: Map<String, Any>, loc: T)
    fun load(from: T): Map<String, Any>
}

/** Primarily used for testing serisalisation in-memory */
internal class VirtualStorageLayer(): StorageLayer<Nothing?> {
    var objects: Map<String, Any> = emptyMap()

    override fun save(obj: Map<String, Any>, loc: Nothing?) { objects = obj }
    override fun load(from: Nothing?): Map<String, Any> = objects

}

internal class StringStorageLayer(): StorageLayer<String> {
    var objects: String = ""

    private fun String.transform(): Any {
        return when {
            Regex("\\d+").matches(this) -> this.toInt()
            Regex("\\d+\\.\\d+").matches(this) -> this.toDouble()
            else -> this
        }
    }

    override fun save(obj: Map<String, Any>, loc: String) { objects = obj.toString() }
    override fun load(from: String): Map<String, Any> {
        return objects
            .slice(1..objects.length - 2)
            .split(", ")
            .associate {
                it.split("=").let { s -> s[0] to s[1].transform() }
            }
    }
}

class SnakeYamlStorageLayer(): StorageLayer<File> {
    val yml = Yaml()
    override fun save(obj: Map<String, Any>, loc: File) {
        val out = yml.dumpAsMap(obj)
        println(out)
        loc.writeText(out)
    }

    override fun load(from: File): Map<String, Any> {
        return yml.load(from.readText())
    }
}