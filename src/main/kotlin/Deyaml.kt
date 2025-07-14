import adapters.Adapters
import adapters.IAdapter
import adapters.MapKeyConverters
import java.lang.constant.ConstantDesc
import java.lang.reflect.Array
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.cast
import kotlin.reflect.full.isSuperclassOf
import kotlin.reflect.jvm.jvmErasure

object Deyaml { // TODO: Convert to class with storage layer property

    /* TODO: stuff
        - obj(field=Map<String, Object>)
        - Nested Lists in Maps of Objects? (Map<String, List<Object>>) - Recursion :pensive:
        - Nested Sets D: (Map<String, Set<*>>)
        - Bukkit Storage Layer (compileonly & testimpl)
     */

    data class Settings(var camelToKebabCaseConverter: Boolean = false)
    val settings = Settings()

    fun <T> registerAdapter(clazz: Class<T>, adapter: IAdapter<T>) {
        Adapters.register(clazz, adapter)
    }

    fun <T: Any> load(objects: Map<String, Any>, clazz: KClass<T>): T {
        // Handle kebab case converter
        val objects = objects.mapKeys { (k, _) -> k.toCamelCase() }

        if (Map::class.isSuperclassOf(clazz)) {
            // Since objects is a Map<String, Any>, we assume the keys are strings and that the YML lib converted the rest :pray:
            // Definitely going to break once nesting starts :sob:
            return clazz.cast(objects)

            // Just need to get a value instance and recursively load here (.map)
        }

        val constructor = clazz.constructors.first()
        val constructorParamNames = constructor.parameters.map(KParameter::name)

        val args = constructor.parameters.associateWith { param ->
            val name = param.name
            if (name == null || !objects.containsKey(name)) return@associateWith null

            val kclass = param.type.jvmErasure
            var value = objects[name]!!

            // Value is a map but clazz is not a map (We know clazz is not map because would have returned already)
            // * Handle basic nested objects
            if (value is Map<*, *>) {
                value = load(value as Map<String, Any>, kclass)
            }

            // Handle Objects nested in collections
            if (value is Collection<*> && value.isNotEmpty() && value.first() is Map<*, *>) {
                value = value.map { load(it as Map<String, Any>, param.type.arguments.first().type?.jvmErasure!!) }
            }

            // Handle type conversions
            when (kclass) {
                Set::class -> value = (value as List<*>).toSet()
            }

            // Handle Arrays being primitive and not holding types :|
            if (kclass.java.isArray) {
                val componentType = (param.type.classifier as KClass<*>).java.componentType
                val array = Array.newInstance(componentType, (value as List<*>).size)
                value.forEachIndexed { index, element -> Array.set(array, index, element) }
                value = array
            }

            // Handle Enums being unfriendly :(
            if (kclass.java.isEnum) {
                value = kclass.java.enumConstants.first { (it as Enum<*>).name == value }
            }

            // Patch for primitive arrays
            if (value is IntArray && param.type.arguments.isNotEmpty()) {
                value = Array<Int>(value.size) { value[it] }
            }

            // Handle String Adapters
            if (value is String && kclass != String::class.java && Adapters.has(kclass.java)) {
                value = kclass.cast(Adapters.string(kclass.java)!!.fromString.invoke(value))
            }

           // // Optional: handle nested deserialization here

            value
        }
            // Removes null assignment from optional parameters so that defaults work.
            .filterNot { it.value == null && it.key.isOptional }

        val constructed = constructor.callBy(args)

        clazz.java.declaredFields
            // Filter for unconstructed properties that we're prepared to load
            .filter { it.name !in constructorParamNames && !Modifier.isFinal(it.modifiers) && objects.containsKey(it.name) }
            .forEach { field -> field.withAccessible { set(constructed, objects[field.name]!!) } }

        return constructed
    }

    private val nonRecursiveTypes = arrayOf(ConstantDesc::class, Collection::class, Map::class)

