import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.w3c.dom.DOMStringList

class TestMoneyTokens {

    /*
        fix IntRanges
        fix map values
     */

    lateinit var storageLayer: FakeYmlStorageLayer
    val builtConfig = CoinVaultConfig(
        "Paper Vault",
        mapOf(
            1 to CoinVaultLevel(Range(1..5), null, 1, listOf(CoinVaultCommand("this is a command", 50.0))),
            2 to CoinVaultLevel(Range(10..20), "new name!", commands = listOf(CoinVaultCommand("this is a second command", 10.5))),
        ),
        MoneyTokenConfig("money token name", listOf("Insert", "List", "Here"))
    )
    val expectedYML = """
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

    @BeforeEach
    fun setup() {
        Deyaml.settings.apply { camelToKebabCaseConverter = true }
        storageLayer = FakeYmlStorageLayer()
    }

    @Test
    fun testMoneyTokens() {
        testIO(storageLayer, builtConfig, expectedYML)
    }


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

    data class CoinVaultCommand(
        val cmd: String,
        val chance: Double
    )

    data class MoneyTokenConfig(
        val name: String,
        val lore: List<String>
    )

    data class Range(
        val min: Int,
        val max: Int
    ) {
        constructor(range: IntRange): this(range.start, range.endInclusive)
    }
}