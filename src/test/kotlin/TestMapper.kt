import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import kotlin.jvm.java
import kotlin.random.Random
import kotlin.test.assertEquals

class TestMapper {

    lateinit var storageLayer: FakeYmlStorageLayer

    @BeforeEach
    fun setup() {
        storageLayer = FakeYmlStorageLayer()
    }

    @Test fun testSimpleObject() {
        testIO(TestObject.default, TestObject.defaultYML)
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

    @Test fun testCollections() {
        val yml = """
            list:
            - 1
            - 2
            set:
            - 3
            - 4
            array:
            - '5'
            - '6'
            arrayints:
            - 7
            - 8
            intarray:
            - 9
            - 10
        """.trimIndent()
        testIO(TestCollections(listOf(1, 2), setOf(3, 4), arrayOf("5", "6"), arrayOf(7, 8), intArrayOf(9, 10)), yml)
    }

//    @Test fun testTopLevelMaps() {}
//    @Test fun testMapsAsParameters() {}



    inline fun <reified T : Any> testIO(obj: T, expectedYml: String) {
        storageLayer.save(Deyaml.deserialise(obj), null)
        assertEquals(expectedYml, storageLayer.ymlstring.trim())

        val map = storageLayer.load(null)
        assertEquals(obj, Deyaml.load(map, T::class))
    }

    class TestCollections(
        val list: List<Int>,
        val set: Set<Int>,
        val array: Array<String>,
        val arrayints: Array<Int>,
        val intarray: IntArray
    ) {

        override fun equals(other: Any?): Boolean {
            if (other !is TestCollections) return false
            return list == other.list
                    && set == other.set
                    && array.contentEquals(other.array)
                    && arrayints.contentEquals(other.arrayints)
                    && intarray.contentEquals(other.intarray)
        }

        override fun hashCode(): Int {
            var result = list.hashCode()
            result = 31 * result + set.hashCode()
            result = 31 * result + array.contentHashCode()
            result = 31 * result + arrayints.contentHashCode()
            result = 31 * result + intarray.contentHashCode()
            return result
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

    class FakeYmlStorageLayer(): StorageLayer<String?> {
        val yml = SnakeYamlStorageLayer().yml
        var ymlstring: String = ""

        override fun save(obj: Map<String, Any>, loc: String?) {
            ymlstring = loc ?: yml.dumpAsMap(obj)
        }

        override fun load(from: String?): Map<String, Any> {
            return yml.load(from ?: ymlstring)
        }
    }


}