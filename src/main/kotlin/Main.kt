import org.yaml.snakeyaml.Yaml

data class OlliConfig(val lemons: Int, val carrots: Int, val dog: OllisDog)
data class OllisDog(val legs: Int, val height: Int, val name: String, val favouriteNumbers: List<Int>)

fun main() {
    val config = OlliConfig(6, 5, OllisDog(4, 10, "sauce", listOf(1, 2, 3)))
    val yml = Deyaml.deserialise(config)

    val YAML = Yaml()
    val parsed = YAML.dump(yml)
    println(parsed)

    val deserial =  Deyaml.load<OlliConfig>(yml)
    println(deserial)

}