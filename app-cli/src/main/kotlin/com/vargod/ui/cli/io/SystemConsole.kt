package com.vargod.ui.cli.io

import java.io.BufferedReader
import java.io.InputStreamReader

class SystemConsole(
    private val reader: BufferedReader = BufferedReader(InputStreamReader(System.`in`)),
    private val out: java.io.PrintStream = System.out,
    private val err: java.io.PrintStream = System.err,
) : Console {

    override fun readLine(prompt: String): String? {
        out.print(prompt)
        out.flush()
        return reader.readLine()
    }

    /**
     * `System.console()` доступен только из реального TTY. В Docker без `-it`,
     * IDE-консоли или при перенаправлении stdin вернётся null — тогда читаем
     * пароль обычным способом с предупреждением.
     */
    override fun readPassword(prompt: String): String? {
        val systemConsole = System.console()
        return if (systemConsole != null) {
            systemConsole.readPassword(prompt)?.concatToString()
        } else {
            err.println("(warning: пароль будет виден в терминале — нет доступа к TTY)")
            readLine(prompt)
        }
    }

    override fun write(line: String) {
        out.println(line)
    }

    override fun writeError(line: String) {
        err.println(line)
    }
}
