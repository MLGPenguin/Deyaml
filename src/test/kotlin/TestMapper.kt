import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.yaml.snakeyaml.Yaml
import kotlin.random.Random
import kotlin.test.assertEquals

class TestMapper {

    lateinit var storageLayer: FakeYmlStorageLayer

    @BeforeEach
    fun setup() {
        storageLayer = FakeYmlStorageLayer()
    }

    @Test fun testSimpleSerialise() {
        val test = TestObject.default

        storageLayer.save(Deyaml.deserialise(test), null)
        assertEquals(storageLayer.ymlstring.trim(), TestObject.defaultYML)
    }

    @Test fun testSimpleLoad() {
        val map = storageLayer.load(TestObject.defaultYML)

        assertEquals(TestObject.default, Deyaml.load(map, TestObject::class))
    }

    @Test fun testLoadNonConstructorFields() {
        val yml = TestObject.defaultYML.replace("regularField: Jolly Good", "regularField: TEST")
        val map = storageLayer.load(yml)
        val obj = Deyaml.load(map, TestObject::class)

        assertEquals(obj.regularField, "TEST")
    }

    @Test fun testObjectsWithDefaultProperties() {
        val yml = TestObject.defaultYML.replace("\nmutableProperty: 20", "")
        val map = storageLayer.load(yml)

        assertDoesNotThrow { Deyaml.load(map, TestObject::class) }
    }

    @Test fun testLists() {
        testIterable(TestIterables(listOf("Alex", "Dawson")))
    }
    
    @Test fun testSets() {
        testIterable(TestIterables(setOf("Alex", "Dawson")))
    }
    
    @Test fun testArrays() {
        val obj = TestArrays("Steve", arrayOf("Dawson", "Alex"))
        val expectedyml = """
            name: Steve
            children:
            - Dawson
            - Alex
        """.trimIndent()
        
        storageLayer.save(Deyaml.deserialise(obj), null)
        assertEquals(expectedyml, storageLayer.ymlstring.trim())

        val map = storageLayer.load(null)
        assertEquals(obj, Deyaml.load(map, TestArrays::class))
    }

    fun <T : Collection<String>> testIterable(obj: TestIterables<T>) {
        // Note the indentation of the children.
        val expectedyml = """
            children:
            - Alex
            - Dawson
        """.trimIndent()
        
        storageLayer.save(Deyaml.deserialise(obj), null)
        assertEquals(expectedyml, storageLayer.ymlstring.trim())

        val map = storageLayer.load(null)
        assertEquals(obj, Deyaml.load(map, TestIterables::class))
    }


    class FakeYmlStorageLayer(): StorageLayer<String?> {
        val yml = Yaml()
        var ymlstring: String = ""

        override fun save(obj: Map<String, Any>, loc: String?) {
            ymlstring = loc ?: yml.dumpAsMap(obj)
        }

        override fun load(from: String?): Map<String, Any> {
            return yml.load(from ?: ymlstring)
        }
    }
    data class TestObject(val name: String, val age: Int, var mutableProperty: Int = Random.nextInt(100)) {
        companion object {
            val default = TestObject("Steve", 41, 20)
            val defaultYML = """
                name: Steve
                age: 41
                mutableProperty: 20
                regularField: Jolly Good
            """.trimIndent()
        }
        val immutableProperty: String = "Hi!"
        var regularField: String = "Jolly Good"
    }

    data class TestIterables<T: Collection<String>>(val children: T)
    data class TestArrays(val name: String, val children: Array<String>) {

        override fun equals(other: Any?): Boolean {
            return if (other !is TestArrays) false
            else other.name == name &&  children.contentEquals(other.children)
        }

        override fun hashCode(): Int {
            var result = name.hashCode()
            result = 31 * result + children.contentHashCode()
            return result
        }
    }

}