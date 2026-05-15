package com.vargod.engine.validation

import com.vargod.engine.ConstraintRule
import com.vargod.engine.ConstraintRuleRegistry

class MapConstraintRuleRegistry(rules: List<ConstraintRule>) : ConstraintRuleRegistry {

    private val byType: Map<String, ConstraintRule>

    init {
        val map = LinkedHashMap<String, ConstraintRule>(rules.size)
        for (rule in rules) {
            val type = rule.type()
            require(type.isNotBlank()) { "ConstraintRule type must not be blank" }
            require(map.put(type, rule) == null) { "Duplicate constraint rule type: $type" }
        }
        byType = map
    }

    override fun getByType(type: String): ConstraintRule =
        byType[type] ?: throw IllegalArgumentException("Unknown constraint rule type: $type")

    override fun listAll(): Map<String, ConstraintRule> = byType
}
