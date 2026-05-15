package com.vargod.ui.cli.format

import com.vargod.domain.Experiment

class ExperimentFormatter(
    private val simulationFormatter: SimulationResultFormatter,
) : Formatter<Experiment> {

    override fun render(value: Experiment): String = buildString {
        appendLine("Experiment ${value.id}")
        appendLine("  name        : ${value.name ?: "—"}")
        appendLine("  status      : ${value.status}")
        appendLine("  isPublic    : ${value.isPublic}")
        appendLine("  userId      : ${value.userId}")
        appendLine("  modelTplId  : ${value.modelTemplateId}")
        appendLine("  createdAt   : ${value.createdAt}")
        val ps = value.parameterSet
        if (ps == null) {
            appendLine("  params      : (none)")
        } else {
            appendLine("  params      : ${ps.values}")
            appendLine("  t           : [${ps.tStart}, ${ps.tEnd}] step=${ps.step}")
        }
        val r = value.result
        if (r == null) {
            appendLine("  result      : (not yet)")
        } else {
            appendLine("  result      :")
            append(simulationFormatter.renderPreview(r))
        }
    }.trimEnd()

    fun renderList(experiments: List<Experiment>): String {
        if (experiments.isEmpty()) return "(no experiments)"
        return experiments.joinToString("\n") { e ->
            "  ${e.id}  ${e.status.name.padEnd(10)} pub=${if (e.isPublic) "Y" else "n"}  ${e.name ?: "—"}"
        }
    }
}
