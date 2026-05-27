package com.vargod.api.security

import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.MACSigner
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import com.vargod.config.AppConfig
import com.vargod.domain.User
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.Date
import javax.crypto.spec.SecretKeySpec

/**
 * Выдача access-токенов JWT (HS256).
 *
 * Разбор/валидация токенов выполняется автоматически Spring Security через
 * `oauth2ResourceServer { jwt() }` и `NimbusJwtDecoder.withSecretKey(...)`,
 * который мы регистрируем в [com.vargod.api.security.SecurityConfig].
 */
@Service
class JwtService(private val config: AppConfig) {

    private val signer = MACSigner(secretBytes(config.jwtSecret))

    fun issue(user: User): IssuedToken {
        val now = Instant.now()
        val expires = now.plusSeconds(config.jwtTtlSeconds)
        val claims = JWTClaimsSet.Builder()
            .subject(user.id.toString())
            .issuer(ISSUER)
            .audience(AUDIENCE)
            .claim("email", user.email)
            .claim("role", user.role.name)
            .issueTime(Date.from(now))
            .expirationTime(Date.from(expires))
            .build()
        val header = JWSHeader.Builder(JWSAlgorithm.HS256)
            .type(JOSEObjectType.JWT)
            .build()
        val jwt = SignedJWT(header, claims).apply { sign(signer) }
        return IssuedToken(token = jwt.serialize(), expiresAt = expires)
    }

    private fun secretBytes(secret: String): ByteArray {
        // Nimbus требует ≥256 бит (32 байта) для HS256. Если секрет слишком короткий,
        // дозабиваем нулями — лучше явная ошибка-предупреждение в логе, чем падение.
        val bytes = secret.toByteArray(Charsets.UTF_8)
        return if (bytes.size >= 32) bytes else bytes + ByteArray(32 - bytes.size)
    }

    companion object {
        const val ISSUER = "vargod"
        const val AUDIENCE = "vargod-api"

        fun secretKey(secret: String): SecretKeySpec {
            val bytes = secret.toByteArray(Charsets.UTF_8)
            val padded = if (bytes.size >= 32) bytes else bytes + ByteArray(32 - bytes.size)
            return SecretKeySpec(padded, "HmacSHA256")
        }
    }

    data class IssuedToken(val token: String, val expiresAt: Instant)
}
