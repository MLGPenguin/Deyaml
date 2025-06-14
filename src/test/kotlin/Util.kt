import kotlin.test.assertEquals

inline fun <reified T : Any> testIO(storage: FakeYmlStorageLayer, obj: T, expectedYml: String) {
    storage.save(Deyaml.deserialise(obj), null)
    assertEquals(expectedYml, storage.ymlstring.trim())

    val map = storage.load(null)
    assertEquals(obj, Deyaml.load(map, T::class))
}