import Deyaml.deserialise
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

    val obj: Mapped = Deyaml.load(storage.load(file), Mapped::class)
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
    val remapped = Deyaml.load(storageLayer.load(null), Mapped::class)
    println("Remapped: $remapped")

    println(remapped.test)
}

data class Mapped(val name: String, val hair: String, val age: Int) {
    val test = "INITIALISED"
    var health = 0.5
}