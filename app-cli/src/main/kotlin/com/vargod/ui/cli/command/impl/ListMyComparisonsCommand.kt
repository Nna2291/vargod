package com.vargod.ui.cli.command.impl

import com.vargod.ui.cli.command.Command
import com.vargod.ui.cli.command.CommandContext

class ListMyComparisonsCommand : Command {
    override fun name(): String = "my-cmps"
    override fun help(): String = "my-cmps                       — list comparisons owned by the current user"
    override fun execute(args: List<String>, ctx: CommandContext) {
        val userId = ctx.session.requireUserId()
        val comparisons = ctx.comparisonService.listByUser(userId)
        ctx.console.write(ctx.comparisonFormatter.renderList(comparisons))
    }
}
