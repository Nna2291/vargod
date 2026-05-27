package com.vargod.engine.ode

import com.vargod.engine.OdeModel

/**
 * FitzHugh-Nagumo — упрощённая 2D-модель возбудимой мембраны.
 * V — мембранный потенциал, w — медленная восстанавливающая переменная.
 */
class FitzHughNagumoModel : OdeModel {
    override fun code(): String = CODE
    override fun variables(): List<String> = listOf("V", "w")
    override fun parameters(): List<String> = listOf("a", "b", "tau", "Iext")

    override fun derivatives(state: DoubleArray, params: Map<String, Double>, t: Double): DoubleArray {
        require(state.size == 2) { "FitzHugh-Nagumo expects 2 state variables [V, w], got ${state.size}" }
        val v = state[0]
        val w = state[1]
        val a = requireParam(params, "a")
        val b = requireParam(params, "b")
        val tau = requireParam(params, "tau")
        val iExt = requireParam(params, "Iext")
        require(tau > 0.0) { "tau must be positive" }
        val dV = v - v * v * v / 3.0 - w + iExt
        val dW = (v + a - b * w) / tau
        return doubleArrayOf(dV, dW)
    }

    private fun requireParam(params: Map<String, Double>, name: String): Double =
        params[name] ?: throw IllegalArgumentException("Missing parameter '$name' for FitzHugh-Nagumo")

    companion object {
        const val CODE = "FITZHUGH_NAGUMO"
    }
}
