package com.vargod.ui.cli.command.impl

import com.vargod.ui.cli.command.Command
import com.vargod.ui.cli.command.CommandContext

class ListModelsCommand : Command {
    override fun name(): String = "models"
    override fun help(): String = "models                        — list all model templates"
    override fun execute(args: List<String>, ctx: CommandContext) {
        val models = ctx.modelService.listAll()
        ctx.console.write(ctx.modelFormatter.renderList(models))
    }
}
