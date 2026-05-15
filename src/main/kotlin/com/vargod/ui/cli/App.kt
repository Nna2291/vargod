package com.vargod.ui.cli

import com.vargod.domain.PersistenceException
import kotlin.system.exitProcess

fun main() {
    val config = CliConfig.fromEnv()
    try {
        val (loop, _) = CompositionRoot(config).bootstrap()
        loop.run()
    } catch (e: PersistenceException) {
        System.err.println("Startup failed: ${e.message}")
        e.cause?.let { System.err.println("  cause: ${it.javaClass.simpleName}: ${it.message}") }
        exitProcess(1)
    } catch (e: Exception) {
        System.err.println("Fatal: ${e.javaClass.simpleName}: ${e.message}")
        exitProcess(1)
    }
}
