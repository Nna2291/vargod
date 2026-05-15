package com.vargod.ui.cli.command.impl

import com.vargod.domain.NotFoundException
import com.vargod.domain.dto.CreateExperimentRequest
import com.vargod.ui.cli.command.Command
import com.vargod.ui.cli.command.CommandContext

class CreateExperimentCommand : Command {
    override fun name(): String = "new-exp"
    override fun help(): String = "new-exp                       — interactively create an experiment (researcher only)"

    override fun execute(args: List<String>, ctx: CommandContext) {
        val userId = ctx.session.requireUserId()

        val code = prompt(ctx, "Model code (e.g. SIR, LOTKA_VOLTERRA, LOGISTIC): ").trim().uppercase()
        val model = ctx.modelService.listAll().firstOrNull { it.code.equals(code, ignoreCase = true) }
            ?: throw NotFoundException("ModelTemplate", java.util.UUID(0, 0))
        val ode = ctx.modelRegistry.getByCode(model.code)

        val values = linkedMapOf<String, Double>()
        ode.variables().forEach { v ->
            values[v] = prompt(ctx, "  param $v (initial value): ").trim().toDouble()
        }
        ode.parameters().forEach { p ->
            values[p] = prompt(ctx, "  param $p: ").trim().toDouble()
        }

        val tStart = prompt(ctx, "  tStart: ").trim().toDouble()
        val tEnd = prompt(ctx, "  tEnd  : ").trim().toDouble()
        val step = prompt(ctx, "  step  : ").trim().toDouble()
        val name = prompt(ctx, "  name (optional, blank = none): ").trim().ifBlank { null }

        val req = CreateExperimentRequest(
            name = name,
            modelTemplateId = model.id,
            parameters = values,
            tStart = tStart,
            tEnd = tEnd,
            step = step,
        )
        val experiment = ctx.experimentService.create(req, userId)
        ctx.console.write("→ created experiment ${experiment.id}; status=${experiment.status}")
    }

    private fun prompt(ctx: CommandContext, message: String): String =
        ctx.console.readLine(message) ?: throw IllegalStateException("Unexpected end of input")
}
