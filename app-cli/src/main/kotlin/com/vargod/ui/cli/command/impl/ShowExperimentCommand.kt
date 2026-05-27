package com.vargod.ui.cli.command.impl

import com.vargod.ui.cli.command.Command
import com.vargod.ui.cli.command.CommandContext
import java.util.UUID

class ShowExperimentCommand : Command {
    override fun name(): String = "exp"
    override fun help(): String = "exp <id>                      — show experiment details + result preview"
    override fun execute(args: List<String>, ctx: CommandContext) {
        require(args.isNotEmpty()) { "Usage: exp <id>" }
        val id = UUID.fromString(args.first())
        val experiment = ctx.experimentService.getById(id)
        ctx.console.write(ctx.experimentFormatter.render(experiment))
    }
}
