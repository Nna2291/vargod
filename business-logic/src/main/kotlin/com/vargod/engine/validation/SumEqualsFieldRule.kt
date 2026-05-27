package com.vargod.engine.validation

import com.vargod.engine.ConstraintRule
import kotlin.math.abs

class SumEqualsFieldRule : ConstraintRule {
    override fun type(): String = TYPE
    override fun paramsSchema(): RuleParamsSchema = SCHEMA

    override fun check(values: Map<String, Any>, params: Map<String, Any>): Boolean {
        val fields = fieldsOf(params)
        val expectedField = params["expectedField"] as? String
            ?: throw IllegalArgumentException("Missing 'expectedField' parameter")
        val tolerance = (params["tolerance"] as? Number)?.toDouble() ?: 1e-9
        val sum = fields.sumOf { numberOf(values[it], it) }
        val expected = numberOf(values[expectedField], expectedField)
        return abs(sum - expected) <= tolerance
    }

    companion object {
        const val TYPE = "SUM_EQUALS_FIELD"

        private val SCHEMA = RuleParamsSchema(
            fields = listOf(
                RuleParamsSchema.FieldSpec(
                    "fields", RuleParamsSchema.ParamType.LIST_OF_STRING,
                    description = "Имена полей, чья сумма проверяется.",
                ),
                RuleParamsSchema.FieldSpec(
                    "expectedField", RuleParamsSchema.ParamType.STRING,
                    description = "Имя поля, значение которого должно равняться сумме.",
                ),
                RuleParamsSchema.FieldSpec(
                    "tolerance", RuleParamsSchema.ParamType.NUMBER, required = false,
                    description = "Допустимая абсолютная погрешность. По умолчанию 1e-9.",
                ),
            ),
        )
    }
}
