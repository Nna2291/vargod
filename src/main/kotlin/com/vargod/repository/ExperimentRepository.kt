package com.vargod.repository

import com.vargod.domain.Experiment
import java.util.UUID

interface ExperimentRepository {
    fun findById(id: UUID): Experiment?
    fun findAllByUserId(userId: UUID): List<Experiment>
    fun findAllPublic(): List<Experiment>
    fun findAllByIds(ids: List<UUID>): List<Experiment>
    fun save(experiment: Experiment): Experiment
    fun deleteById(id: UUID)
}
