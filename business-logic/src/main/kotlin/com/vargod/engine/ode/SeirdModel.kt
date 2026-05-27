package com.vargod.engine.ode

import com.vargod.engine.OdeModel

/**
 * SEIRD — расширение SIR с инкубационным (E) и летальным (D) компартментами.
 * S + E + I + R + D = N сохраняется по построению (сумма производных = 0).
 */
class SeirdModel : OdeModel {
    override fun code(): String = CODE
    override fun variables(): List<String> = listOf("S", "E", "I", "R", "D")
    override fun parameters(): List<String> = listOf("beta", "sigma", "gamma", "mu", "N")

    override fun derivatives(state: DoubleArray, params: Map<String, Double>, t: Double): DoubleArray {
        require(state.size == 5) { "SEIRD expects 5 state variables [S, E, I, R, D], got ${state.size}" }
        val s = state[0]
        val e = state[1]
        val i = state[2]
        val beta = requireParam(params, "beta")
        val sigma = requireParam(params, "sigma")
        val gamma = requireParam(params, "gamma")
        val mu = requireParam(params, "mu")
        val n = requireParam(params, "N")
        require(n > 0.0) { "N must be positive" }
        val newInfections = beta * s * i / n
        val dS = -newInfections
        val dE = newInfections - sigma * e
        val dI = sigma * e - (gamma + mu) * i
        val dR = gamma * i
        val dD = mu * i
        return doubleArrayOf(dS, dE, dI, dR, dD)
    }

    private fun requireParam(params: Map<String, Double>, name: String): Double =
        params[name] ?: throw IllegalArgumentException("Missing parameter '$name' for SEIRD")

    companion object {
        const val CODE = "SEIRD"
    }
}
