package com.vargod.support

import at.favre.lib.crypto.bcrypt.BCrypt

/** BCrypt-хелперы для тестов. cost = 4 — минимально допустимый, чтобы тесты были быстрыми. */
object Passwords {
    const val TEST_COST = 4

    fun hash(plain: String): String = BCrypt.withDefaults().hashToString(TEST_COST, plain.toCharArray())

    fun matches(plain: String, hash: String): Boolean = BCrypt.verifyer().verify(plain.toCharArray(), hash).verified
}
