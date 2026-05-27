package com.vargod.engine.validation

/**
 * Декларация ожидаемых параметров правила. Используется на двух уровнях:
 *  1) Admin UI (P5) рендерит форму создания/редактирования валидатора по этой схеме —
 *     добавление нового типа правила на бэкенде автоматически появляется в UI.
 *  2) ValidatorAdminService (P3a) валидирует payload перед сохранением, чтобы 422
 *     прилетал в момент создания валидатора, а не при попытке запустить эксперимент.
 */
data class RuleParamsSchema(val fields: List<FieldSpec>) {

    data class FieldSpec(
        val name: String,
        val type: ParamType,
        val required: Boolean = true,
        val description: String = "",
        /** Только для ENUM-полей (или STRING с фиксированным набором). */
        val allowedValues: List<String> = emptyList(),
    )

    enum class ParamType { NUMBER, INTEGER, STRING, BOOLEAN, LIST_OF_STRING }

    /** Универсальная проверка: тип, наличие required, ENUM-набор. Возвращает список ошибок. */
    fun validate(params: Map<String, Any>): List<String> {
        val errors = mutableListOf<String>()
        for (spec in fields) {
            val value = params[spec.name]
            if (value == null) {
                if (spec.required) errors += "Missing required parameter '${spec.name}'"
                continue
            }
            val typeError = checkType(spec, value)
            if (typeError != null) errors += typeError
            if (spec.allowedValues.isNotEmpty() && value is String && value !in spec.allowedValues) {
                errors += "Parameter '${spec.name}' must be one of ${spec.allowedValues}, got '$value'"
            }
        }
        return errors
    }

    private fun checkType(spec: FieldSpec, value: Any): String? = when (spec.type) {
        ParamType.NUMBER -> if (value is Number) null
            else "Parameter '${spec.name}' must be a number, got ${value.javaClass.simpleName}"
        ParamType.INTEGER -> when (value) {
            is Int, is Long, is Short, is Byte -> null
            is Number -> if (value.toDouble() == value.toLong().toDouble()) null
                else "Parameter '${spec.name}' must be an integer, got $value"
            else -> "Parameter '${spec.name}' must be an integer"
        }
        ParamType.STRING -> if (value is String) null
            else "Parameter '${spec.name}' must be a string"
        ParamType.BOOLEAN -> if (value is Boolean) null
            else "Parameter '${spec.name}' must be a boolean"
        ParamType.LIST_OF_STRING -> when {
            value !is List<*> -> "Parameter '${spec.name}' must be a list of strings"
            value.any { it !is String } -> "Parameter '${spec.name}' contains non-string elements"
            else -> null
        }
    }
}
