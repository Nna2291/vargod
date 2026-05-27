package com.vargod.ui.cli.router

import com.vargod.ui.cli.command.Command
import com.vargod.ui.cli.command.CommandContext

class CommandRouter(commands: Collection<Command>) {

    private val byKey: Map<String, Command> = buildMap {
        commands.forEach { cmd ->
            require(put(cmd.name(), cmd) == null) { "Duplicate command name: ${cmd.name()}" }
            cmd.aliases().forEach { alias ->
                require(put(alias, cmd) == null) { "Duplicate command alias: $alias" }
            }
        }
    }

    fun commands(): Collection<Command> = byKey.values

    fun dispatch(line: String, ctx: CommandContext) {
        val tokens = tokenize(line)
        if (tokens.isEmpty()) return
        val key = tokens.first()
        val cmd = byKey[key] ?: byKey[key.lowercase()]
        if (cmd == null) {
            ctx.console.writeError("Unknown command: '$key'. Type 'help' to list commands.")
            return
        }
        cmd.execute(tokens.drop(1), ctx)
    }

    private fun tokenize(line: String): List<String> =
        line.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
}
