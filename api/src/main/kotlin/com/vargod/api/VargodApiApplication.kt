package com.vargod.api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * Точка входа REST API-сервера Vargod.
 *
 * API — это компонент связи между веб-GUI (React SPA) и бизнес-логикой:
 * контроллеры (Spring MVC) принимают HTTP-запросы и вызывают сервисы
 * из компонента business-logic.
 */
@SpringBootApplication
class VargodApiApplication

fun main(args: Array<String>) {
    runApplication<VargodApiApplication>(*args)
}
