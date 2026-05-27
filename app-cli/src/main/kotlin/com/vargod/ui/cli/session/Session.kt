package com.vargod.ui.cli.session

import com.vargod.domain.User
import java.util.UUID

enum class Mode { GUEST, RESEARCHER, ADMIN }

/**
 * Состояние сессии CLI. До P1 хранила только UUID (любой клиент мог выдать
 * себя за любого пользователя). Теперь — типизированный User?, заполняется
 * только после успешного `login email password` (см. LoginCommand).
 */
class Session(initialUser: User? = null) {

    var currentUser: User? = initialUser
        private set

    val userId: UUID?
        get() = currentUser?.id

    val mode: Mode
        get() = when (currentUser?.role?.name) {
            "ADMIN" -> Mode.ADMIN
            "RESEARCHER" -> Mode.RESEARCHER
            else -> Mode.GUEST
        }

    fun loginAs(user: User) {
        this.currentUser = user
    }

    fun logout() {
        this.currentUser = null
    }

    fun requireUserId(): UUID = userId
        ?: throw IllegalStateException("This command requires login. Use 'login <email>' first.")
}
