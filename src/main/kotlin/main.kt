import kotlin.jvm.java
import kotlin.reflect.KClass


fun main() {
    val initial = Mapped("Alex", "Not anymore")
    println("Initial: $initial")
    val mapping = deserialise(initial)
    println("Objects: $mapping")
    val remapped = loadKotlin(mapping, Mapped::class)
    println("Remapped: $remapped")
}

fun <T: Any> load(objects: Map<String, String>, clazz: Class<T>): T {
//    val instance = clazz.getDeclaredConstructor().newInstance()
    val fields = clazz.declaredFields

    val argedConstructor = clazz.getDeclaredConstructor(*fields.map { it.type }.toTypedArray())
//
//    for (field in fields) {
//        field.set(instance, objects[field.name])
//    }
//    return instance
    val positions = argedConstructor.parameters.mapIndexed { index, parameter -> parameter.name to index }.toMap()
//    argedConstructor.
    val values = argedConstructor.parameters.map { objects[it.name] }.toTypedArray()
    return argedConstructor.newInstance(*values) as T
}


fun <T: Any> loadKotlin(objects: Map<String, String>, clazz: KClass<T>): T {
    val constructor = clazz.constructors.first()

    val args = constructor.parameters.associateWith { param ->
        val name = param.name
        if (name != null && objects.containsKey(name)) {
            val value = objects[name]
            // Optional: handle nested deserialization here
            value
        } else {
            null // or param.defaultValue if you want to support defaults
        }
    }

    return constructor.callBy(args)
}

fun <T> deserialise(obj: T): Map<String, String> {
    val fields = obj!!::class.java.declaredFields
    val map = mutableMapOf<String, String>()

    for (field in fields) {
        field.isAccessible = true
        map[field.name] = field.get(obj).toString()
        field.isAccessible = false
    }

    return map
}


data class Mapped(val name: String, val hair: String)