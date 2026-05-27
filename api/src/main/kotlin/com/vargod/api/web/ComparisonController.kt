package com.vargod.api.web

import com.vargod.api.security.CurrentUser
import com.vargod.api.web.dto.CreateComparisonRequest
import com.vargod.domain.Comparison
import com.vargod.service.ComparisonService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

/** REST-контроллер сравнений вычислительных экспериментов. */
@RestController
@RequestMapping("/api/comparisons")
@Tag(name = "Comparisons", description = "Сравнение экспериментов на одних осях")
class ComparisonController(private val comparisonService: ComparisonService) {

    private val log = LoggerFactory.getLogger(ComparisonController::class.java)

    @Operation(summary = "Список сравнений текущего пользователя")
    @GetMapping
    fun list(currentUser: CurrentUser): List<Comparison> = comparisonService.listByUser(currentUser.id)

    @Operation(summary = "Получить сравнение по ID")
    @GetMapping("/{id}")
    fun get(@PathVariable id: UUID): Comparison = comparisonService.getById(id)

    @Operation(summary = "Создать сравнение из набора experimentIds")
    @PostMapping
    fun create(
        @RequestBody request: CreateComparisonRequest,
        currentUser: CurrentUser,
    ): Comparison {
        log.info(
            "Действие пользователя {}: создание сравнения из {} экспериментов",
            currentUser.email, request.experimentIds.size,
        )
        return comparisonService.create(request.experimentIds, currentUser.id)
    }
}
