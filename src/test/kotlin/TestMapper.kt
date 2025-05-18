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
            val default = TestObject("Stephen", 41, 20)
            val defaultYML = """
                name: Stephen
                age: 41
                mutableProperty: 20
                regularField: Jolly Good
            """.trimIndent()
        }
        val immutableProperty: String = "Hi!"
        var regularField: String = "Jolly Good"
    }

}