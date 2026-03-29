package com.github.mlgpenguin.deyaml.adapters

object Adapters {
    private val registeredAdapters: MutableMap<Class<*>, StringAdapter<*>> = mutableMapOf()

    fun <T> register(convertingType: Class<T>, converter: StringAdapter<T>) {
        registeredAdapters[convertingType] = converter
    }

    fun <T> unregister(type: Class<T>) = registeredAdapters.remove(type)
    fun <T> has(type: Class<T>) = registeredAdapters.containsKey(type)
    fun <T> of(type: Class<T>): StringAdapter<T>? = registeredAdapters[type] as? StringAdapter<T>
}

abstract class StringAdapter<T>(val toString: (T) -> String, val fromString: (String) -> T)

class IntRangeAdapter : StringAdapter<IntRange>(
    { "${it.start}:${it.endInclusive}" },
    { string -> string.split(":").map(String::toInt).let { it[0]..it[1] } }
)
