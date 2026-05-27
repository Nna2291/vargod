package com.vargod.engine

import com.vargod.engine.validation.RuleParamsSchema

/**
 * Параметрическое правило валидации. Каждое реализующее правило объявляет:
 *  - стабильный строковый [type] для хранения в БД (поле `validators.check_type`
 *    или `rules.type` для CONSTRAINT-валидаторов);
 *  - [paramsSchema] — декларацию ожидаемых параметров, по которой admin UI
 *    строит форму, а ValidatorAdminService проверяет payload до сохранения;
 *  - [check] — собственно проверку значений, выполняемую перед запуском
 *    эксперимента.
 *
 * Намеренно НЕ предусматриваем выполнение произвольного пользовательского кода
 * (SpEL/Kotlin scripting): такие движки невозможно безопасно изолировать в
 * рамках одного JVM-процесса. Расширяемость идёт через добавление новых
 * реализаций [ConstraintRule] и регистрацию их в [ConstraintRuleRegistry].
 */
interface ConstraintRule {
    fun type(): String
    fun paramsSchema(): RuleParamsSchema
    fun check(values: Map<String, Any>, params: Map<String, Any>): Boolean

    /**
     * Дефолт делегирует [RuleParamsSchema.validate]. Правило может переопределить
     * для добавления крос-полевых ограничений (например, у RANGE — «хотя бы один
     * из min/max должен быть задан»).
     */
    fun validateParams(params: Map<String, Any>): List<String> = paramsSchema().validate(params)
}
