package com.vargod.engine.validation

import com.vargod.domain.ValidationResult
import com.vargod.domain.ValidationStatus
import com.vargod.domain.Validator
import com.vargod.domain.ValidatorType
import com.vargod.engine.ConstraintRuleRegistry
import com.vargod.engine.ValidationEngine

/**
 * Единая точка применения валидаторов.
 *
 * До P2 у движка были две независимые ветки: для RANGE (вшитая численная
 * проверка) и для CONSTRAINT (через ConstraintRuleRegistry). Теперь RANGE —
 * обычное правило в реестре (см. [RangeRule]); обе ветки идут одним путём.
 * Поле Validator.checkType сохраняется ради backward compat с таблицей
 * `validators.check_type` (значения "RANGE" / "CONSTRAINT") и помогает
 * выбрать, как сформировать map параметров для правила.
 */
class RuleValidationEngine(
    private val constraintRules: ConstraintRuleRegistry,
) : ValidationEngine {

    override fun validate(values: Map<String, Any>, validators: List<Validator>): List<ValidationResult> =
        validators.map { v ->
            val (ruleType, params) = resolve(v) ?: return@map fail(
                v, "CONSTRAINT validator '${v.name}' missing 'type' in rules"
            )
            val rule = try {
                constraintRules.getByType(ruleType)
            } catch (e: IllegalArgumentException) {
                return@map fail(v, e.message ?: "Unknown rule type '$ruleType'")
            }
            try {
                if (rule.check(values, params)) pass(v) else fail(v, "Constraint '$ruleType' violated")
            } catch (e: IllegalArgumentException) {
                fail(v, e.message ?: "Constraint '$ruleType' check failed")
            }
        }

    /**
     * RANGE-валидатор хранит rules без поля `type` — оно подразумевается
     * самим Validator.checkType. Чтобы передать имя целевого поля в RangeRule,
     * добавляем в params служебное поле `field` из Validator.targetField.
     */
    private fun resolve(v: Validator): Pair<String, Map<String, Any>>? = when (v.checkType) {
        ValidatorType.RANGE -> RangeRule.TYPE to (v.rules + mapOf("field" to v.targetField))
        ValidatorType.CONSTRAINT -> (v.rules["type"] as? String)?.let { it to v.rules }
    }

    private fun pass(v: Validator) = ValidationResult(ValidationStatus.PASS, null, v.id)
    private fun fail(v: Validator, msg: String) = ValidationResult(ValidationStatus.FAIL, msg, v.id)
}
