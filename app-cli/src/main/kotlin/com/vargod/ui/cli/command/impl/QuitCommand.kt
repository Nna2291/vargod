package com.vargod.ui.cli.command.impl

import com.vargod.ui.cli.command.Command
import com.vargod.ui.cli.command.CommandContext
import com.vargod.ui.cli.command.QuitSignal

class QuitCommand : Command {
    override fun name(): String = "quit"
    override fun aliases(): List<String> = listOf("exit", "q")
    override fun help(): String = "quit                          — exit the application"
    override fun execute(args: List<String>, ctx: CommandContext) {
        throw QuitSignal()
    }
}
