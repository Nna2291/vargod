package com.vargod.engine

import com.vargod.domain.ModelTemplate
import com.vargod.domain.ParameterSet
import com.vargod.domain.SimulationResult

interface SimulationEngine {
    fun solve(model: ModelTemplate, params: ParameterSet): SimulationResult
}
