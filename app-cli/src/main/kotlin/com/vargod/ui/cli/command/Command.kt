package com.vargod.ui.cli.command

interface Command {
    fun name(): String
    fun aliases(): List<String> = emptyList()
    fun help(): String
    fun execute(args: List<String>, ctx: CommandContext)
}
