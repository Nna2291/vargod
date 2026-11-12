package com.vargod.service.impl

import com.vargod.domain.NotFoundException
import com.vargod.domain.ValidationException
import com.vargod.domain.ValidationStatus
import com.vargod.domain.Validator
import com.vargod.domain.repository.ValidatorRepository
import com.vargod.engine.ConstraintRule
import com.vargod.engine.ConstraintRuleRegistry
import com.vargod.engine.validation.RuleParamsSchema
import com.vargod.support.RealEngines
import com.vargod.testing.BoundaryValues
import com.vargod.testing.Classical
import com.vargod.testing.DecisionTable
import com.vargod.testing.EquivalencePartitioning
import com.vargod.testing.ErrorGuessing
import com.vargod.testing.London
import com.vargod.testing.builders.aValidator
import com.vargod.testing.inmemory.InMemoryValidatorRepository
import com.vargod.testing.mothers.ValidatorMother
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.runs
import io.mockk.verify
import io.qameta.allure.Epic
import io.qameta.allure.Feature
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.UUID

@London
@Epic("Business logic")
@Feature("ValidatorAdminService")
@DisplayName("ValidatorAdminServiceImpl — London style (MockK)")
@ExtendWith(MockKExtension::class)
class ValidatorAdminServiceImplLondonTest {

    @MockK private lateinit var validatorRepository: ValidatorRepository
    @MockK private lateinit var ruleRegistry: ConstraintRuleRegistry
    @MockK private lateinit var rule: ConstraintRule

    private lateinit var sut: ValidatorAdminServiceImpl

