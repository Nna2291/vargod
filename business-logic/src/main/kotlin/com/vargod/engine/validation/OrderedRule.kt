package com.vargod.engine.validation

import com.vargod.engine.ConstraintRule

class OrderedRule : ConstraintRule {
    override fun type(): String = TYPE
    override fun paramsSchema(): RuleParamsSchema = SCHEMA

    override fun check(values: Map<String, Any>, params: Map<String, Any>): Boolean {
        val fields = fieldsOf(params)
        require(fields.size >= 2) { "ORDERED requires at least 2 fields" }
        val direction = (params["direction"] as? String)?.uppercase()
            ?: throw IllegalArgumentException("Missing 'direction' (ASCENDING or DESCENDING)")
        val seq = fields.map { numberOf(values[it], it) }
        return when (direction) {
            "ASCENDING" -> seq.zipWithNext().all { (a, b) -> a < b }
            "DESCENDING" -> seq.zipWithNext().all { (a, b) -> a > b }
            else -> throw IllegalArgumentException("Unknown direction '$direction', expected ASCENDING or DESCENDING")
        }
    }

    override fun validateParams(params: Map<String, Any>): List<String> {
        val errors = super.validateParams(params).toMutableList()
        val fields = params["fields"]
        if (fields is List<*> && fields.size < 2) {
            errors += "ORDERED requires at least 2 fields"
        }
        return errors
    }

    companion object {
        const val TYPE = "ORDERED"

        private val SCHEMA = RuleParamsSchema(
            fields = listOf(
                RuleParamsSchema.FieldSpec(
                    "fields", RuleParamsSchema.ParamType.LIST_OF_STRING,
                    description = "Имена полей в желаемом порядке (минимум 2).",
                ),
                RuleParamsSchema.FieldSpec(
                    "direction", RuleParamsSchema.ParamType.STRING,
                    description = "Направление: ASCENDING (строго возрастает) или DESCENDING.",
                    allowedValues = listOf("ASCENDING", "DESCENDING"),
                ),
            ),
        )
    }
}
