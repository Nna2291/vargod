package com.vargod.service.impl

import com.vargod.domain.NotFoundException
import com.vargod.domain.ValidationException
import com.vargod.domain.ValidationResult
import com.vargod.domain.ValidationStatus
import com.vargod.domain.Validator
import com.vargod.domain.ValidatorType
import com.vargod.domain.repository.ValidatorRepository
import com.vargod.engine.ConstraintRuleRegistry
import com.vargod.engine.validation.RangeRule
import com.vargod.service.ValidatorAdminService
import com.vargod.service.ValidatorTypeInfo
import java.util.UUID

/**
 * Реализация admin-CRUD валидаторов.
 *
 * До сохранения payload каждого валидатора прогоняется через
 * ConstraintRule.validateParams(...) — те же правила, что применит
 * RuleValidationEngine на запуске эксперимента. Это:
 *   1) перенос ошибки конфигурации с момента запуска эксперимента
 *      на момент создания валидатора (422 в admin UI, а не позже);
 *   2) единая точка истины: схема правила объявлена в самом правиле,
 *      ValidatorAdminService её не дублирует.
 *
 * Для CONSTRAINT-валидаторов тип правила лежит в rules["type"] — это
 * та же конвенция, что у RuleValidationEngine.resolve(). RANGE же
 * имеет фиксированный type=RangeRule.TYPE и читает params как есть.
 */
class ValidatorAdminServiceImpl(
    private val validatorRepository: ValidatorRepository,
    private val constraintRuleRegistry: ConstraintRuleRegistry,
) : ValidatorAdminService {

    override fun list(modelTemplateId: UUID?): List<Validator> =
        validatorRepository.findAll(modelTemplateId)

    override fun get(id: UUID): Validator =
        validatorRepository.findById(id) ?: throw NotFoundException("Validator", id)

    override fun create(validator: Validator, modelTemplateId: UUID, createdBy: UUID?): Validator {
        validateRules(validator)
        return validatorRepository.save(validator, modelTemplateId, createdBy)
    }

    override fun update(validator: Validator): Validator {
        // 404 если нет — иначе update тихо вернёт ошибку из репозитория.
        validatorRepository.findById(validator.id) ?: throw NotFoundException("Validator", validator.id)
        validateRules(validator)
        return validatorRepository.update(validator)
    }

    override fun delete(id: UUID) {
        validatorRepository.findById(id) ?: throw NotFoundException("Validator", id)
        validatorRepository.deleteById(id)
    }

    override fun listAvailableTypes(): List<ValidatorTypeInfo> =
        constraintRuleRegistry.listAll().map { (type, rule) ->
            ValidatorTypeInfo(type = type, paramsSchema = rule.paramsSchema())
        }.sortedBy { it.type }

    private fun validateRules(validator: Validator) {
        // Для CONSTRAINT тип берём из rules["type"] (так же, как делает
        // RuleValidationEngine при запуске); для RANGE — фиксированный тип.
        val ruleType: String = when (validator.checkType) {
            ValidatorType.RANGE -> RangeRule.TYPE
            ValidatorType.CONSTRAINT -> validator.rules["type"] as? String
                ?: throw ValidationException(
                    listOf(fail(validator.id, "CONSTRAINT validator '${validator.name}' missing 'type' in rules"))
                )
        }
        val rule = try {
            constraintRuleRegistry.getByType(ruleType)
        } catch (e: IllegalArgumentException) {
            throw ValidationException(
                listOf(fail(validator.id, e.message ?: "Unknown rule type '$ruleType'"))
            )
        }
        val errors = rule.validateParams(validator.rules)
        if (errors.isNotEmpty()) {
            throw ValidationException(errors.map { fail(validator.id, it) })
        }
    }

    private fun fail(id: UUID, msg: String) = ValidationResult(ValidationStatus.FAIL, msg, id)
}
