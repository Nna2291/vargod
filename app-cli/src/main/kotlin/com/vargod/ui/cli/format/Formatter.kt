package com.vargod.ui.cli.format

interface Formatter<T> {
    fun render(value: T): String
}
