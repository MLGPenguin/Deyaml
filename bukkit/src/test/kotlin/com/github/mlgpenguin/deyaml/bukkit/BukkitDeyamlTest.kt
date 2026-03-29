package com.github.mlgpenguin.deyaml.bukkit

import com.github.mlgpenguin.deyaml.AbstractDeyamlTest
import com.github.mlgpenguin.deyaml.Deyaml
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals

/**
 * Extends the shared abstract suite with Bukkit-specific roundtrip tests.
 * All inherited tests verify the core engine; tests here write real files via [BukkitStorageLayer].
 */
class BukkitDeyamlTest : AbstractDeyamlTest() {

    @TempDir
    lateinit var tempDir: File

    private fun tempFile(name: String = "test.yml") = File(tempDir, name).also { it.createNewFile() }

    private inline fun <reified T : Any> testBukkitRoundtrip(obj: T) {
        val storage = BukkitStorageLayer()
        val file = tempFile()
        storage.save(Deyaml.deserialise(obj), file)
        println(file.readText())
        assertEquals(obj, Deyaml.load<T>(storage.load(file)))
    }

    @Test fun testSimpleObjectViaBukkit() {
        testBukkitRoundtrip(TestObject.default)
    }

    @Test fun testNestedObjectsViaBukkit() {
        testBukkitRoundtrip(TestNesting(listOf(BasicObject("a", 1), BasicObject("b", null)), BasicObject("c", 3)))
    }

    @Test fun testMapsAsParametersViaBukkit() {
        testBukkitRoundtrip(TestMaps(mapOf("a" to 1, "b" to 2)))
    }

    @Test fun testNullFieldsViaBukkit() {
        testBukkitRoundtrip(BasicObject("basic object", null))
    }
}
