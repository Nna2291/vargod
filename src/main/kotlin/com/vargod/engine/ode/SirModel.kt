package com.vargod.engine.ode

import com.vargod.engine.OdeModel

class SirModel : OdeModel {
    override fun code(): String = CODE
    override fun variables(): List<String> = listOf("S", "I", "R")
    override fun parameters(): List<String> = listOf("beta", "gamma", "N")

    override fun derivatives(state: DoubleArray, params: Map<String, Double>, t: Double): DoubleArray {
        require(state.size == 3) { "SIR expects 3 state variables [S, I, R], got ${state.size}" }
        val s = state[0]
        val i = state[1]
        val beta = requireParam(params, "beta")
        val gamma = requireParam(params, "gamma")
        val n = requireParam(params, "N")
        require(n > 0.0) { "N must be positive" }
        val dS = -beta * s * i / n
        val dI = beta * s * i / n - gamma * i
        val dR = gamma * i
        return doubleArrayOf(dS, dI, dR)
    }

    private fun requireParam(params: Map<String, Double>, name: String): Double =
        params[name] ?: throw IllegalArgumentException("Missing parameter '$name' for SIR")

    companion object {
        const val CODE = "SIR"
    }
}
