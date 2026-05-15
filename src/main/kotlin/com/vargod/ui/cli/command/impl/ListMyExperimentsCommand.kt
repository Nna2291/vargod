package com.vargod.ui.cli.command.impl

import com.vargod.ui.cli.command.Command
import com.vargod.ui.cli.command.CommandContext

class ListMyExperimentsCommand : Command {
    override fun name(): String = "my-exps"
    override fun help(): String = "my-exps                       — list experiments owned by the current user"
    override fun execute(args: List<String>, ctx: CommandContext) {
        val userId = ctx.session.requireUserId()
        val experiments = ctx.experimentService.listByUser(userId)
        ctx.console.write(ctx.experimentFormatter.renderList(experiments))
    }
}
