package com.vargod.repository

import com.vargod.domain.Comparison
import java.util.UUID

interface ComparisonRepository {
    fun findById(id: UUID): Comparison?
    fun findAllByUserId(userId: UUID): List<Comparison>
    fun save(comparison: Comparison): Comparison
}
