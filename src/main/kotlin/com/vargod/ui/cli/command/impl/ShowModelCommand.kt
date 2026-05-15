package com.vargod.ui.cli.command.impl

import com.vargod.domain.NotFoundException
import com.vargod.ui.cli.command.Command
import com.vargod.ui.cli.command.CommandContext
import java.util.UUID

class ShowModelCommand : Command {
    override fun name(): String = "model"
    override fun help(): String = "model <code|id>               — show model details + validators"

    override fun execute(args: List<String>, ctx: CommandContext) {
        require(args.isNotEmpty()) { "Usage: model <code|id>" }
        val key = args.first()
        val model = resolve(key, ctx) ?: throw NotFoundException("ModelTemplate", parseUuidOrNil(key))
        ctx.console.write(ctx.modelFormatter.render(model))
    }

    private fun resolve(key: String, ctx: CommandContext): com.vargod.domain.ModelTemplate? {
        val asUuid = runCatching { UUID.fromString(key) }.getOrNull()
        return if (asUuid != null) {
            runCatching { ctx.modelService.getById(asUuid) }.getOrNull()
        } else {
            ctx.modelService.listAll().firstOrNull { it.code.equals(key, ignoreCase = true) }
        }
    }

    private fun parseUuidOrNil(key: String): UUID =
        runCatching { UUID.fromString(key) }.getOrElse { UUID(0, 0) }
}
