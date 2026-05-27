package com.vargod.service

import com.vargod.domain.Experiment
import com.vargod.domain.SimulationResult
import com.vargod.domain.dto.CreateExperimentRequest
import java.util.UUID

interface ExperimentService {
    fun create(req: CreateExperimentRequest, userId: UUID): Experiment
    fun getById(id: UUID): Experiment
    fun listByUser(userId: UUID): List<Experiment>
    fun listPublic(): List<Experiment>
    fun runSimulation(experimentId: UUID): SimulationResult
    fun publish(id: UUID, userId: UUID): Experiment
    /** Снять публичность с эксперимента. Допускается только владельцу. */
    fun unpublish(id: UUID, userId: UUID): Experiment
}
