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

    override fun write(line: String) {
        out.println(line)
    }

    override fun writeError(line: String) {
        err.println(line)
    }
}