    private val modelId = UUID.randomUUID()
    private val adminId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        sut = ValidatorAdminServiceImpl(validatorRepository, ruleRegistry)
    }

    // ---------------------------------------------------------------- list / get

    @Test
    @EquivalencePartitioning
    fun `list - passes model filter to repository`() {
        // Arrange
        val validators = listOf(ValidatorMother.positive("P"))
        every { validatorRepository.findAll(modelId) } returns validators

        // Act
        val result = sut.list(modelId)

        // Assert
        assertThat(result).isEqualTo(validators)
    }

    @Test
    @BoundaryValues
    fun `list - without filter asks repository for all validators and may get none`() {
        // Arrange
        every { validatorRepository.findAll(null) } returns emptyList()

        // Act
        val result = sut.list()

        // Assert
        assertThat(result).isEmpty()
        verify(exactly = 1) { validatorRepository.findAll(null) }
    }

    @Test
    @EquivalencePartitioning
    fun `get - returns validator found by id`() {
        // Arrange
        val validator = ValidatorMother.positive("K")
        every { validatorRepository.findById(validator.id) } returns validator

        // Act
        val result = sut.get(validator.id)

        // Assert
        assertThat(result).isSameAs(validator)
    }

    @Test
    @EquivalencePartitioning
    fun `get - unknown id gives NotFoundException`() {
        // Arrange
        val id = UUID.randomUUID()
        every { validatorRepository.findById(id) } returns null

        // Act
        val error = assertThrows<NotFoundException> { sut.get(id) }

        // Assert
        assertThat(error.message).isEqualTo("Validator not found: id=$id")
    }

    // ---------------------------------------------------------------- create

    @Test
    @EquivalencePartitioning
    fun `create - RANGE rules are checked by RANGE rule and then saved`() {
        // Arrange
        val validator = ValidatorMother.fraction("gamma")
        every { ruleRegistry.getByType("RANGE") } returns rule
        every { rule.validateParams(validator.rules) } returns emptyList()
        every { validatorRepository.save(validator, modelId, adminId) } returns validator

        // Act
        val result = sut.create(validator, modelId, adminId)

        // Assert
        assertThat(result).isSameAs(validator)
        verify(exactly = 1) { rule.validateParams(validator.rules) }
        verify(exactly = 1) { validatorRepository.save(validator, modelId, adminId) }
    }

    @Test
    @EquivalencePartitioning
    fun `create - every rule error becomes a FAIL result and nothing is saved`() {
        // Arrange
        val validator = ValidatorMother.rangeWithoutBounds()
        every { ruleRegistry.getByType("RANGE") } returns rule
        every { rule.validateParams(any()) } returns listOf("error one", "error two")

        // Act
        val error = assertThrows<ValidationException> { sut.create(validator, modelId, adminId) }

        // Assert
        assertThat(error.results.map { it.message }).containsExactly("error one", "error two")
        assertThat(error.results).allMatch { it.status == ValidationStatus.FAIL && it.validatorId == validator.id }
        verify(exactly = 0) { validatorRepository.save(any(), any(), any()) }
    }

    @Test
    @ErrorGuessing
    fun `create - CONSTRAINT without type is rejected without asking registry`() {
        // Arrange
        val validator = ValidatorMother.constraintWithoutType()

        // Act
        val error = assertThrows<ValidationException> { sut.create(validator, modelId, adminId) }

        // Assert
        assertThat(error.message).contains("missing 'type'")
        verify(exactly = 0) { ruleRegistry.getByType(any()) }
    }

    // ---------------------------------------------------------------- update

    @Test
    @EquivalencePartitioning
    fun `update - existing validator with valid rules is updated`() {
        // Arrange
        val validator = ValidatorMother.positive("r")
        every { validatorRepository.findById(validator.id) } returns validator
        every { ruleRegistry.getByType("RANGE") } returns rule
        every { rule.validateParams(any()) } returns emptyList()
        every { validatorRepository.update(validator) } returns validator

        // Act
        val result = sut.update(validator)

        // Assert
        assertThat(result).isSameAs(validator)
        verify(exactly = 1) { validatorRepository.update(validator) }
    }

    @Test
    @EquivalencePartitioning
    fun `update - unknown validator gives NotFoundException before rules are checked`() {
        // Arrange
        val validator = ValidatorMother.positive("r")
        every { validatorRepository.findById(validator.id) } returns null

        // Act
        val error = assertThrows<NotFoundException> { sut.update(validator) }

        // Assert
        assertThat(error.message).contains(validator.id.toString())
        verify(exactly = 0) { ruleRegistry.getByType(any()) }
        verify(exactly = 0) { validatorRepository.update(any()) }
    }

    // ---------------------------------------------------------------- delete

    @Test
    @EquivalencePartitioning
    fun `delete - existing validator is deleted`() {
        // Arrange
        val validator = ValidatorMother.positive("P")
        every { validatorRepository.findById(validator.id) } returns validator
        every { validatorRepository.deleteById(validator.id) } just runs

        // Act
        sut.delete(validator.id)

        // Assert
        verify(exactly = 1) { validatorRepository.deleteById(validator.id) }
    }

    @Test
    @EquivalencePartitioning
    fun `delete - unknown validator gives NotFoundException and nothing is deleted`() {
        // Arrange
        val id = UUID.randomUUID()
        every { validatorRepository.findById(id) } returns null

        // Act
        assertThrows<NotFoundException> { sut.delete(id) }

        // Assert
        verify(exactly = 0) { validatorRepository.deleteById(any()) }
    }

    // ---------------------------------------------------------------- listAvailableTypes

    @Test
    @EquivalencePartitioning
    fun `listAvailableTypes - types are sorted and carry rule schemas`() {
        // Arrange
        val schemaB = RuleParamsSchema(emptyList())
        val schemaA = RuleParamsSchema(listOf(RuleParamsSchema.FieldSpec("x", RuleParamsSchema.ParamType.NUMBER)))
        val ruleB = io.mockk.mockk<ConstraintRule> { every { paramsSchema() } returns schemaB }
        val ruleA = io.mockk.mockk<ConstraintRule> { every { paramsSchema() } returns schemaA }
        every { ruleRegistry.listAll() } returns linkedMapOf("B_RULE" to ruleB, "A_RULE" to ruleA)

        // Act
        val result = sut.listAvailableTypes()

        // Assert
        assertThat(result.map { it.type }).containsExactly("A_RULE", "B_RULE")
        assertThat(result.first().paramsSchema).isSameAs(schemaA)
    }

    @Test
    @BoundaryValues
    fun `listAvailableTypes - empty registry gives empty list`() {
        // Arrange
        every { ruleRegistry.listAll() } returns emptyMap()

        // Act
        val result = sut.listAvailableTypes()

        // Assert
        assertThat(result).isEmpty()
    }
}

