package com.vargod.ui.cli.command.impl

import com.vargod.ui.cli.command.Command
import com.vargod.ui.cli.command.CommandContext

class WhoamiCommand : Command {
    override fun name(): String = "whoami"
    override fun help(): String = "whoami                        — show current actor (mode + email + userId)"
    override fun execute(args: List<String>, ctx: CommandContext) {
        val user = ctx.session.currentUser
        if (user == null) {
            ctx.console.write("mode=${ctx.session.mode}; not logged in")
        } else {
            ctx.console.write("mode=${ctx.session.mode}; email=${user.email}; userId=${user.id}")
        }
    }
}
