package com.vargod.engine.validation

import com.vargod.domain.ValidationResult
import com.vargod.domain.ValidationStatus
import com.vargod.domain.Validator
import com.vargod.domain.ValidatorType
import com.vargod.engine.ConstraintRuleRegistry
import com.vargod.engine.ValidationEngine

class RuleValidationEngine(
    private val constraintRules: ConstraintRuleRegistry,
) : ValidationEngine {

    override fun validate(values: Map<String, Any>, validators: List<Validator>): List<ValidationResult> =
        validators.map { v ->
            when (v.checkType) {
                ValidatorType.RANGE -> applyRangeCheck(v, values[v.targetField])
                ValidatorType.CONSTRAINT -> applyConstraintCheck(v, values)
            }
        }

    private fun applyRangeCheck(v: Validator, value: Any?): ValidationResult {
        if (value == null) {
            return fail(v, "Field '${v.targetField}' is missing")
        }
        val numeric = (value as? Number)?.toDouble()
            ?: return fail(v, "Field '${v.targetField}' must be numeric, got $value")
        val min = (v.rules["min"] as? Number)?.toDouble()
        val max = (v.rules["max"] as? Number)?.toDouble()
        if (min == null && max == null) {
            return fail(v, "RANGE validator must have at least 'min' or 'max'")
        }
        val minExclusive = (v.rules["minExclusive"] as? Boolean) ?: false
        val maxExclusive = (v.rules["maxExclusive"] as? Boolean) ?: false
        if (min != null) {
            val passes = if (minExclusive) numeric > min else numeric >= min
            if (!passes) {
                return fail(v, "Field '${v.targetField}' = $numeric violates min=$min (exclusive=$minExclusive)")
            }
        }
        if (max != null) {
            val passes = if (maxExclusive) numeric < max else numeric <= max
            if (!passes) {
                return fail(v, "Field '${v.targetField}' = $numeric violates max=$max (exclusive=$maxExclusive)")
            }
        }
        return pass(v)
    }

    private fun applyConstraintCheck(v: Validator, values: Map<String, Any>): ValidationResult {
        val type = v.rules["type"] as? String
            ?: return fail(v, "CONSTRAINT validator missing 'type'")
        val rule = try {
            constraintRules.getByType(type)
        } catch (e: IllegalArgumentException) {
            return fail(v, e.message ?: "Unknown rule type '$type'")
        }
        return try {
            if (rule.check(values, v.rules)) pass(v) else fail(v, "Constraint '$type' violated")
        } catch (e: IllegalArgumentException) {
            fail(v, e.message ?: "Constraint '$type' check failed")
        }
    }

    private fun pass(v: Validator) = ValidationResult(ValidationStatus.PASS, null, v.id)
    private fun fail(v: Validator, msg: String) = ValidationResult(ValidationStatus.FAIL, msg, v.id)
}
