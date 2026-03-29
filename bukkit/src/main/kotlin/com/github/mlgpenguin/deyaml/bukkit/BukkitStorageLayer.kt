package com.github.mlgpenguin.deyaml.bukkit

import com.github.mlgpenguin.deyaml.StorageLayer
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

class BukkitStorageLayer : StorageLayer<File> {
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
