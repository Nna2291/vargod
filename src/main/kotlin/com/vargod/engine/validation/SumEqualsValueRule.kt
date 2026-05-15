package com.vargod.engine.validation

import com.vargod.engine.ConstraintRule
import kotlin.math.abs

class SumEqualsValueRule : ConstraintRule {
    override fun type(): String = TYPE

    override fun check(values: Map<String, Any>, params: Map<String, Any>): Boolean {
        val fields = fieldsOf(params)
        val expected = numberOf(params["expected"], "expected")
        val tolerance = (params["tolerance"] as? Number)?.toDouble() ?: DEFAULT_TOLERANCE
        val sum = fields.sumOf { numberOf(values[it], it) }
        return abs(sum - expected) <= tolerance
    }

    companion object {
        const val TYPE = "SUM_EQUALS_VALUE"
        const val DEFAULT_TOLERANCE = 1e-9
    }
}
