package adapters

object MapKeyConverters {
    private val registered_converters: MutableMap<Class<*>, MapKeyConverter<*>> = mutableMapOf(Int::class.java to IntConverter(), Integer::class.java to IntConverter())

    /** For example: register(Int::class.java, IntConverter()) */
    fun <T> register(convertingType: Class<T>, converter: MapKeyConverter<T>) {
        registered_converters.put(convertingType, converter)
    }

    fun <T> has(type: Class<T>) = registered_converters.containsKey(type)
    fun <T> of(type: Class<T>) = registered_converters[type] as? MapKeyConverter<T>
}

abstract class MapKeyConverter<T>(val toString: (T) -> String, val fromString: (String) -> T)
class IntConverter: MapKeyConverter<Int>(Int::toString, String::toInt)