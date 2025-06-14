import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TestMoneyTokens {

    /*
        add a camel<->kebab case converter
        fix map values
     */

    lateinit var storageLayer: FakeYmlStorageLayer
    val builtConfig = CoinVaultConfig(
        "Paper Vault",
        mapOf(
            1 to CoinVaultLevel(1..5, null, 1, listOf(CoinVaultCommand("this is a command", 50.0))),
            2 to CoinVaultLevel(10..20, "new name!", commands = listOf(CoinVaultCommand("this is a second command", 10.5))),
        ),
        MoneyTokenConfig("money token name", listOf("Insert", "List", "Here"))
    )
    val expectedYML = """
coinvault-name: Paper Vault
coinvault-levels:
  1:
    min: 1
    max: 5
    max-commands: 1
    commands:
      1:
        command: this is a command
        chance: 50.0
  2:
    coinvault-name: new name!
    min: 10
    max: 20

money-token:
  name: money token name
  lore:
  - Insert
  - List
  - Here
    """.trimIndent()

    @BeforeEach
    fun setup() {
        storageLayer = FakeYmlStorageLayer()
    }

    @Test
    fun testMoneyTokens() {
        testIO(storageLayer, builtConfig, expectedYML)
    }


    data class CoinVaultConfig(
        val coinVaultName: String,
        val coinVaultLevels: Map<Int, CoinVaultLevel>,
        val moneyTokens: MoneyTokenConfig
    )

    data class CoinVaultLevel(
        val range: IntRange,
        val levelName: String? = null,
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
}