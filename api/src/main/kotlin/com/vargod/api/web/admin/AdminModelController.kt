package com.vargod.api.web.admin

import com.vargod.api.security.CurrentUser
import com.vargod.domain.ModelTemplate
import com.vargod.domain.NotFoundException
import com.vargod.domain.repository.ModelTemplateRepository
import com.vargod.service.AuditActions
import com.vargod.service.AuditLogService
import com.vargod.service.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

/**
 * Admin-CRUD шаблонов моделей.
 * Чтение каталога моделей остаётся публичным — [com.vargod.api.web.ModelController].
 */
@RestController
@RequestMapping("/api/admin/models")
@Tag(name = "Admin: Models", description = "CRUD шаблонов математических моделей")
class AdminModelController(
    private val modelTemplateRepository: ModelTemplateRepository,
    private val auditLogService: AuditLogService,
    private val userService: UserService,
) {

    private val log = LoggerFactory.getLogger(AdminModelController::class.java)

    @Operation(summary = "Создать шаблон модели")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @RequestBody req: CreateModelTemplateRequest,
        currentUser: CurrentUser,
    ): ModelTemplate {
        val template = ModelTemplate(
            id = UUID.randomUUID(),
            code = req.code,
            name = req.name,
            description = req.description,
            solverType = req.solverType,
            validators = emptyList(),
            recommendedMaxStep = req.recommendedMaxStep,
            recommendedRelTol = req.recommendedRelTol,
            recommendedAbsTol = req.recommendedAbsTol,
        )
        val saved = modelTemplateRepository.save(template)
        log.info("Admin {} создал шаблон модели {} ({})", currentUser.email, saved.code, saved.id)
        auditLogService.log(
            actor = userService.getById(currentUser.id),
            action = AuditActions.MODEL_CREATE,
            targetType = "MODEL",
            targetId = saved.id,
            payload = mapOf("code" to saved.code, "name" to saved.name),
        )
        return saved
    }

    @Operation(summary = "Обновить шаблон модели")
    @PutMapping("/{id}")
    fun update(
        @PathVariable id: UUID,
        @RequestBody req: UpdateModelTemplateRequest,
        currentUser: CurrentUser,
    ): ModelTemplate {
        val existing = modelTemplateRepository.findById(id)
            ?: throw NotFoundException("ModelTemplate", id)
        val updated = existing.copy(
            code = req.code,
            name = req.name,
            description = req.description,
            solverType = req.solverType,
            recommendedMaxStep = req.recommendedMaxStep,
            recommendedRelTol = req.recommendedRelTol,
            recommendedAbsTol = req.recommendedAbsTol,
        )
        val saved = modelTemplateRepository.save(updated)
        log.info("Admin {} обновил шаблон модели {}", currentUser.email, id)
        auditLogService.log(
            actor = userService.getById(currentUser.id),
            action = AuditActions.MODEL_UPDATE,
            targetType = "MODEL",
            targetId = id,
            payload = mapOf("code" to saved.code, "name" to saved.name),
        )
        return saved
    }

    @Operation(summary = "Удалить шаблон модели")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: UUID, currentUser: CurrentUser) {
        val existing = modelTemplateRepository.findById(id)
            ?: throw NotFoundException("ModelTemplate", id)
        modelTemplateRepository.deleteById(id)
        log.info("Admin {} удалил шаблон модели {} ({})", currentUser.email, existing.code, id)
        auditLogService.log(
            actor = userService.getById(currentUser.id),
            action = AuditActions.MODEL_DELETE,
            targetType = "MODEL",
            targetId = id,
            payload = mapOf("code" to existing.code),
        )
    }
}
