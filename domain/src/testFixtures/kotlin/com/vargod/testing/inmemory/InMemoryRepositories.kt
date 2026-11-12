package com.vargod.testing.inmemory

import com.vargod.domain.AuditEntry
import com.vargod.domain.Comparison
import com.vargod.domain.Experiment
import com.vargod.domain.ModelTemplate
import com.vargod.domain.PersistenceException
import com.vargod.domain.Role
import com.vargod.domain.User
import com.vargod.domain.Validator
import com.vargod.domain.repository.AuditLogFilter
import com.vargod.domain.repository.AuditLogRepository
import com.vargod.domain.repository.ComparisonRepository
import com.vargod.domain.repository.ExperimentRepository
import com.vargod.domain.repository.ModelTemplateRepository
import com.vargod.domain.repository.UserRepository
import com.vargod.domain.repository.ValidatorRepository
import java.util.UUID

/*
 * In-memory реализации репозиториев для тестов бизнес-логики в классическом стиле.
 *
 * Это не mock и не stub: у них нет запрограммированных ответов и проверок вызовов —
 * это полноценные (хоть и упрощённые) реализации контрактов domain.repository,
 * повторяющие наблюдаемое поведение JDBC-версий (регистронезависимый email,
 * PersistenceException на update несуществующей записи, порядок выдачи и т.д.).
 * Классическая школа заменяет только внешние разделяемые зависимости (БД),
 * а тест проверяет итоговое состояние, а не последовательность вызовов.
 */

class InMemoryExperimentRepository(vararg initial: Experiment) : ExperimentRepository {
    private val store = LinkedHashMap<UUID, Experiment>()

    init { initial.forEach { store[it.id] = it } }

    override fun findById(id: UUID): Experiment? = store[id]
    override fun findAllByUserId(userId: UUID): List<Experiment> = store.values.filter { it.userId == userId }.sortedBy { it.createdAt }
    override fun findAllPublic(): List<Experiment> = store.values.filter { it.isPublic }.sortedBy { it.createdAt }
    override fun findAllByIds(ids: List<UUID>): List<Experiment> = store.values.filter { it.id in ids }.sortedBy { it.createdAt }
    override fun findAllPaginated(limit: Int, offset: Int): List<Experiment> {
        require(limit in 1..1000) { "limit must be in 1..1000, got $limit" }
        require(offset >= 0) { "offset must be >= 0, got $offset" }
        return store.values.sortedByDescending { it.createdAt }.drop(offset).take(limit)
    }
    override fun save(experiment: Experiment): Experiment = experiment.also { store[it.id] = it }
    override fun deleteById(id: UUID) { store.remove(id) }

    fun count(): Int = store.size
}

class InMemoryModelTemplateRepository(vararg initial: ModelTemplate) : ModelTemplateRepository {
    private val store = LinkedHashMap<UUID, ModelTemplate>()

    init { initial.forEach { store[it.id] = it } }

    override fun findAll(): List<ModelTemplate> = store.values.sortedBy { it.code }
    override fun findById(id: UUID): ModelTemplate? = store[id]
    override fun save(template: ModelTemplate): ModelTemplate = template.also { store[it.id] = it }
    override fun deleteById(id: UUID) { store.remove(id) }
}

class InMemoryUserRepository(vararg initial: User) : UserRepository {
    private val store = LinkedHashMap<UUID, User>()

    init { initial.forEach { store[it.id] = it } }

    override fun findById(id: UUID): User? = store[id]
    override fun findByEmail(email: String): User? = store.values.firstOrNull { it.email.equals(email, ignoreCase = true) }
    override fun findAll(): List<User> = store.values.sortedBy { it.createdAt }
    override fun existsAdmin(): Boolean = store.values.any { it.role == Role.ADMIN }
    override fun save(user: User): User = user.also { store[it.id] = it }
    override fun updateRole(id: UUID, role: Role): User {
        val user = store[id] ?: throw PersistenceException("User not found for role update: $id")
        return user.copy(role = role).also { store[id] = it }
    }
    override fun deleteById(id: UUID) { store.remove(id) }

    fun count(): Int = store.size
}

class InMemoryComparisonRepository(vararg initial: Comparison) : ComparisonRepository {
    private val store = LinkedHashMap<UUID, Comparison>()

    init { initial.forEach { store[it.id] = it } }

    override fun findById(id: UUID): Comparison? = store[id]
    override fun findAllByUserId(userId: UUID): List<Comparison> = store.values.filter { it.userId == userId }.sortedBy { it.createdAt }
    override fun save(comparison: Comparison): Comparison = comparison.also { store[it.id] = it }

    fun count(): Int = store.size
}

class InMemoryValidatorRepository : ValidatorRepository {
    private data class Row(val validator: Validator, val modelTemplateId: UUID, val createdBy: UUID?)

    private val store = LinkedHashMap<UUID, Row>()

    /** Arrange-хелпер: положить валидатор, привязанный к модели. */
    fun given(modelTemplateId: UUID, vararg validators: Validator) = apply {
        validators.forEach { store[it.id] = Row(it, modelTemplateId, null) }
    }

    override fun findAll(modelTemplateId: UUID?): List<Validator> =
        store.values.filter { modelTemplateId == null || it.modelTemplateId == modelTemplateId }.map { it.validator }
    override fun findById(id: UUID): Validator? = store[id]?.validator
    override fun save(validator: Validator, modelTemplateId: UUID, createdBy: UUID?): Validator =
        validator.also { store[it.id] = Row(it, modelTemplateId, createdBy) }
    override fun update(validator: Validator): Validator {
        val row = store[validator.id] ?: throw PersistenceException("Validator not found for update: ${validator.id}")
        store[validator.id] = row.copy(validator = validator)
        return validator
    }
    override fun deleteById(id: UUID) { store.remove(id) }

    fun modelOf(id: UUID): UUID? = store[id]?.modelTemplateId
    fun createdByOf(id: UUID): UUID? = store[id]?.createdBy
}

class InMemoryAuditLogRepository : AuditLogRepository {
    private val store = mutableListOf<AuditEntry>()

    override fun save(entry: AuditEntry): AuditEntry = entry.also { store += it }
    override fun find(filter: AuditLogFilter): List<AuditEntry> {
        require(filter.limit in 1..1000) { "limit must be in 1..1000, got ${filter.limit}" }
        require(filter.offset >= 0) { "offset must be >= 0, got ${filter.offset}" }
        return store.asSequence()
            .filter { filter.action == null || it.action == filter.action }
            .filter { filter.userId == null || it.userId == filter.userId }
            .filter { filter.from == null || !it.createdAt.isBefore(filter.from) }
            .filter { filter.to == null || it.createdAt.isBefore(filter.to) }
            .sortedByDescending { it.createdAt }
            .drop(filter.offset)
            .take(filter.limit)
            .toList()
    }

    fun all(): List<AuditEntry> = store.toList()
}