    fun <T: Any> deserialise(obj: T): Map<String, Any> {
        // TODO: Just flat??? map to List<Map<String, Any>>
        if (obj is Collection<*>) throw IllegalArgumentException("Cannot deserialise a Collection yet! Please use a declaring object instead")

        val fields = obj::class.java.declaredFields
        val constructorArgs = obj::class.constructors.first().parameters.map { it.name }
        val map = mutableMapOf<String, Any>()

        if (obj is Map<*, *>) { // TODO: IF first param is a string: all good, otherwise need to register a converter or something? idk
            val key = obj.keys.first() ?: throw IllegalArgumentException("Sorry, we don't know how to handle empty maps yet") // TODO: Handle empty maps
            if (key is String) { // assume all keys match the first one
                // TODO: Map Values????? :o - this is only if the whole object is a map
                return obj as Map<String, Any> // TODO: Reify or put clazz as parameter..  Why? (So that we can handle empty maps)
            } else if (MapKeyConverters.has(key.javaClass)) {
                val converter = MapKeyConverters.of(obj.javaClass)!!
                // TODO: Recursion
                return obj.map { (k, v) -> converter.toString.invoke(obj.javaClass.cast(k)) to v }.toMap() as Map<String, Any>
            }
        }

        for (field in fields) {
            // Will not bother to 'remember'/save (non-constructor) final fields (vals) but will remember vars
            if (field.name !in constructorArgs && Modifier.isFinal(field.modifiers)) continue

            // Get the value of this field - omit null fields, they will be automatically inferred.
            field.withAccessible {
                map[field.name] = field.get(obj) ?: return@withAccessible
            }

            val javaclass = map[field.name]?.javaClass ?: continue

            // Manually deserialise certain types of fields to define specific behaviour.
            when {
                // Deserialise each element inside the collection individually
                Collection::class.java.isAssignableFrom(javaclass) -> {
                    val col = map[field.name] as Collection<*>
                    if (col.isNotEmpty() && shouldDeserialiseType(col.first()!!.javaClass)) {
                        map[field.name] = col.map { deserialise(it!!) }
                    }
                }
                // Convert Map keys to strings if possible and fully deserialise Map values.
                Map::class.java.isAssignableFrom(javaclass) -> {
                    var fieldMap = (map[field.name] as? Map<out Any, Any>)?.takeIf { it.isNotEmpty() } ?: continue
                    val (k, v) = fieldMap.entries.first()

                    if (k !is String) {
                        val converter = MapKeyConverters.of(k.javaClass) ?: throw IllegalArgumentException("${k.javaClass.name} is not a valid Map key for deserialisation. Please add a MapKeyConverter")
                        fieldMap = fieldMap.mapKeys { converter.toString.invoke(it.key) }
                    }

                    if (shouldDeserialiseType(v.javaClass)) {
                        fieldMap = fieldMap.mapValues { (_, v) -> deserialise(v) }
                    }

                    map[field.name] = fieldMap as Map<String, Any>
                }
//                javaclass.isArray -> map[field.name] =
            }

            // Handle String Adapters
            if (Adapters.has(javaclass)) {
                map[field.name] = Adapters.string(javaclass)!!.toString.invoke(map[field.name]!!)
            }

            // Automatically deserialise any other type that can be deserialised.
            else if (shouldDeserialiseType(javaclass)) {
                map[field.name] = deserialise(map[field.name]!!) // Recursively deconstruct objects
            }
        }

        // Apply kebab case conversion
        val finalMap = if (settings.camelToKebabCaseConverter) map.mapKeys { (k, _) -> k.toKebabCase() } else map
        println(finalMap)
        return finalMap
    }

    private fun Field.withAccessible(func: Field.() -> Unit) {
        if (isAccessible) func(this)
        else {
            isAccessible = true
            func(this)
            isAccessible = false
        }
    }

    private fun <T: Any> shouldDeserialiseType(clazz: Class<T>): Boolean {
        return !(clazz.isPrimitive || clazz.isArray || clazz.isEnum || nonRecursiveTypes.any { it.java.isAssignableFrom(clazz) })
    }
}