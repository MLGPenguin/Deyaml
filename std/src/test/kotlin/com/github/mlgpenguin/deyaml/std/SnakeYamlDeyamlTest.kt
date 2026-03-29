package com.github.mlgpenguin.deyaml.std

import com.github.mlgpenguin.deyaml.AbstractDeyamlTest
import com.github.mlgpenguin.deyaml.Deyaml
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * Extends the shared abstract suite with SnakeYAML-specific tests.
 * All inherited tests run against the core engine; additional tests here verify
 * the actual YAML text produced by [StringStorageLayer].
 */
class SnakeYamlDeyamlTest : AbstractDeyamlTest() {

    private lateinit var storage: StringStorageLayer

    @BeforeEach
    fun setupStorage() {
        storage = StringStorageLayer()
    }

    /** Serialises [obj], checks the YAML output matches [expectedYml], then deserialises and asserts equality. */
    private inline fun <reified T : Any> testYaml(obj: T, expectedYml: String) {
        val demap = Deyaml.deserialise(obj)
        // Test YML
        storage.save(demap, "")
        assertEquals(expectedYml, storage.ymlString.trim(), "YAML output mismatch")
        // Test Serialiser
        assertEquals(obj, Deyaml.load<T>(demap))
    }

    @Test fun testSimpleObjectYaml() {
        testYaml(TestObject.default, TestObject.defaultYml)
    }

    @Test fun testNestedCollectionsYaml() {
        testYaml(
            TestCollections(listOf(1, 2), setOf(3.0, 4.0), arrayOf("5", "6"), arrayOf(7, 8), intArrayOf(9, 10)),
            """
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
        )
    }

    @Test fun testSerialiseNullFieldsYaml() {
        testYaml(BasicObject("basic object", null), "name: basic object")
    }

    @Test fun testMapsAsParametersYaml() {
        testYaml(
            TestMaps(mapOf("a" to 1, "b" to 2)),
            "map:\n  a: 1\n  b: 2"
        )
    }

    @Test fun testTopLevelMapsYaml() {
        testYaml(mapOf("a" to 1, "b" to 2), "a: 1\nb: 2")
    }

    @Test fun testFirstLayerNestedObjectsYaml() {
        testYaml(
            TestNesting(listOf(BasicObject("a", 1), BasicObject("b", null)), BasicObject("c", 3)),
            """
            objs:
            - name: a
              meta: 1
            - name: b
            otherObj:
              name: c
              meta: 3
            """.trimIndent()
        )
    }

    @Test fun testIntsYaml() {
        testYaml(TestAdapters(1, 3), "min: 1\nmax: 3")
    }

    @Test fun testDeepNestingYaml() {
        testYaml(MoreNesting(), "nested:\n  range:\n    min: -4\n    max: 100")
    }

    @Test fun testMapWithObjectValuesYaml() {
        testYaml(
            mapOf(1 to Range(1, 2), 2 to Range(2, 3)),
            "'1':\n  min: 1\n  max: 2\n'2':\n  min: 2\n  max: 3"
        )
    }

    @Test fun testMapWithStringKeysPrimitiveValuesYaml() {
        testYaml(mapOf("1" to 1, "2" to 2), "'1': 1\n'2': 2")
    }

    @Test fun testMapWithPrimitiveValuesYaml() {
        testYaml(mapOf(1 to 2, 2 to 4, 4 to 8), "'1': 2\n'2': 4\n'4': 8")
    }

    @Test fun testKebabCaseConversion() {
        Deyaml.settings.apply { camelToKebabCaseConverter = true }
        testYaml(
            KebabConfig("Paper Vault", 42),
            "vault-name: Paper Vault\nmax-level: 42"
        )
    }

    @Test fun testComplexKebabCaseConfig() {
        Deyaml.settings.apply { camelToKebabCaseConverter = true }
        val config = CoinVaultConfig(
            "Paper Vault",
            mapOf(
                1 to CoinVaultLevel(Range(1, 5), null, 1, listOf(CoinVaultCommand("this is a command", 50.0))),
                2 to CoinVaultLevel(Range(10, 20), "new name!", commands = listOf(CoinVaultCommand("this is a second command", 10.5))),
            ),
            MoneyTokenConfig("money token name", listOf("Insert", "List", "Here"))
        )
        val expectedYml = """
coin-vault-name: Paper Vault
coin-vault-levels:
  '1':
    range:
      min: 1
      max: 5
    max-commands: 1
    commands:
    - cmd: this is a command
      chance: 50.0
  '2':
    range:
      min: 10
      max: 20
    coin-vault-name: new name!
    max-commands: 1
    commands:
    - cmd: this is a second command
      chance: 10.5
money-token:
  name: money token name
  lore:
  - Insert
  - List
  - Here
        """.trimIndent()
        testYaml(config, expectedYml)
    }

    // ---- Test data specific to YAML format tests ----

    data class KebabConfig(val vaultName: String, val maxLevel: Int)

    data class CoinVaultConfig(
        val coinVaultName: String,
        val coinVaultLevels: Map<Int, CoinVaultLevel>,
        val moneyToken: MoneyTokenConfig
    )

    data class CoinVaultLevel(
        val range: Range,
        val coinVaultName: String? = null,
        val maxCommands: Int = 1,
        val commands: List<CoinVaultCommand> = listOf()
    )

    data class CoinVaultCommand(val cmd: String, val chance: Double)
    data class MoneyTokenConfig(val name: String, val lore: List<String>)
}
