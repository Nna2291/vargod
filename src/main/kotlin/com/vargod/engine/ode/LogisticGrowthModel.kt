package com.vargod.engine.ode

import com.vargod.engine.OdeModel

class LogisticGrowthModel : OdeModel {
    override fun code(): String = CODE
    override fun variables(): List<String> = listOf("P")
    override fun parameters(): List<String> = listOf("r", "K")

    override fun derivatives(state: DoubleArray, params: Map<String, Double>, t: Double): DoubleArray {
        require(state.size == 1) { "Logistic expects 1 state variable [P], got ${state.size}" }
        val p = state[0]
        val r = requireParam(params, "r")
        val k = requireParam(params, "K")
        require(k > 0.0) { "K (carrying capacity) must be positive" }
        return doubleArrayOf(r * p * (1.0 - p / k))
    }

    private fun requireParam(params: Map<String, Double>, name: String): Double =
        params[name] ?: throw IllegalArgumentException("Missing parameter '$name' for Logistic")

    companion object {
        const val CODE = "LOGISTIC"
    }
}
