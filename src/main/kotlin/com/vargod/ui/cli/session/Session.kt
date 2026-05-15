package com.vargod.ui.cli.session

import java.util.UUID

enum class Mode { GUEST, RESEARCHER }

class Session(initialUserId: UUID? = null) {
    var userId: UUID? = initialUserId
        private set
    val mode: Mode
        get() = if (userId == null) Mode.GUEST else Mode.RESEARCHER

    fun loginAs(userId: UUID) {
        this.userId = userId
    }

    fun logout() {
        this.userId = null
    }

    fun requireUserId(): UUID = userId
        ?: throw IllegalStateException("This command requires login (mode=GUEST). Use 'login' first.")
}
