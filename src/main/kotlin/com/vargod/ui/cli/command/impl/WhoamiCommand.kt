package com.vargod.ui.cli.command.impl

import com.vargod.ui.cli.command.Command
import com.vargod.ui.cli.command.CommandContext

class WhoamiCommand : Command {
    override fun name(): String = "whoami"
    override fun help(): String = "whoami                        — show current actor (mode + userId)"
    override fun execute(args: List<String>, ctx: CommandContext) {
        ctx.console.write("mode=${ctx.session.mode}; userId=${ctx.session.userId ?: "—"}")
    }
}
