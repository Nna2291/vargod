package com.vargod.ui.cli.command.impl

import com.vargod.ui.cli.command.Command
import com.vargod.ui.cli.command.CommandContext

/**
 * Вход по email/паролю. Пароль может быть передан вторым аргументом (удобно для
 * скриптов), но безопаснее запросить интерактивно — тогда он не попадает в
 * историю shell'а и не печатается на экран (см. Console.readPassword).
 */
class LoginCommand : Command {
    override fun name(): String = "login"
    override fun help(): String =
        "login <email> [password]      — log in as user; password prompted securely if omitted"

    override fun execute(args: List<String>, ctx: CommandContext) {
        if (args.isEmpty()) {
            ctx.console.writeError("usage: login <email> [password]")
            return
        }
        val email = args[0]
        val password = args.getOrNull(1)
            ?: ctx.console.readPassword("password: ")
            ?: run {
                ctx.console.writeError("password input cancelled")
                return
            }
        val user = ctx.authService.authenticate(email, password)
        ctx.session.loginAs(user)
        ctx.console.write("→ logged in as ${user.email} (${user.role})")
    }
}
