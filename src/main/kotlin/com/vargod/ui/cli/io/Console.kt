package com.vargod.ui.cli.io

interface Console {
    fun readLine(prompt: String): String?
    fun write(line: String)
    fun writeError(line: String)
}
