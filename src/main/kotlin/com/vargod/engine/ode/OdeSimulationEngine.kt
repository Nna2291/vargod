package com.vargod.engine.ode

import com.vargod.domain.ModelTemplate
import com.vargod.domain.ParameterSet
import com.vargod.domain.SimulationResult
import com.vargod.engine.ModelRegistry
import com.vargod.engine.OdeModel
import com.vargod.engine.SimulationEngine
import org.apache.commons.math3.ode.FirstOrderDifferentialEquations
import org.apache.commons.math3.ode.nonstiff.DormandPrince54Integrator
import org.apache.commons.math3.ode.sampling.FixedStepHandler
import org.apache.commons.math3.ode.sampling.StepNormalizer
import java.time.Instant
import java.util.UUID

class OdeSimulationEngine(
    private val modelRegistry: ModelRegistry,
    private val absoluteTolerance: Double = 1.0e-10,
    private val relativeTolerance: Double = 1.0e-10,
    private val minStep: Double = 1.0e-8,
    private val maxStep: Double = 1.0,
) : SimulationEngine {

    override fun solve(model: ModelTemplate, params: ParameterSet): SimulationResult {
        require(params.tEnd > params.tStart) { "tEnd (${params.tEnd}) must be greater than tStart (${params.tStart})" }
        require(params.step > 0.0) { "step must be positive, got ${params.step}" }

        val ode = modelRegistry.getByCode(model.code)
        val varNames = ode.variables()
        val initial = initialState(ode, params)

        val integrator = DormandPrince54Integrator(minStep, maxStep, absoluteTolerance, relativeTolerance)
        val timePoints = mutableListOf<Double>()
        val series: Map<String, MutableList<Double>> = varNames.associateWith { mutableListOf() }

        val handler = object : FixedStepHandler {
            override fun init(t0: Double, y0: DoubleArray, t: Double) { /* no-op */ }
            override fun handleStep(t: Double, y: DoubleArray, yDot: DoubleArray, isLast: Boolean) {
                timePoints.add(t)
                varNames.forEachIndexed { idx, name -> series.getValue(name).add(y[idx]) }
            }
        }
        integrator.addStepHandler(StepNormalizer(params.step, handler))

        val finalState = initial.copyOf()
        integrator.integrate(
            asEquations(ode, params, varNames),
            params.tStart,
            initial,
            params.tEnd,
            finalState,
        )

        return SimulationResult(
            id = UUID.randomUUID(),
            timePoints = timePoints.toList(),
            variables = series.mapValues { it.value.toList() },
            createdAt = Instant.now(),
        )
    }

    private fun initialState(ode: OdeModel, params: ParameterSet): DoubleArray {
        val varNames = ode.variables()
        val arr = DoubleArray(varNames.size)
        for ((i, name) in varNames.withIndex()) {
            arr[i] = params.values[name]
                ?: throw IllegalArgumentException("Missing initial value for variable '$name'")
        }
        return arr
    }

    private fun asEquations(
        ode: OdeModel,
        params: ParameterSet,
        varNames: List<String>,
    ): FirstOrderDifferentialEquations = object : FirstOrderDifferentialEquations {
        override fun getDimension(): Int = varNames.size

        override fun computeDerivatives(t: Double, y: DoubleArray, yDot: DoubleArray) {
            val derivs = ode.derivatives(y, params.values, t)
            require(derivs.size == varNames.size) {
                "OdeModel '${ode.code()}' produced ${derivs.size} derivatives but expected ${varNames.size}"
            }
            System.arraycopy(derivs, 0, yDot, 0, derivs.size)
        }
    }
}
