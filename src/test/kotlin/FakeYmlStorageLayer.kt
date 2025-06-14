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
