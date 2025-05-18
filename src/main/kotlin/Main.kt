import java.io.File
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import kotlin.reflect.KClass
import kotlin.reflect.KParameter

val file = File("Test.yml")

fun main() {
    testLoadingFromFile()
    testWritingFile()
}

private fun testLoadingFromFile() {
    val storage = SnakeYamlStorageLayer()

    val obj: Mapped = load(storage.load(file), Mapped::class)
    println(obj)
    println(obj.health)
}

private fun testWritingFile() {
    val storage = SnakeYamlStorageLayer()
    val mapped = Mapped("Stephen", "Somewhat", 41)
    storage.save(deserialise(mapped), file)
}

private fun testFunction() {
    val storageLayer = VirtualStorageLayer()
    val initial = Mapped("Alex", "Not anymore", 21)
    println("Initial: $initial")

    val mapping = deserialise(initial)
    println("Objects: $mapping")

    storageLayer.save(mapping, null)

//    val remapped = loadKotlin(mapping, Mapped::class)
    val remapped = load(storageLayer.load(null), Mapped::class)
    println("Remapped: $remapped")

    println(remapped.test)
}

fun <T: Any> load(objects: Map<String, Any>, clazz: KClass<T>): T {
    val constructor = clazz.constructors.first()
    val constructorParamNames = constructor.parameters.map(KParameter::name)

    val args = constructor.parameters.associateWith { param ->
        val name = param.name
        if (name != null && objects.containsKey(name)) {
            val value = objects[name]
            // Optional: handle nested deserialization here
            value
        } else {
            null
        }
    }
//        .filterNot { it.value == null && it.key.isOptional } // Removes null assignment from optional parameters.

    val constructed = constructor.callBy(args)

    clazz.java.declaredFields
        // Filter for unconstructed properties that we're prepared to load
        .filter { it.name !in constructorParamNames && !Modifier.isFinal(it.modifiers) && objects.containsKey(it.name) }
        .forEach { field -> field.withAccessible { set(constructed, objects[field.name]!!) } }

    return constructed
}

fun <T: Any> deserialise(obj: T): Map<String, Any> {
    val fields = obj::class.java.declaredFields // TODO: Only save constructor fields? OR set these properties later after instance made (only vars?)
    val constructorArgs = obj::class.constructors.first().parameters.map { it.name }
    val map = mutableMapOf<String, Any>()

    for (field in fields) {
        // Will not bother to 'remember' (non-constructor) final fields (vals) but will remember vars
        if (field.name !in constructorArgs && Modifier.isFinal(field.modifiers)) continue

        field.withAccessible {
            map[field.name] = field.get(obj)!! // TODO: I forgot but it's surely something
        }
    }

    return map
}


data class Mapped(val name: String, val hair: String, val age: Int) {
    val test = "INITIALISED"
    var health = 0.5
}

private fun Field.withAccessible(func: Field.() -> Unit) {
    if (isAccessible) func(this)
    else {
        isAccessible = true
        func(this)
        isAccessible = false
    }
}