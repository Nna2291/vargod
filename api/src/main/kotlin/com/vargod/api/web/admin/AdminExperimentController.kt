package com.vargod.api.web.admin

import com.vargod.api.security.CurrentUser
import com.vargod.domain.Experiment
import com.vargod.domain.NotFoundException
import com.vargod.domain.repository.ExperimentRepository
import com.vargod.service.AuditActions
import com.vargod.service.AuditLogService
import com.vargod.service.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

/**
 * Admin-инспекция и force-delete экспериментов.
 * Чтение/создание пользовательских экспериментов — публичный контроллер.
 */
@RestController
@RequestMapping("/api/admin/experiments")
@Tag(name = "Admin: Experiments", description = "Инспекция и принудительное удаление экспериментов")
class AdminExperimentController(
    private val experimentRepository: ExperimentRepository,
    private val auditLogService: AuditLogService,
    private val userService: UserService,
) {

    private val log = LoggerFactory.getLogger(AdminExperimentController::class.java)

    @Operation(summary = "Все эксперименты с пагинацией (limit/offset)")
    @GetMapping
    fun list(
        @RequestParam(defaultValue = "100") limit: Int,
        @RequestParam(defaultValue = "0") offset: Int,
    ): List<Experiment> = experimentRepository.findAllPaginated(limit, offset)

    @Operation(summary = "Принудительно удалить эксперимент любого пользователя")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun forceDelete(@PathVariable id: UUID, currentUser: CurrentUser) {
        // Снимок до удаления — в payload журнала.
        val existing = experimentRepository.findById(id)
            ?: throw NotFoundException("Experiment", id)
        experimentRepository.deleteById(id)
        log.info("Admin {} принудительно удалил эксперимент {} (владелец {})",
            currentUser.email, id, existing.userId)
        auditLogService.log(
            actor = userService.getById(currentUser.id),
            action = AuditActions.EXPERIMENT_FORCE_DELETE,
            targetType = "EXPERIMENT",
            targetId = id,
            payload = mapOf(
                "name" to existing.name,
                "ownerUserId" to existing.userId.toString(),
                "status" to existing.status.name,
            ),
        )
    }
}
