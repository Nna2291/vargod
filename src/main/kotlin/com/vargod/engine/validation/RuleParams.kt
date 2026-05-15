package com.vargod.engine.validation

internal fun fieldsOf(params: Map<String, Any>): List<String> {
    val raw = params["fields"] ?: throw IllegalArgumentException("Missing 'fields' parameter")
    require(raw is List<*>) { "'fields' must be a list" }
    return raw.map {
        it as? String ?: throw IllegalArgumentException("'fields' contains non-string item: $it")
    }
}

internal fun numberOf(value: Any?, name: String): Double =
    (value as? Number)?.toDouble()
        ?: throw IllegalArgumentException("Value '$name' must be a number, got $value")
