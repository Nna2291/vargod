package com.vargod.ui.cli

import com.vargod.config.ConfigLoader
import com.vargod.domain.PersistenceException
import org.slf4j.LoggerFactory
import kotlin.system.exitProcess

private val log = LoggerFactory.getLogger("com.vargod.ui.cli.App")

fun main() {
    log.info("Запуск приложения Vargod CLI")
    val config = ConfigLoader.load()
    try {
        val (loop, _) = CompositionRoot(config).bootstrap()
        loop.run()
        log.info("Приложение Vargod CLI завершено штатно")
    } catch (e: PersistenceException) {
        log.error("Сбой запуска: ошибка доступа к данным — {}", e.message, e)
        System.err.println("Startup failed: ${e.message}")
        e.cause?.let { System.err.println("  cause: ${it.javaClass.simpleName}: ${it.message}") }
        exitProcess(1)
    } catch (e: Exception) {
        log.error("Фатальная ошибка приложения — {}", e.message, e)
        System.err.println("Fatal: ${e.javaClass.simpleName}: ${e.message}")
        exitProcess(1)
    }
}
