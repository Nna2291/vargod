package com.vargod.domain

import java.time.Instant
import java.util.UUID

data class Comparison(
    val id: UUID,
    val name: String?,
    val userId: UUID,
    val experiments: List<Experiment>,
    val createdAt: Instant,
)
