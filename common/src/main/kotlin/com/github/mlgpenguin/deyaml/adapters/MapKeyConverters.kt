package com.github.mlgpenguin.deyaml.adapters

object MapKeyConverters {
    private val registeredConverters: MutableMap<Class<*>, MapKeyConverter<*>> = mutableMapOf(
        Int::class.java to IntConverter(),
        Integer::class.java to IntConverter()
    )

    /** For example: register(Int::class.java, IntConverter()) */
    fun <T> register(convertingType: Class<T>, converter: MapKeyConverter<T>) {
        registeredConverters[convertingType] = converter
    }

    fun <T> has(type: Class<T>) = registeredConverters.containsKey(type)
    fun <T> of(type: Class<T>) = registeredConverters[type] as? MapKeyConverter<T>
}

abstract class MapKeyConverter<T>(val toString: (T) -> String, val fromString: (String) -> T)
class IntConverter : MapKeyConverter<Int>(Int::toString, String::toInt)
