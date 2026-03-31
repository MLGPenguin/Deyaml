package com.github.mlgpenguin.deyaml

import java.io.File

interface StorageLayer<T> {
    fun save(obj: Map<String, Any>, loc: T)
    fun load(from: T): Map<String, Any>
}

// Storing items virtually retains type info I dont want.
//class VirtualStorageLayer : StorageLayer<Nothing?> {
//    var objects: Map<String, Any> = emptyMap()
//    override fun save(obj: Map<String, Any>, loc: Nothing?) { objects = obj }
//    override fun load(from: Nothing?): Map<String, Any> = objects
//}

class BukkitStorageLayer : StorageLayer<File> {
    override fun save(obj: Map<String, Any>, loc: File) {
        // Can probably just use SnakeYaml here which simplifies this a lot since it's included in paper, and used under the hood..
        val cfg = YamlConfiguration.loadConfiguration(loc)
        obj.entries.forEach { (key, value) ->
            var set = value
            if (value.javaClass.isEnum) set = value.toString()
            cfg.set(key, set)
        }
        cfg.save(loc)
    }

    override fun load(from: File): Map<String, Any> {
        val cfg = YamlConfiguration.loadConfiguration(from)
        return cfg.getValues(true)
    }
}

class SnakeYamlStorageLayer : StorageLayer<File> {
    internal val yml = Yaml(CustomRepresenter(), DumperOptions())

    override fun save(obj: Map<String, Any>, loc: File) {
        loc.writeText(yml.dumpAsMap(obj))
    }

    override fun load(from: File): Map<String, Any> = yml.load(from.readText())

    class CustomRepresenter : Representer(DumperOptions()) {
        init {
            val setRepresenter = Represent { data ->
                representSequence(Tag.SEQ, (data as Set<*>).toList(), DumperOptions.FlowStyle.BLOCK)
            }
            val enumRepresenter = Represent { data ->
                representScalar(Tag.STR, (data as Enum<*>).toString())
            }
            multiRepresenters.putAll(arrayOf(
                Set::class.java to setRepresenter,
                Enum::class.java to enumRepresenter,
            ))
        }
    }

    class CustomConstructor : Constructor(LoaderOptions())
}

/** Storage layer that serialises to/from an in-memory YAML string. Useful for testing and string-based configs. */
class StringStorageLayer : StorageLayer<String> {
    private val yml = Yaml(SnakeYamlStorageLayer.CustomRepresenter(), DumperOptions())
    var ymlString: String = ""

    override fun save(obj: Map<String, Any>, loc: String) { ymlString = loc.ifEmpty { yml.dumpAsMap(obj) } }
    override fun load(from: String): Map<String, Any> = yml.load(from.ifEmpty { ymlString })
}
