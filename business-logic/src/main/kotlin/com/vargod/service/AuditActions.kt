package com.vargod.service

/**
 * Белый список кодов admin-действий, попадающих в audit_log.action.
 *
 * Намеренно строки, а не enum: значения должны переживать переименования
 * в коде (записи в БД уже зафиксированы); а enum-маппинг при чтении дал бы
 * IllegalArgumentException на «забытый» код. Список консультативный — БД
 * не enforce'ит CHECK CONSTRAINT.
 */
object AuditActions {
    const val USER_REGISTER = "USER_REGISTER"
    const val USER_LOGIN_SUCCESS = "USER_LOGIN_SUCCESS"
    const val USER_LOGIN_FAIL = "USER_LOGIN_FAIL"
    const val USER_CREATE = "USER_CREATE"
    const val USER_DELETE = "USER_DELETE"
    const val USER_ROLE_CHANGE = "USER_ROLE_CHANGE"
    const val MODEL_CREATE = "MODEL_CREATE"
    const val MODEL_UPDATE = "MODEL_UPDATE"
    const val MODEL_DELETE = "MODEL_DELETE"
    const val VALIDATOR_CREATE = "VALIDATOR_CREATE"
    const val VALIDATOR_UPDATE = "VALIDATOR_UPDATE"
    const val VALIDATOR_DELETE = "VALIDATOR_DELETE"
    const val EXPERIMENT_FORCE_DELETE = "EXPERIMENT_FORCE_DELETE"
}
