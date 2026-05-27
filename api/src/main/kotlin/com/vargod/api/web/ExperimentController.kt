package com.vargod.api.web

import com.vargod.api.security.CurrentUser
import com.vargod.domain.AuthorizationException
import com.vargod.domain.Experiment
import com.vargod.domain.dto.CreateExperimentRequest
import com.vargod.service.ExperimentService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

/**
 * REST-контроллер вычислительных экспериментов.
 *
 * scope=public видна гостю (без токена). scope=mine / создание / запуск / публикация —
 * только аутентифицированному пользователю; principal приходит из JWT через
 * [CurrentUser] (см. CurrentUserArgumentResolver).
 */
@RestController
@RequestMapping("/api/experiments")
@Tag(name = "Experiments", description = "Вычислительные эксперименты: создание, запуск, публикация")
class ExperimentController(private val experimentService: ExperimentService) {

    private val log = LoggerFactory.getLogger(ExperimentController::class.java)

    /** scope=public — публичные эксперименты; scope=mine — эксперименты пользователя. */
    @Operation(summary = "Список экспериментов: scope=public (гость) или scope=mine (требуется JWT)")
    @GetMapping
    fun list(
        @RequestParam(defaultValue = "public") scope: String,
        currentUser: CurrentUser?,
    ): List<Experiment> = when (scope) {
        "mine" -> {
            val user = currentUser ?: throw AuthorizationException("Для scope=mine требуется вход")
            experimentService.listByUser(user.id)
        }
        else -> experimentService.listPublic()
    }

    @Operation(summary = "Получить эксперимент по ID")
    @GetMapping("/{id}")
    fun get(@PathVariable id: UUID): Experiment = experimentService.getById(id)

    @Operation(summary = "Создать эксперимент (требуется JWT)")
    @PostMapping
    fun create(
        @RequestBody request: CreateExperimentRequest,
        currentUser: CurrentUser,
    ): Experiment {
        log.info("Действие пользователя {}: создание эксперимента '{}'", currentUser.email, request.name)
        return experimentService.create(request, currentUser.id)
    }

    @Operation(summary = "Запустить численную симуляцию эксперимента")
    @PostMapping("/{id}/run")
    fun run(@PathVariable id: UUID, currentUser: CurrentUser): Experiment {
        log.info("Действие пользователя {}: запуск симуляции эксперимента {}", currentUser.email, id)
        experimentService.runSimulation(id)
        return experimentService.getById(id)
    }

    @Operation(summary = "Опубликовать эксперимент (сделать видимым гостям)")
    @PostMapping("/{id}/publish")
    fun publish(@PathVariable id: UUID, currentUser: CurrentUser): Experiment {
        log.info("Действие пользователя {}: публикация эксперимента {}", currentUser.email, id)
        return experimentService.publish(id, currentUser.id)
    }

    @Operation(summary = "Снять публичность с эксперимента (только владелец)")
    @PostMapping("/{id}/unpublish")
    fun unpublish(@PathVariable id: UUID, currentUser: CurrentUser): Experiment {
        log.info("Действие пользователя {}: снятие публичности эксперимента {}", currentUser.email, id)
        return experimentService.unpublish(id, currentUser.id)
    }
}
