package com.github.mlgpenguin.deyaml

interface StorageLayer<T> {
    fun save(obj: Map<String, Any>, loc: T)
    fun load(from: T): Map<String, Any>
}

class VirtualStorageLayer : StorageLayer<Nothing?> {
    var objects: Map<String, Any> = emptyMap()
    override fun save(obj: Map<String, Any>, loc: Nothing?) { objects = obj }
    override fun load(from: Nothing?): Map<String, Any> = objects
}
