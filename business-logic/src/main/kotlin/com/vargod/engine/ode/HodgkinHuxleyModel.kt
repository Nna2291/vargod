package com.vargod.engine.ode

import com.vargod.engine.OdeModel
import kotlin.math.abs
import kotlin.math.exp

/**
 * Hodgkin-Huxley — каноническая модель потенциала действия гигантского аксона
 * кальмара (1952). Жёсткая система: рекомендуется maxStep=0.01, relTol=1e-6,
 * absTol=1e-9 (хранится в model_templates.recommended_*).
 *
 * Переменные:
 *  - V (мВ)   — мембранный потенциал;
 *  - n, m, h  — безразмерные воротные переменные (∈ [0, 1]).
 *
 * Параметры (стандартные единицы):
 *  - gNa, gK, gL (мСм/см²) — максимальные проводимости каналов;
 *  - ENa, EK, EL (мВ)      — потенциалы реверсии;
 *  - Cm (мкФ/см²)          — мембранная ёмкость;
 *  - Iext (мкА/см²)        — внешний ток инжекции.
 *
 * Скорости alpha_n и alpha_m имеют устранимые особенности при V=-55 и V=-40
 * (знаменатель → 0). Через L'Hôpital пределы равны 0.1 и 1.0 соответственно.
 */
class HodgkinHuxleyModel : OdeModel {
    override fun code(): String = CODE
    override fun variables(): List<String> = listOf("V", "n", "m", "h")
    override fun parameters(): List<String> = listOf(
        "gNa", "gK", "gL", "ENa", "EK", "EL", "Cm", "Iext"
    )

    override fun derivatives(state: DoubleArray, params: Map<String, Double>, t: Double): DoubleArray {
        require(state.size == 4) { "Hodgkin-Huxley expects 4 state variables [V, n, m, h], got ${state.size}" }
        val v = state[0]
        val n = state[1]
        val m = state[2]
        val h = state[3]

        val gNa = requireParam(params, "gNa")
        val gK = requireParam(params, "gK")
        val gL = requireParam(params, "gL")
        val eNa = requireParam(params, "ENa")
        val eK = requireParam(params, "EK")
        val eL = requireParam(params, "EL")
        val cm = requireParam(params, "Cm")
        val iExt = requireParam(params, "Iext")
        require(cm > 0.0) { "Cm must be positive" }

        val aN = alphaN(v)
        val bN = betaN(v)
        val aM = alphaM(v)
        val bM = betaM(v)
        val aH = alphaH(v)
        val bH = betaH(v)

        val iNa = gNa * m * m * m * h * (v - eNa)
        val iK = gK * n * n * n * n * (v - eK)
        val iL = gL * (v - eL)

        val dV = (iExt - iNa - iK - iL) / cm
        val dN = aN * (1.0 - n) - bN * n
        val dM = aM * (1.0 - m) - bM * m
        val dH = aH * (1.0 - h) - bH * h
        return doubleArrayOf(dV, dN, dM, dH)
    }

    // alpha_n(V) = 0.01·(V+55) / (1 − exp(−(V+55)/10))
    // Устранимая особенность при V=-55 (числитель и знаменатель -> 0).
    // L'Hôpital: lim = 0.01 / (1/10) = 0.1.
    private fun alphaN(v: Double): Double {
        val x = v + 55.0
        return if (abs(x) < 1e-6) 0.1 else 0.01 * x / (1.0 - exp(-x / 10.0))
    }

    private fun betaN(v: Double): Double = 0.125 * exp(-(v + 65.0) / 80.0)

    // alpha_m(V) = 0.1·(V+40) / (1 − exp(−(V+40)/10))
    // Устранимая особенность при V=-40. L'Hôpital: lim = 0.1 / (1/10) = 1.0.
    private fun alphaM(v: Double): Double {
        val x = v + 40.0
        return if (abs(x) < 1e-6) 1.0 else 0.1 * x / (1.0 - exp(-x / 10.0))
    }

    private fun betaM(v: Double): Double = 4.0 * exp(-(v + 65.0) / 18.0)

    private fun alphaH(v: Double): Double = 0.07 * exp(-(v + 65.0) / 20.0)

    private fun betaH(v: Double): Double = 1.0 / (1.0 + exp(-(v + 35.0) / 10.0))

    private fun requireParam(params: Map<String, Double>, name: String): Double =
        params[name] ?: throw IllegalArgumentException("Missing parameter '$name' for Hodgkin-Huxley")

    companion object {
        const val CODE = "HODGKIN_HUXLEY"
    }
}
