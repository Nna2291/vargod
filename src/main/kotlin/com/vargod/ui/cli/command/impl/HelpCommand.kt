package com.vargod.ui.cli.command.impl

import com.vargod.ui.cli.command.Command
import com.vargod.ui.cli.command.CommandContext

class HelpCommand(private val commandsProvider: () -> Collection<Command>) : Command {
    override fun name(): String = "help"
    override fun aliases(): List<String> = listOf("?")
    override fun help(): String = "help                          — show available commands"
    override fun execute(args: List<String>, ctx: CommandContext) {
        ctx.console.write("Available commands:")
        commandsProvider()
            .distinctBy { it.name() }
            .sortedBy { it.name() }
            .forEach { ctx.console.write("  ${it.help()}") }
    }
}
