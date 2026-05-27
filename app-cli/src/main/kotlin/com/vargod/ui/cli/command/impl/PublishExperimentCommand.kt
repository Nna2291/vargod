package com.vargod.ui.cli.command.impl

import com.vargod.ui.cli.command.Command
import com.vargod.ui.cli.command.CommandContext
import java.util.UUID

class PublishExperimentCommand : Command {
    override fun name(): String = "publish"
    override fun help(): String = "publish <id>                  — publish your experiment (set isPublic=true)"
    override fun execute(args: List<String>, ctx: CommandContext) {
        require(args.isNotEmpty()) { "Usage: publish <id>" }
        val userId = ctx.session.requireUserId()
        val id = UUID.fromString(args.first())
        val experiment = ctx.experimentService.publish(id, userId)
        ctx.console.write("→ published; isPublic=${experiment.isPublic}")
    }
}
