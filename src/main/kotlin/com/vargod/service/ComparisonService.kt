package com.vargod.service

import com.vargod.domain.Comparison
import java.util.UUID

interface ComparisonService {
    fun create(experimentIds: List<UUID>, userId: UUID): Comparison
    fun getById(id: UUID): Comparison
    fun listByUser(userId: UUID): List<Comparison>
}
