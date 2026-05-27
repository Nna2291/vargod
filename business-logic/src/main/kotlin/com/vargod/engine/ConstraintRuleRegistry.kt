package com.vargod.engine

interface ConstraintRuleRegistry {
    fun getByType(type: String): ConstraintRule
    fun listAll(): Map<String, ConstraintRule>
}
