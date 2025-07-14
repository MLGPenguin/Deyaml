package adapters

object Adapters {
    // TODO: Separate string and object adapters
    private val registered_adapters: MutableMap<Class<*>, StringAdapter<*>> = mutableMapOf()

    /** For example: register(Int::class.java, IntConverter()) */
    fun <T> register(convertingType: Class<T>, converter: StringAdapter<T>) {
        registered_adapters.put(convertingType, converter)
    }

    fun <T> unregisterString(type: Class<T>) = registered_adapters.remove(type)
    fun <T> hasString(type: Class<T>) = registered_adapters.containsKey(type)
    fun <T> string(type: Class<T>): StringAdapter<T>? = registered_adapters[type] as? StringAdapter<T>
}

/** Serialises an entire object into a string for inline adaptations */
abstract class StringAdapter<T>(val toString: (T) -> String, val fromString: (String) -> T)

class IntRangeAdapter: StringAdapter<IntRange>(
    { "${it.start}:${it.endInclusive}" },
    { string -> string.split(":").map(String::toInt).let { it[0]..it[1] } }
)