package com.vargod.engine.validation

import com.vargod.engine.ConstraintRule

/**
 * Числовой диапазон [min, max] (включая/исключая концы — через minExclusive/maxExclusive).
 *
 * До P2 эта логика была вшита в RuleValidationEngine.applyRangeCheck и не была
 * видна через ConstraintRuleRegistry — admin не мог получить список «доступные
 * типы правил». Теперь RANGE — обычное правило в реестре.
 *
 * Чтобы вписать «проверка одного поля» в API «check(values, params)», RANGE
 * читает имя проверяемого поля из params["field"] — это поле подставляет
 * RuleValidationEngine из Validator.targetField.
 */
class RangeRule : ConstraintRule {
    override fun type(): String = TYPE

    override fun paramsSchema(): RuleParamsSchema = SCHEMA

    override fun check(values: Map<String, Any>, params: Map<String, Any>): Boolean {
        val field = params["field"] as? String
            ?: throw IllegalArgumentException("RANGE rule expects 'field' in params (auto-filled by engine)")
        val value = values[field]
            ?: throw IllegalArgumentException("Field '$field' is missing")
        val numeric = (value as? Number)?.toDouble()
            ?: throw IllegalArgumentException("Field '$field' must be numeric, got $value")
        val min = (params["min"] as? Number)?.toDouble()
        val max = (params["max"] as? Number)?.toDouble()
        if (min == null && max == null) {
            // На рантайме это конфиг-баг: валидатор зарегистрирован без границ.
            // Поднимаем как fail, чтобы было видно, а не молча проходило.
            throw IllegalArgumentException("RANGE validator must have at least 'min' or 'max'")
        }
        val minExclusive = (params["minExclusive"] as? Boolean) ?: false
        val maxExclusive = (params["maxExclusive"] as? Boolean) ?: false
        if (min != null) {
            val ok = if (minExclusive) numeric > min else numeric >= min
            if (!ok) {
                throw IllegalArgumentException(
                    "Field '$field' = $numeric violates min=$min (exclusive=$minExclusive)"
                )
            }
        }
        if (max != null) {
            val ok = if (maxExclusive) numeric < max else numeric <= max
            if (!ok) {
                throw IllegalArgumentException(
                    "Field '$field' = $numeric violates max=$max (exclusive=$maxExclusive)"
                )
            }
        }
        return true
    }

    override fun validateParams(params: Map<String, Any>): List<String> {
        val baseErrors = super.validateParams(params).toMutableList()
        // Крос-полевая проверка: хотя бы один из min/max должен быть задан.
        if (params["min"] == null && params["max"] == null) {
            baseErrors += "RANGE requires at least one of 'min' or 'max'"
        }
        // Если оба заданы — min не должен превышать max.
        val min = (params["min"] as? Number)?.toDouble()
        val max = (params["max"] as? Number)?.toDouble()
        if (min != null && max != null && min > max) {
            baseErrors += "RANGE 'min' ($min) must not exceed 'max' ($max)"
        }
        return baseErrors
    }

    companion object {
        const val TYPE = "RANGE"

        private val SCHEMA = RuleParamsSchema(
            fields = listOf(
                RuleParamsSchema.FieldSpec("min", RuleParamsSchema.ParamType.NUMBER, required = false,
                    description = "Нижняя граница. Если не задана — диапазон не ограничен снизу."),
                RuleParamsSchema.FieldSpec("max", RuleParamsSchema.ParamType.NUMBER, required = false,
                    description = "Верхняя граница. Если не задана — диапазон не ограничен сверху."),
                RuleParamsSchema.FieldSpec("minExclusive", RuleParamsSchema.ParamType.BOOLEAN, required = false,
                    description = "Если true — нижняя граница строгая (>) вместо нестрогой (>=)."),
                RuleParamsSchema.FieldSpec("maxExclusive", RuleParamsSchema.ParamType.BOOLEAN, required = false,
                    description = "Если true — верхняя граница строгая (<) вместо нестрогой (<=)."),
            ),
        )
    }
}
