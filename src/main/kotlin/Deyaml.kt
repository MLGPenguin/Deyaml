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
        - Nested Objects
        - Nested Lists in Maps of Objects? (Map<String, List<Object>>) - Recursion :pensive:
        - Nested Sets D: (Map<String, Set<*>>)
        - Bukkit Storage Layer (compileonly & testimpl)
     */

    fun <T: Any> load(objects: Map<String, Any>, clazz: KClass<T>): T {
        if (Map::class.isSuperclassOf(clazz)) {
            // Since objects is a Map<String, Any>, we assume the keys are strings and that the YML lib converted the rest :pray:
            // Definitely going to break once nesting starts :sob:
            return clazz.cast(objects)
        }

        val constructor = clazz.constructors.first()
        val constructorParamNames = constructor.parameters.map(KParameter::name)

        val args = constructor.parameters.associateWith { param ->
            val name = param.name
            if (name == null || !objects.containsKey(name)) return@associateWith null

            var value = objects[name]!!

            // Value is a map but clazz is not a map (We know clazz is not map because would have returned already)
            // * Handle basic nested objects
            if (value is Map<*, *>) {
                value = load(value as Map<String, Any>, param.type.jvmErasure)
            }

            // Handle Objects nested in collections
            if (value is Collection<*> && value.isNotEmpty() && value.first() is Map<*, *>) {
                value = value.map { load(it as Map<String, Any>, param.type.arguments.first().type?.jvmErasure!!) }
            }

            // Handle type conversions
            when (param.type.jvmErasure) {
                Set::class -> value = (value as List<*>).toSet()
            }

            // Handle Arrays being primitive and not holding types :|
            if (param.type.jvmErasure.java.isArray) {
                val componentType = (param.type.classifier as KClass<*>).java.componentType
                val array = Array.newInstance(componentType, (value as List<*>).size)
                value.forEachIndexed { index, element -> Array.set(array, index, element) }
                value = array
            }

            // Handle Enums being unfriendly :(
            if (param.type.jvmErasure.java.isEnum) {
                value = param.type.jvmErasure.java.enumConstants.first { (it as Enum<*>).name == value }
            }

            // Patch for primitive arrays
            if (value is IntArray && param.type.arguments.isNotEmpty()) {
                value = Array<Int>(value.size) { value[it] }
            }

            // Optional: handle nested deserialization here

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
        if (obj is Collection<*>) throw IllegalArgumentException("Cannot deserialise a Collection! Please use a declaring object instead")

        val fields = obj::class.java.declaredFields
        val constructorArgs = obj::class.constructors.first().parameters.map { it.name }
        val map = mutableMapOf<String, Any>()

        if (obj is Map<*, *>) { // TODO: IF first param is a string: all good, otherwise need to register a converter or something? idk
            if (obj.keys.first() is String) { // ASSUME ALL KEYS ARE STRINGS IF ANY MATCH
                return obj as Map<String, Any> // TODO: Reify or put clazz as parameter..  Why?
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

            // Manually deserialise certain types to define specific behaviour.
            when {
                Collection::class.java.isAssignableFrom(javaclass) -> {
                    val col = map[field.name] as Collection<*>
                    if (col.isNotEmpty() && shouldDeserialiseType(col.first()!!.javaClass)) {
                        map[field.name] = col.map { deserialise(it!!) }
                    }
                }
//                javaclass.isArray -> map[field.name] =
            }

            // Automatically deserialise any other type that can be deserialised.
            if (shouldDeserialiseType(javaclass)) {
                map[field.name] = deserialise(map[field.name]!!) // Recursively deconstruct objects
            }
        }

        return map
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