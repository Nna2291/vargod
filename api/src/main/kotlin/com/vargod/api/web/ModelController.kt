package com.vargod.api.web

import com.vargod.domain.ModelTemplate
import com.vargod.service.ModelService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

/** REST-контроллер каталога математических моделей. */
@RestController
@RequestMapping("/api/models")
@Tag(name = "Models", description = "Публичный каталог математических моделей")
class ModelController(private val modelService: ModelService) {

    @Operation(summary = "Список всех моделей с описаниями и валидаторами")
    @GetMapping
    fun list(): List<ModelTemplate> = modelService.listAll()

    @Operation(summary = "Шаблон модели по ID")
    @GetMapping("/{id}")
    fun get(@PathVariable id: UUID): ModelTemplate = modelService.getById(id)
}
