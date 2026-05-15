package com.vargod.engine.validation

import com.vargod.engine.ConstraintRule

class OrderedRule : ConstraintRule {
    override fun type(): String = TYPE

    override fun check(values: Map<String, Any>, params: Map<String, Any>): Boolean {
        val fields = fieldsOf(params)
        require(fields.size >= 2) { "ORDERED requires at least 2 fields" }
        val direction = (params["direction"] as? String)?.uppercase()
            ?: throw IllegalArgumentException("Missing 'direction' (ASCENDING or DESCENDING)")
        val seq = fields.map { numberOf(values[it], it) }
        return when (direction) {
            "ASCENDING" -> seq.zipWithNext().all { (a, b) -> a < b }
            "DESCENDING" -> seq.zipWithNext().all { (a, b) -> a > b }
            else -> throw IllegalArgumentException("Unknown direction '$direction', expected ASCENDING or DESCENDING")
        }
    }

    companion object {
        const val TYPE = "ORDERED"
    }
}
