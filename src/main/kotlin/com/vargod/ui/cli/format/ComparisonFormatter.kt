package com.vargod.ui.cli.format

import com.vargod.domain.Comparison

class ComparisonFormatter : Formatter<Comparison> {

    override fun render(value: Comparison): String = buildString {
        appendLine("Comparison ${value.id}")
        appendLine("  name        : ${value.name ?: "—"}")
        appendLine("  userId      : ${value.userId}")
        appendLine("  createdAt   : ${value.createdAt}")
        appendLine("  experiments : ${value.experiments.size}")
        value.experiments.forEachIndexed { idx, e ->
            appendLine("    [$idx] ${e.id}  ${e.status.name.padEnd(10)} name=${e.name ?: "—"}  hasResult=${e.result != null}")
        }
    }.trimEnd()

    fun renderList(comparisons: List<Comparison>): String {
        if (comparisons.isEmpty()) return "(no comparisons)"
        return comparisons.joinToString("\n") { c ->
            "  ${c.id}  experiments=${c.experiments.size}  ${c.name ?: "—"}"
        }
    }
}
