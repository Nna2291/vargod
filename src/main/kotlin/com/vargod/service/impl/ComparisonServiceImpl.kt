package com.vargod.service.impl

import com.vargod.domain.AuthorizationException
import com.vargod.domain.Comparison
import com.vargod.domain.Experiment
import com.vargod.domain.NotFoundException
import com.vargod.domain.repository.ComparisonRepository
import com.vargod.domain.repository.ExperimentRepository
import com.vargod.service.ComparisonService
import java.time.Instant
import java.util.UUID

class ComparisonServiceImpl(
    private val comparisonRepository: ComparisonRepository,
    private val experimentRepository: ExperimentRepository,
) : ComparisonService {

    override fun create(experimentIds: List<UUID>, userId: UUID): Comparison {
        require(experimentIds.size >= 2) { "Comparison requires at least 2 experiments, got ${experimentIds.size}" }
        require(experimentIds.distinct().size == experimentIds.size) { "Duplicate experiment ids in comparison" }

        val experiments = experimentRepository.findAllByIds(experimentIds)
        if (experiments.size != experimentIds.size) {
            val foundIds = experiments.map { it.id }.toSet()
            val missing = experimentIds.first { it !in foundIds }
            throw NotFoundException("Experiment", missing)
        }
        assertAccess(experiments, userId)
        assertSameModel(experiments)

        val comparison = Comparison(
            id = UUID.randomUUID(),
            name = null,
            userId = userId,
            experiments = experiments,
            createdAt = Instant.now(),
        )
        return comparisonRepository.save(comparison)
    }

    override fun getById(id: UUID): Comparison =
        comparisonRepository.findById(id) ?: throw NotFoundException("Comparison", id)

    override fun listByUser(userId: UUID): List<Comparison> =
        comparisonRepository.findAllByUserId(userId)

    private fun assertAccess(experiments: List<Experiment>, userId: UUID) {
        val inaccessible = experiments.firstOrNull { it.userId != userId && !it.isPublic }
        if (inaccessible != null) {
            throw AuthorizationException(
                "Experiment ${inaccessible.id} is not accessible to user $userId"
            )
        }
    }

    private fun assertSameModel(experiments: List<Experiment>) {
        val distinctModels = experiments.map { it.modelTemplateId }.distinct()
        require(distinctModels.size == 1) {
            "All experiments in a comparison must use the same model, found ${distinctModels.size}"
        }
    }
}
