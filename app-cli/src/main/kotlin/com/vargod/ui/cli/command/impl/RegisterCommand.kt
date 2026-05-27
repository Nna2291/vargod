package com.vargod.ui.cli.command.impl

import com.vargod.ui.cli.command.Command
import com.vargod.ui.cli.command.CommandContext

/**
 * Регистрация нового пользователя с ролью RESEARCHER. Логин автоматически
 * выполняется после успешной регистрации.
 *
 * Для создания ADMIN-аккаунта используйте VARGOD_ADMIN_EMAIL/PASSWORD при первом
 * старте API (AdminBootstrap) или admin-эндпоинты `/api/admin/users` (P3a).
 */
class RegisterCommand : Command {
    override fun name(): String = "register"
    override fun help(): String =
        "register <email> [password]   — create a new RESEARCHER account; password prompted if omitted"

    override fun execute(args: List<String>, ctx: CommandContext) {
        if (args.isEmpty()) {
            ctx.console.writeError("usage: register <email> [password]")
            return
        }
        val email = args[0]
        val password = args.getOrNull(1)
            ?: ctx.console.readPassword("password (min 8 chars): ")
            ?: run {
                ctx.console.writeError("password input cancelled")
                return
            }
        val user = ctx.authService.register(email = email, plainPassword = password)
        ctx.session.loginAs(user)
        ctx.console.write("→ registered and logged in as ${user.email} (${user.role})")
    }
}
