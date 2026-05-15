package com.vargod.ui.cli.command.impl

import com.vargod.ui.cli.command.Command
import com.vargod.ui.cli.command.CommandContext

class GuestCommand : Command {
    override fun name(): String = "guest"
    override fun help(): String = "guest                         — switch to guest mode (no userId)"
    override fun execute(args: List<String>, ctx: CommandContext) {
        ctx.session.logout()
        ctx.console.write("→ now in guest mode")
    }
}
