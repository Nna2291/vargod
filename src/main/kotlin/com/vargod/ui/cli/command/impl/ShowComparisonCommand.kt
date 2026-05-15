package com.vargod.ui.cli.command.impl

import com.vargod.ui.cli.command.Command
import com.vargod.ui.cli.command.CommandContext
import java.util.UUID

class ShowComparisonCommand : Command {
    override fun name(): String = "cmp"
    override fun help(): String = "cmp <id>                      — show comparison details"
    override fun execute(args: List<String>, ctx: CommandContext) {
        require(args.isNotEmpty()) { "Usage: cmp <id>" }
        val id = UUID.fromString(args.first())
        val comparison = ctx.comparisonService.getById(id)
        ctx.console.write(ctx.comparisonFormatter.render(comparison))
    }
}
