package adapters

object Adapters {
    // TODO: Separate string and object adapters
    private val registered_string_adapters: MutableMap<Class<*>, StringAdapter<*>> = mutableMapOf()
    private val registered_map_adapters: MutableMap<Class<*>, MapAdapter<*>> = mutableMapOf()

    /** For example: register(Int::class.java, IntConverter()) */
    fun <T> register(convertingType: Class<T>, converter: IAdapter<T>) {
        if (converter is StringAdapter) {
            registered_string_adapters.put(convertingType, converter)
        } else if (converter is MapAdapter) {
            registered_map_adapters.put(convertingType, converter)
        }
    }

    fun <T> unregisterString(type: Class<T>) = registered_string_adapters.remove(type)
    fun <T> unregisterMap(type: Class<T>) = registered_map_adapters.remove(type)

    fun <T> hasString(type: Class<T>) = registered_string_adapters.containsKey(type)
    fun <T> hasMap(type: Class<T>) = registered_map_adapters.containsKey(type)

    fun <T> string(type: Class<T>): StringAdapter<T>? = registered_string_adapters[type] as? StringAdapter<T>
    fun <T> map(type: Class<T>): MapAdapter<T>? = registered_map_adapters[type] as? MapAdapter<T>
}

interface IAdapter<T>

/** Serialises an object into a map when required so that it may continue along the serialisation process */
abstract class MapAdapter<T>(val toMap: (T) -> Map<String, Any>, val fromMap: (Map<String, Any>) -> T): IAdapter<T>
/** Serialises an entire object into a string for inline adaptations */
abstract class StringAdapter<T>(val toString: (T) -> String, val fromString: (String) -> T): IAdapter<T>

class IntRangeAdapter: StringAdapter<IntRange>(
    { "${it.start}:${it.endInclusive}" },
    { string -> string.split(":").map(String::toInt).let { it[0]..it[1] } }
)

class IntRangeMapAdapter: MapAdapter<IntRange>(
    { mapOf("min" to it.start, "max" to it.endInclusive) },
    { (it["min"] as Int)..(it["max"] as Int) }
)