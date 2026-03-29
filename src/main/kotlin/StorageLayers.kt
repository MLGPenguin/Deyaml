import org.bukkit.configuration.file.YamlConfiguration
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.Constructor
import org.yaml.snakeyaml.nodes.Tag
import org.yaml.snakeyaml.representer.Represent
import org.yaml.snakeyaml.representer.Representer
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
    internal val yml = Yaml(CustomRepresenter(), DumperOptions())

    override fun save(obj: Map<String, Any>, loc: File) {
        val out = yml.dumpAsMap(obj)
        loc.writeText(out)
    }

    override fun load(from: File): Map<String, Any> {
        return yml.load(from.readText())
    }

    class CustomRepresenter(): Representer(DumperOptions()) {
        init {
            val setRepresenter = Represent { data -> representSequence(Tag.SEQ, (data as Set<*>).toList(), DumperOptions.FlowStyle.BLOCK) }
            val enumRepresenter = Represent { data -> representScalar(Tag.STR, (data as Enum<*>).toString()) }
            this.multiRepresenters.putAll(arrayOf(
                Set::class.java to setRepresenter,
                Enum::class.java to enumRepresenter,
            ))
        }
    }

    class CustomConstructor : Constructor(LoaderOptions()) {}
}


class BukkitStorageLayer(): StorageLayer<File> {
    override fun save(obj: Map<String, Any>, loc: File) {
        val cfg = YamlConfiguration.loadConfiguration(loc)
        cfg.set("", obj)
        cfg.save(loc)
    }

    override fun load(from: File): Map<String, Any> {
        val cfg = YamlConfiguration.loadConfiguration(from)
        return cfg.getValues(true)
    }
}