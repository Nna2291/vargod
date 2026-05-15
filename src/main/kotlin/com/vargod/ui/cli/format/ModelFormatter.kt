package com.vargod.ui.cli.format

import com.vargod.domain.ModelTemplate
import com.vargod.domain.ValidatorType
import com.vargod.engine.ModelRegistry

class ModelFormatter(private val modelRegistry: ModelRegistry) : Formatter<ModelTemplate> {

    override fun render(value: ModelTemplate): String = buildString {
        appendLine("Model ${value.code}")
        appendLine("  id          : ${value.id}")
        appendLine("  name        : ${value.name}")
        appendLine("  description : ${value.description ?: "—"}")
        appendLine("  solverType  : ${value.solverType}")
        val ode = runCatching { modelRegistry.getByCode(value.code) }.getOrNull()
        if (ode != null) {
            appendLine("  variables   : ${ode.variables().joinToString(", ")}")
            appendLine("  parameters  : ${ode.parameters().joinToString(", ")}")
        }
        if (value.validators.isEmpty()) {
            appendLine("  validators  : (none)")
        } else {
            appendLine("  validators  :")
            value.validators.forEach { v ->
                val tag = when (v.checkType) {
                    ValidatorType.RANGE -> "RANGE"
                    ValidatorType.CONSTRAINT -> "CONSTRAINT(${v.rules["type"] ?: "?"})"
                }
                appendLine("    - ${v.name} [$tag] target=${v.targetField} rules=${v.rules}")
            }
        }
    }.trimEnd()

    fun renderList(models: List<ModelTemplate>): String {
        if (models.isEmpty()) return "(no models)"
        return models.mapIndexed { idx, m ->
            "  [${idx + 1}] ${m.code.padEnd(18)} — ${m.name}${m.description?.let { d -> " (${d.take(60)})" } ?: ""}"
        }.joinToString("\n")
    }
}
