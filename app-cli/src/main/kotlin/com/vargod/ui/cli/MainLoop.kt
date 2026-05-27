package com.vargod.ui.cli

import com.vargod.domain.DomainException
import com.vargod.domain.ValidationException
import com.vargod.domain.ValidationStatus
import com.vargod.ui.cli.command.CommandContext
import com.vargod.ui.cli.command.QuitSignal
import com.vargod.ui.cli.router.CommandRouter
import com.vargod.ui.cli.session.Mode
import org.slf4j.LoggerFactory

class MainLoop(
    private val router: CommandRouter,
    private val ctx: CommandContext,
) {
    private val log = LoggerFactory.getLogger(MainLoop::class.java)

    fun run() {
        ctx.console.write("Vargod CLI — type 'help' to list commands, 'quit' to exit.")
        log.info("Главный цикл CLI запущен")
        while (true) {
            val prompt = "vargod${tag()}> "
            val line = ctx.console.readLine(prompt) ?: break
            if (line.isBlank()) continue
            // Каждое действие пользователя фиксируется в лог-файле (уровень INFO).
            log.info("Действие пользователя [{}]: {}", ctx.session.mode, line)
            try {
                router.dispatch(line, ctx)
            } catch (_: QuitSignal) {
                log.info("Действие пользователя: выход из приложения")
                ctx.console.write("Bye.")
                return
            } catch (e: ValidationException) {
                val failed = e.results.count { it.status == ValidationStatus.FAIL }
                log.warn("Валидация не пройдена для команды '{}': нарушений — {}", line, failed)
                ctx.console.writeError("validation failed:")
                e.results.filter { it.status == ValidationStatus.FAIL }
                    .forEach { ctx.console.writeError("  - ${it.message ?: it.validatorId.toString()}") }
            } catch (e: DomainException) {
                log.warn("Доменная ошибка при команде '{}': {}: {}", line, e.javaClass.simpleName, e.message)
                ctx.console.writeError("${e.javaClass.simpleName}: ${e.message}")
            } catch (e: IllegalArgumentException) {
                log.warn("Некорректный ввод в команде '{}': {}", line, e.message)
                ctx.console.writeError("bad input: ${e.message}")
            } catch (e: IllegalStateException) {
                log.warn("Некорректное состояние при команде '{}': {}", line, e.message)
                ctx.console.writeError("invalid state: ${e.message}")
            } catch (e: NumberFormatException) {
                log.warn("Ошибка разбора числа в команде '{}': {}", line, e.message)
                ctx.console.writeError("bad number: ${e.message}")
            } catch (e: Exception) {
                // Любое необработанное исключение пишется в лог с полным стектрейсом.
                log.error("Необработанное исключение при команде '{}'", line, e)
                ctx.console.writeError("${e.javaClass.simpleName}: ${e.message}")
            }
        }
        log.info("Главный цикл CLI завершён")
    }

    private fun tag(): String = when (ctx.session.mode) {
        Mode.GUEST -> " [guest]"
        Mode.RESEARCHER -> " [researcher]"
        Mode.ADMIN -> " [admin]"
    }
}
