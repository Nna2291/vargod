package com.vargod.ui.cli.command.impl

import com.vargod.ui.cli.command.Command
import com.vargod.ui.cli.command.CommandContext
import java.util.UUID

class LoginCommand : Command {
    override fun name(): String = "login"
    override fun help(): String = "login [uuid]                  — switch to researcher; without arg generates a new UUID"
    override fun execute(args: List<String>, ctx: CommandContext) {
        val userId = if (args.isEmpty()) UUID.randomUUID() else UUID.fromString(args.first())
        ctx.session.loginAs(userId)
        ctx.console.write("→ logged in as researcher; userId=$userId")
    }
}
