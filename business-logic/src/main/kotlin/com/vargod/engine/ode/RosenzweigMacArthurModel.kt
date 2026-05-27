package com.vargod.engine.ode

import com.vargod.engine.OdeModel

/**
 * Rosenzweig-MacArthur — модель «хищник-жертва» с логистическим ростом жертвы
 * и насыщающейся функциональной реакцией II типа: f(x) = a·x / (1 + a·b·x).
 *
 * Переменные: x — жертва, y — хищник.
 * Параметры:
 *  - r   — собственный темп роста жертвы;
 *  - K   — ёмкость среды для жертвы;
 *  - a   — скорость атаки хищника;
 *  - b   — время обработки жертвы;
 *  - eps — эффективность конверсии жертвы в хищника;
 *  - m   — смертность хищника.
 */
class RosenzweigMacArthurModel : OdeModel {
    override fun code(): String = CODE
    override fun variables(): List<String> = listOf("x", "y")
    override fun parameters(): List<String> = listOf("r", "K", "a", "b", "eps", "m")

    override fun derivatives(state: DoubleArray, params: Map<String, Double>, t: Double): DoubleArray {
        require(state.size == 2) { "Rosenzweig-MacArthur expects 2 state variables [x, y], got ${state.size}" }
        val x = state[0]
        val y = state[1]
        val r = requireParam(params, "r")
        val k = requireParam(params, "K")
        val a = requireParam(params, "a")
        val b = requireParam(params, "b")
        val eps = requireParam(params, "eps")
        val m = requireParam(params, "m")
        require(k > 0.0) { "K must be positive" }
        val f = a * x / (1.0 + a * b * x)
        val dx = r * x * (1.0 - x / k) - f * y
        val dy = eps * f * y - m * y
        return doubleArrayOf(dx, dy)
    }

    private fun requireParam(params: Map<String, Double>, name: String): Double =
        params[name] ?: throw IllegalArgumentException("Missing parameter '$name' for Rosenzweig-MacArthur")

    companion object {
        const val CODE = "ROSENZWEIG_MACARTHUR"
    }
}
