package com.vargod.ui.cli.format

import com.vargod.domain.SimulationResult

class SimulationResultFormatter : Formatter<SimulationResult> {

    override fun render(value: SimulationResult): String = renderPreview(value, headRows = 5, tailRows = 5)

    fun renderPreview(value: SimulationResult, headRows: Int = 5, tailRows: Int = 5): String {
        val n = value.timePoints.size
        if (n == 0) return "(empty result)"
        val varNames = value.variables.keys.toList()
        val header = "  " + ("t".padEnd(COL) + varNames.joinToString("") { it.padEnd(COL) })
        val sep = "  " + "-".repeat(COL * (1 + varNames.size))
        val indices = pickIndices(n, headRows, tailRows)
        val rows = indices.map { i ->
            "  " + fmt(value.timePoints[i]).padEnd(COL) +
                varNames.joinToString("") { name -> fmt(value.variables.getValue(name)[i]).padEnd(COL) }
        }
        return buildString {
            appendLine("  points: $n; result id=${value.id}; createdAt=${value.createdAt}")
            appendLine(header)
            appendLine(sep)
            rows.forEachIndexed { idx, row ->
                appendLine(row)
                if (idx == headRows - 1 && n > headRows + tailRows) {
                    appendLine("  " + "…".padEnd(COL * (1 + varNames.size)))
                }
            }
        }.trimEnd()
    }

    private fun pickIndices(n: Int, head: Int, tail: Int): List<Int> {
        if (n <= head + tail) return (0 until n).toList()
        return (0 until head) + ((n - tail) until n)
    }

    private fun fmt(d: Double): String = "%.4f".format(d)

    companion object {
        private const val COL = 12
    }
}
