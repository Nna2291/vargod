package com.vargod.ui.cli.command.impl

import com.vargod.ui.cli.command.Command
import com.vargod.ui.cli.command.CommandContext
import java.util.UUID

class CreateComparisonCommand : Command {
    override fun name(): String = "new-cmp"
    override fun help(): String = "new-cmp <id1> <id2> [...]     — create comparison from 2+ experiments (same model)"
    override fun execute(args: List<String>, ctx: CommandContext) {
        require(args.size >= 2) { "Usage: new-cmp <id1> <id2> [...]" }
        val userId = ctx.session.requireUserId()
        val ids = args.map { UUID.fromString(it) }
        val comparison = ctx.comparisonService.create(ids, userId)
        ctx.console.write("→ created comparison ${comparison.id} (${comparison.experiments.size} experiments)")
    }
}
