package com.vargod.engine.ode

import com.vargod.engine.ModelRegistry
import com.vargod.engine.OdeModel

class MapModelRegistry(models: List<OdeModel>) : ModelRegistry {

    private val byCode: Map<String, OdeModel>

    init {
        val map = LinkedHashMap<String, OdeModel>(models.size)
        for (model in models) {
            val code = model.code()
            require(code.isNotBlank()) { "OdeModel code must not be blank" }
            require(map.put(code, model) == null) { "Duplicate ODE model code: $code" }
        }
        byCode = map
    }

    override fun getByCode(code: String): OdeModel =
        byCode[code] ?: throw IllegalArgumentException("Unknown ODE model code: $code")

    override fun listAll(): Map<String, OdeModel> = byCode
}
