package com.vargod.domain

import java.util.UUID

data class Validator(
    val id: UUID,
    val name: String,
    val checkType: ValidatorType,
    val targetKind: ValidatorTargetKind,
    val targetField: String,
    val rules: Map<String, Any>,
)

enum class ValidatorType { RANGE, CONSTRAINT }

enum class ValidatorTargetKind { PARAMETER, DATASET_COLUMN }
