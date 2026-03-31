package com.github.mlgpenguin.deyaml

private val camelRegex = Regex("([A-Z])")
private val kebabRegex = Regex("-([a-z])")

fun String.toKebabCase() = camelRegex.replace(this) { r -> "-${r.groups[1]!!.value.lowercase()}" }
fun String.toCamelCase() = kebabRegex.replace(this) { r -> r.groups[1]!!.value.uppercase() }
