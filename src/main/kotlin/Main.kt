import kotlin.jvm.java
import kotlin.reflect.KClass


fun main() {
    val initial = Mapped("Alex", "Not anymore", 21)
    println("Initial: $initial")
    val mapping = deserialise(initial)
    println("Objects: $mapping")
    val remapped = loadKotlin(mapping, Mapped::class)
    println("Remapped: $remapped")

    println(remapped.test)
}

fun <T: Any> loadKotlin(objects: Map<String, Any>, clazz: KClass<T>): T {
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

fun <T> deserialise(obj: T): Map<String, Any> {
    val fields = obj!!::class.java.declaredFields
    val map = mutableMapOf<String, Any>()

    for (field in fields) {
        val accessible = field.canAccess(obj)
        field.isAccessible = true
        map[field.name] = field.get(obj)
        field.isAccessible = accessible
    }

    return map
}


data class Mapped(val name: String, val hair: String, val age: Int) {
    val test = "INITIALISED"
}