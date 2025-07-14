import adapters.Adapters
import adapters.StringAdapter
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import kotlin.random.Random
import kotlin.test.assertEquals

class TestMapper {

    lateinit var storageLayer: FakeYmlStorageLayer

    @BeforeEach
    fun setup() {
        Deyaml.settings.apply { camelToKebabCaseConverter = false }
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

    @Test fun testNestedCollectionsWithPrimitives() {
        val yml = """
            list:
            - 1
            - 2
            set:
            - 3.0
            - 4.0
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
        testIO(TestCollections(listOf(1, 2), setOf(3.0, 4.0), arrayOf("5", "6"), arrayOf(7, 8), intArrayOf(9, 10)), yml)
    }

    @Test fun testTopLevelCollections() {
        assertThrows<IllegalArgumentException> { Deyaml.deserialise(listOf(1,2,3)) }
    }

    @Test fun testSerialiseNullFields() {
        testIO(BasicObject("basic object", null), "name: basic object")
    }

    @Test fun testMapsAsParameters() {
        val yml = """
            map:
              a: 1
              b: 2
        """.trimIndent()
        testIO(TestMaps(mapOf("a" to 1, "b" to 2)), yml)
    }

    @Test fun testTopLevelMapsWithPrimitives() {
        testIO(mapOf("a" to 1, "b" to 2), "a: 1\nb: 2")
    }

    @Test fun testFirstLayerNestedObjects() {
        testIO(TestNesting(listOf(BasicObject("a", 1), BasicObject("b", null)), BasicObject("c", 3)), """
            objs:
            - name: a
              meta: 1
            - name: b
            otherObj:
              name: c
              meta: 3
        """.trimIndent())
    }

    @Test fun testInts() {
        testIO(TestAdapters(1, 3), "min: 1\nmax: 3")
    }

    @Test fun testDeepNesting() {
        testIO(MoreNesting(), "nested:\n  range:\n    min: -4\n    max: 100")
    }

    @Test fun testStringAdapters() {
        Deyaml.registerAdapter(TestAdapters::class.java, object: StringAdapter<TestAdapters>(
            { "${it.min}:${it.max}" },
            { it.split(":").let { TestAdapters(it[0].toInt(), it[1].toInt()) } }
        ){})
        testIO(TestAdaptersNonTopLevel(), "range: -4:100")
    }

    @Test fun testMapWithObjectValuesAndMapConverters() {
        testIO(mapOf(1 to TestMoneyTokens.Range(1, 2), 2 to TestMoneyTokens.Range(2, 3)), "'1':\n  min: 1\n  max: 2\n'2':\n  min: 2\n  max: 3")
    }

    @Test fun testMapWithPrimitiveValues() {
        testIO(mapOf(1 to 2, 2 to 4, 4 to 8), "1: 2\n2: 4\n4: 8")
    }

    inline fun <reified T : Any> testIO(obj: T, expectedYml: String) {
        storageLayer.save(Deyaml.deserialise(obj), null)
        assertEquals(expectedYml, storageLayer.ymlstring.trim())

        val map = storageLayer.load(null)
        assertEquals(obj, Deyaml.load(map, T::class))
    }

    enum class TestEnums { HEAD, SHOULDERS, KNEES, TOES }
    data class TestObject(val name: String, val age: Int, var mutableProperty: Int = Random.nextInt(100), val enumTest: TestEnums) {
        companion object {
            val default = TestObject("Steve", 41, 20, TestEnums.HEAD)
            val defaultYML = """
                name: Steve
                age: 41
                mutableProperty: 20
                enumTest: HEAD
                regularField: Jolly Good
            """.trimIndent()
        }
        val immutableProperty: String = "Hi!"
        var regularField: String = "Jolly Good"
    }
    data class TestMaps(val map: Map<String, Int>)
    data class BasicObject(val name: String, val meta: Int?)
    data class TestNesting(val objs: List<BasicObject>, val otherObj: BasicObject)
    class TestCollections(
        val list: List<Int>,
        val set: Set<Double>,
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
    data class TestAdapters(
        val min: Int,
        val max: Int = 100
    )
    data class TestAdaptersNonTopLevel(
        val range: TestAdapters = TestAdapters(-4),
    )
    data class SomeRange(
        val range: TestMoneyTokens.Range = TestMoneyTokens.Range(-4, 100)
    )
    data class MoreNesting(
        val nested: SomeRange = SomeRange(),
    )




}