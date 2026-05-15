package com.vargod.engine

interface ConstraintRule {
    fun type(): String
    fun check(values: Map<String, Any>, params: Map<String, Any>): Boolean
}
