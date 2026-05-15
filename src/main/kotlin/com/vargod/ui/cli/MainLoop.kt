package com.vargod.ui.cli

import com.vargod.domain.DomainException
import com.vargod.domain.ValidationException
import com.vargod.domain.ValidationStatus
import com.vargod.ui.cli.command.CommandContext
import com.vargod.ui.cli.command.QuitSignal
import com.vargod.ui.cli.router.CommandRouter

class MainLoop(
    private val router: CommandRouter,
    private val ctx: CommandContext,
) {
    fun run() {
        ctx.console.write("Vargod CLI — type 'help' to list commands, 'quit' to exit.")
        while (true) {
            val prompt = "vargod${tag()}> "
            val line = ctx.console.readLine(prompt) ?: break
            if (line.isBlank()) continue
            try {
                router.dispatch(line, ctx)
            } catch (_: QuitSignal) {
                ctx.console.write("Bye.")
                return
            } catch (e: ValidationException) {
                ctx.console.writeError("validation failed:")
                e.results.filter { it.status == ValidationStatus.FAIL }
                    .forEach { ctx.console.writeError("  - ${it.message ?: it.validatorId.toString()}") }
            } catch (e: DomainException) {
                ctx.console.writeError("${e.javaClass.simpleName}: ${e.message}")
            } catch (e: IllegalArgumentException) {
                ctx.console.writeError("bad input: ${e.message}")
            } catch (e: IllegalStateException) {
                ctx.console.writeError("invalid state: ${e.message}")
            } catch (e: NumberFormatException) {
                ctx.console.writeError("bad number: ${e.message}")
            } catch (e: Exception) {
                ctx.console.writeError("${e.javaClass.simpleName}: ${e.message}")
            }
        }
    }

    private fun tag(): String = when (ctx.session.mode) {
        com.vargod.ui.cli.session.Mode.GUEST -> " [guest]"
        com.vargod.ui.cli.session.Mode.RESEARCHER -> " [researcher]"
    }
}
