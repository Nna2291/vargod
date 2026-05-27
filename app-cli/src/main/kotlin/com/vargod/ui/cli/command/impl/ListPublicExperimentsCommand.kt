package com.vargod.ui.cli.command.impl

import com.vargod.ui.cli.command.Command
import com.vargod.ui.cli.command.CommandContext

class ListPublicExperimentsCommand : Command {
    override fun name(): String = "public"
    override fun help(): String = "public                        — list public experiments (guest-accessible)"
    override fun execute(args: List<String>, ctx: CommandContext) {
        val experiments = ctx.experimentService.listPublic()
        ctx.console.write(ctx.experimentFormatter.renderList(experiments))
    }
}
