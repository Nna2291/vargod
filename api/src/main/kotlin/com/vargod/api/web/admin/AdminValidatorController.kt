package com.vargod.api.web.admin

import com.vargod.api.security.CurrentUser
import com.vargod.domain.Validator
import com.vargod.service.AuditActions
import com.vargod.service.AuditLogService
import com.vargod.service.UserService
import com.vargod.service.ValidatorAdminService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

/**
 * Admin-CRUD валидаторов + интроспекция доступных типов правил.
 */
@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin: Validators", description = "CRUD валидаторов и доступные типы правил")
class AdminValidatorController(
    private val validatorAdminService: ValidatorAdminService,
    private val auditLogService: AuditLogService,
    private val userService: UserService,
) {

    private val log = LoggerFactory.getLogger(AdminValidatorController::class.java)

    @Operation(summary = "Список валидаторов; опционально фильтр по modelTemplateId")
    @GetMapping("/validators")
    fun list(@RequestParam(required = false) modelTemplateId: UUID?): List<ValidatorView> =
        validatorAdminService.list(modelTemplateId).map(ValidatorView::of)

    @Operation(summary = "Получить валидатор по ID")
    @GetMapping("/validators/{id}")
    fun get(@PathVariable id: UUID): ValidatorView =
        ValidatorView.of(validatorAdminService.get(id))

    @Operation(summary = "Создать валидатор и привязать к шаблону модели")
    @PostMapping("/validators")
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @RequestBody req: CreateValidatorRequest,
        currentUser: CurrentUser,
    ): ValidatorView {
        val validator = Validator(
            id = UUID.randomUUID(),
            name = req.name,
            checkType = req.checkType,
            targetKind = req.targetKind,
            targetField = req.targetField,
            rules = req.rules,
        )
        val saved = validatorAdminService.create(validator, req.modelTemplateId, currentUser.id)
        log.info("Admin {} создал валидатор {} (модель {})", currentUser.email, saved.name, req.modelTemplateId)
        auditLogService.log(
            actor = userService.getById(currentUser.id),
            action = AuditActions.VALIDATOR_CREATE,
            targetType = "VALIDATOR",
            targetId = saved.id,
            payload = mapOf(
                "name" to saved.name,
                "checkType" to saved.checkType.name,
                "modelTemplateId" to req.modelTemplateId,
            ),
        )
        return ValidatorView.of(saved)
    }

    @Operation(summary = "Обновить валидатор")
    @PutMapping("/validators/{id}")
    fun update(
        @PathVariable id: UUID,
        @RequestBody req: UpdateValidatorRequest,
        currentUser: CurrentUser,
    ): ValidatorView {
        val validator = Validator(
            id = id,
            name = req.name,
            checkType = req.checkType,
            targetKind = req.targetKind,
            targetField = req.targetField,
            rules = req.rules,
        )
        val saved = validatorAdminService.update(validator)
        log.info("Admin {} обновил валидатор {}", currentUser.email, id)
        auditLogService.log(
            actor = userService.getById(currentUser.id),
            action = AuditActions.VALIDATOR_UPDATE,
            targetType = "VALIDATOR",
            targetId = id,
            payload = mapOf("name" to saved.name, "checkType" to saved.checkType.name),
        )
        return ValidatorView.of(saved)
    }

    @Operation(summary = "Удалить валидатор")
    @DeleteMapping("/validators/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: UUID, currentUser: CurrentUser) {
        validatorAdminService.delete(id)
        log.info("Admin {} удалил валидатор {}", currentUser.email, id)
        auditLogService.log(
            actor = userService.getById(currentUser.id),
            action = AuditActions.VALIDATOR_DELETE,
            targetType = "VALIDATOR",
            targetId = id,
        )
    }

    /**
     * Список доступных типов правил и их paramsSchema — фронт строит по нему
     * динамическую форму создания валидатора.
     */
    @Operation(summary = "Доступные типы правил и их paramsSchema (для динамической формы)")
    @GetMapping("/validator-types")
    fun availableTypes(): List<ValidatorTypeView> =
        validatorAdminService.listAvailableTypes().map {
            ValidatorTypeView(type = it.type, paramsSchema = it.paramsSchema)
        }
}
