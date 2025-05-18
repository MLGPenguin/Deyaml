class OldCode {
    fun <T: Any> load(objects: Map<String, String>, clazz: Class<T>): T {
//    val instance = clazz.getDeclaredConstructor().newInstance()
        val fields = clazz.declaredFields

        val argedConstructor = clazz.getDeclaredConstructor(*fields.map { it.type }.toTypedArray())
//
//    for (field in fields) {
//        field.set(instance, objects[field.name])
//    }
//    return instance
        val positions = argedConstructor.parameters.mapIndexed { index, parameter -> parameter.name to index }.toMap()
//    argedConstructor.
        val values = argedConstructor.parameters.map { objects[it.name] }.toTypedArray()
        return argedConstructor.newInstance(*values) as T
    }
}