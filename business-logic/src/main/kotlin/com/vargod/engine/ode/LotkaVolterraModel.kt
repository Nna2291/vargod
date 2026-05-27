package com.vargod.engine.ode

import com.vargod.engine.OdeModel

class LotkaVolterraModel : OdeModel {
    override fun code(): String = CODE
    override fun variables(): List<String> = listOf("x", "y")
    override fun parameters(): List<String> = listOf("alpha", "beta", "gamma", "delta")

    override fun derivatives(state: DoubleArray, params: Map<String, Double>, t: Double): DoubleArray {
        require(state.size == 2) { "Lotka-Volterra expects 2 state variables [x, y], got ${state.size}" }
        val x = state[0]
        val y = state[1]
        val alpha = requireParam(params, "alpha")
        val beta = requireParam(params, "beta")
        val gamma = requireParam(params, "gamma")
        val delta = requireParam(params, "delta")
        val dx = alpha * x - beta * x * y
        val dy = -gamma * y + delta * x * y
        return doubleArrayOf(dx, dy)
    }

    private fun requireParam(params: Map<String, Double>, name: String): Double =
        params[name] ?: throw IllegalArgumentException("Missing parameter '$name' for Lotka-Volterra")

    companion object {
        const val CODE = "LOTKA_VOLTERRA"
    }
}
