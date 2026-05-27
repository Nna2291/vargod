package com.vargod.ui.cli.command.impl

import com.vargod.ui.cli.command.Command
import com.vargod.ui.cli.command.CommandContext
import java.util.UUID

class RunSimulationCommand : Command {
    override fun name(): String = "run"
    override fun help(): String = "run <id>                      — run simulation for a PENDING experiment"
    override fun execute(args: List<String>, ctx: CommandContext) {
        require(args.isNotEmpty()) { "Usage: run <id>" }
        val id = UUID.fromString(args.first())
        val result = ctx.experimentService.runSimulation(id)
        ctx.console.write("→ simulation completed for $id")
        ctx.console.write(ctx.simulationFormatter.render(result))
    }
}
