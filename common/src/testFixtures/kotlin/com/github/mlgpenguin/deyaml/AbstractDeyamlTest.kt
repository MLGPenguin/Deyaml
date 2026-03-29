package com.github.mlgpenguin.deyaml

import com.github.mlgpenguin.deyaml.adapters.Adapters
import com.github.mlgpenguin.deyaml.adapters.StringAdapter
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.random.Random
import kotlin.test.assertEquals

/**
 * Abstract base for all Deyaml test suites. Tests here verify the core serialisation/deserialisation
 * roundtrip using an in-memory storage layer — no YAML library required.
 *
 * Subclasses inherit all tests and can add storage-specific assertions on top.
 */
abstract class AbstractDeyamlTest {

    @TempDir
    lateinit var tempDir: File

    private fun tempFile(name: String = "base-test.yml") = File(tempDir, name).also { it.createNewFile() }

    @BeforeEach
    fun resetSettings() {
        Deyaml.settings.apply { camelToKebabCaseConverter = false }
    }

    @AfterEach
    fun cleanupAdapters() {
        Adapters.unregister(TestAdapters::class.java)
    }

    /** Serialises [obj] to a map and loads it back, asserting full equality. */
    private inline fun <reified T : Any> testRoundtrip(obj: T): T {
        val layer = SnakeYamlStorageLayer()
        layer.save(Deyaml.deserialise(obj), tempFile())
        return Deyaml.load(layer.load(tempFile()))
        // Don't do this because it keeps type info in memory, biasing tests.
//        val map = Deyaml.deserialise(obj)
//        return Deyaml.load<T>(map)
    }

//    @Test fun testEnums() {
//        val enum = TestEnums.HEAD
//        assertEquals(enum, testRoundtrip(enum))
//        val enumList = listOf(TestEnums.HEAD, TestEnums.SHOULDERS)
//        assertEquals(enumList, testRoundtrip(enumList))
//    }

    @Test fun testSimpleObject() {
        assertEquals(TestObject.default, testRoundtrip(TestObject.default))
    }

    @Test fun testLoadNonConstructorFields() {
        val map = mapOf(
            "name" to "Steve", "age" to 41, "mutableProperty" to 20,
            "enumTest" to "HEAD", "regularField" to "TEST"
        )
        assertEquals("TEST", Deyaml.load<TestObject>(map).regularField)
    }

    @Test fun testObjectsWithDefaultProperties() {
        val map = mapOf("name" to "Steve", "age" to 41, "enumTest" to "HEAD")
        assertDoesNotThrow { Deyaml.load<TestObject>(map) }
    }

    @Test fun testNestedCollectionsWithPrimitives() {
        val obj = TestCollections(listOf(1, 2), setOf(3.0, 4.0), arrayOf("5", "6"), arrayOf(7, 8), intArrayOf(9, 10))
        assertEquals(obj, testRoundtrip(obj))
    }

    @Test fun testTopLevelCollections() {
        assertThrows<IllegalArgumentException> { Deyaml.deserialise(listOf(1, 2, 3)) }
    }

    @Test fun testSerialiseNullFields() {
        assertEquals(BasicObject("basic object", null), testRoundtrip(BasicObject("basic object", null)))
    }

    @Test fun testMapsAsParameters() {
        assertEquals(TestMaps(mapOf("a" to 1, "b" to 2)), testRoundtrip(TestMaps(mapOf("a" to 1, "b" to 2))))
    }

    @Test fun testTopLevelMaps() {
        assertEquals(mapOf("a" to 1, "b" to 2), testRoundtrip(mapOf("a" to 1, "b" to 2)))
    }

    @Test fun testFirstLayerNestedObjects() {
        val obj = TestNesting(listOf(BasicObject("a", 1), BasicObject("b", null)), BasicObject("c", 3))
        assertEquals(obj, testRoundtrip(obj))
    }

    @Test fun testInts() {
        assertEquals(TestAdapters(1, 3), testRoundtrip(TestAdapters(1, 3)))
    }

    @Test fun testDeepNesting() {
        assertEquals(MoreNesting(), testRoundtrip(MoreNesting()))
    }

    @Test fun testStringAdapters() {
        Deyaml.registerAdapter(TestAdapters::class.java, object : StringAdapter<TestAdapters>(
            { "${it.min}:${it.max}" },
            { s -> s.split(":").let { TestAdapters(it[0].toInt(), it[1].toInt()) } }
        ) {})
        assertEquals(TestAdaptersNonTopLevel(), testRoundtrip(TestAdaptersNonTopLevel()))
    }

    @Test fun testMapWithObjectValuesAndMapConverters() {
        val obj = mapOf(1 to Range(1, 2), 2 to Range(2, 3))
        assertEquals(obj, testRoundtrip(obj))
    }

    @Test fun testMapWithStringKeysPrimitiveValues() {
        assertEquals(mapOf("1" to 1, "2" to 2), testRoundtrip(mapOf("1" to 1, "2" to 2)))
    }

    @Test fun testMapWithPrimitiveValues() {
        assertEquals(mapOf(1 to 2, 2 to 4, 4 to 8), testRoundtrip(mapOf(1 to 2, 2 to 4, 4 to 8)))
    }

    // ---- Shared test data ----

    enum class TestEnums { HEAD, SHOULDERS, KNEES, TOES }

    data class TestObject(
        val name: String,
        val age: Int,
        var mutableProperty: Int = Random.nextInt(100),
        val enumTest: TestEnums
    ) {
        companion object {
            val default = TestObject("Steve", 41, 20, TestEnums.HEAD)
            val defaultYml = """
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
            return list == other.list && set == other.set
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

    data class TestAdapters(val min: Int, val max: Int = 100)
    data class TestAdaptersNonTopLevel(val range: TestAdapters = TestAdapters(-4))
    data class Range(val min: Int, val max: Int) {
        constructor(intRange: IntRange) : this(intRange.start, intRange.endInclusive)
    }
    data class SomeRange(val range: Range = Range(-4, 100))
    data class MoreNesting(val nested: SomeRange = SomeRange())
}
