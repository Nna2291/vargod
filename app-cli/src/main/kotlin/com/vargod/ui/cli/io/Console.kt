package com.vargod.ui.cli.io

interface Console {
    fun readLine(prompt: String): String?
    /**
     * Прочитать секрет (пароль) без эха в терминал, если возможно.
     * Если процесс запущен не из реального TTY (например, под `docker run` без `-it`),
     * реализация может откатиться к обычному readLine — это лучше, чем падать.
     */
    fun readPassword(prompt: String): String?
    fun write(line: String)
    fun writeError(line: String)
}