@Classical
@Epic("Business logic")
@Feature("ValidatorAdminService")
@DisplayName("ValidatorAdminServiceImpl — classical style (real rule registry + in-memory repository)")
class ValidatorAdminServiceImplClassicalTest {

    private lateinit var validators: InMemoryValidatorRepository
    private lateinit var sut: ValidatorAdminServiceImpl

    private val modelId = UUID.randomUUID()
    private val adminId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        validators = InMemoryValidatorRepository()
        sut = ValidatorAdminServiceImpl(validators, RealEngines.ruleRegistry())
    }

    @Test
    @BoundaryValues
    fun `create - RANGE with min equal to max is a valid degenerate range`() {
        // Arrange
        val validator = aValidator().range("N", min = 1000.0, max = 1000.0).build()

        // Act
        sut.create(validator, modelId, adminId)

        // Assert
        assertThat(validators.findById(validator.id)).isEqualTo(validator)
        assertThat(validators.modelOf(validator.id)).isEqualTo(modelId)
        assertThat(validators.createdByOf(validator.id)).isEqualTo(adminId)
    }

    /**
     * Таблица решений для проверки конфигурации валидатора перед сохранением.
     *
     * | условие / правило               | R1 | R2 | R3 | R4 | R5 | R6 |
     * |---------------------------------|----|----|----|----|----|----|
     * | checkType                       | RG | RG | CN | CN | CN | CN |
     * | rules.type задан                | -  | -  | Y  | N  | Y  | Y  |
     * | тип известен реестру            | Y  | Y  | Y  | -  | N  | Y  |
     * | параметры проходят схему        | Y  | N  | Y  | -  | -  | N  |
     * | действие: валидатор сохранён    | Y  | N  | Y  | N  | N  | N  |
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("configurationDecisionTable")
    @DecisionTable
    fun `create - decision table of validator configuration outcomes`(rule: String, validator: Validator, saved: Boolean) {
        // Arrange
        val modelTemplateId = modelId

        // Act
        val outcome = runCatching { sut.create(validator, modelTemplateId, adminId) }

        // Assert
        assertThat(outcome.isSuccess).describedAs(rule).isEqualTo(saved)
        assertThat(validators.findById(validator.id) != null).describedAs(rule).isEqualTo(saved)
        if (!saved) assertThat(outcome.exceptionOrNull()).isInstanceOf(ValidationException::class.java)
    }

    companion object {
        @JvmStatic
        fun configurationDecisionTable(): List<Arguments> = listOf(
            Arguments.of("R1: RANGE, valid bounds", ValidatorMother.positive("P"), true),
            Arguments.of("R2: RANGE without bounds", ValidatorMother.rangeWithoutBounds(), false),
            Arguments.of("R3: CONSTRAINT, known type, valid params", ValidatorMother.fractionsSumToOne("a", "b"), true),
            Arguments.of("R4: CONSTRAINT without type", ValidatorMother.constraintWithoutType(), false),
            Arguments.of("R5: CONSTRAINT of unknown type", ValidatorMother.constraintOfUnknownType(), false),
            Arguments.of("R6: CONSTRAINT, known type, missing params", aValidator().constraint("SUM_EQUALS_VALUE").build(), false),
        )
    }
}
