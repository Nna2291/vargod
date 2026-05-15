package com.vargod.engine

interface ModelRegistry {
    fun getByCode(code: String): OdeModel
    fun listAll(): Map<String, OdeModel>
}
