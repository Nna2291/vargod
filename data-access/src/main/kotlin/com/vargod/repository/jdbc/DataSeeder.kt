package com.vargod.repository.jdbc

import com.vargod.domain.PersistenceException
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.SQLException
import java.sql.Types
import java.util.UUID
import javax.sql.DataSource

/**
 * Идемпотентное заполнение БД эталонными шаблонами моделей.
 *
 * Входит в компонент доступа к данным: вызывается при старте любого приложения
 * (CLI или API), поэтому не зависит от модуля бизнес-логики. Коды моделей —
 * строковые литералы; они должны совпадать с кодами ОДУ-моделей в business-logic
 * (SirModel.CODE = "SIR", LotkaVolterraModel.CODE = "LOTKA_VOLTERRA",
 * LogisticGrowthModel.CODE = "LOGISTIC", SeirdModel.CODE = "SEIRD",
 * FitzHughNagumoModel.CODE = "FITZHUGH_NAGUMO",
 * HodgkinHuxleyModel.CODE = "HODGKIN_HUXLEY",
 * RosenzweigMacArthurModel.CODE = "ROSENZWEIG_MACARTHUR").
 *
 * Идемпотентность — по коду модели: при каждом старте проверяем `code` и
 * вставляем только отсутствующие. Это позволяет докатить новые модели на уже
 * заселённой dev-БД без миграций данных.
 */
class DataSeeder(
    private val dataSource: DataSource,
    private val codec: JsonCodec = JsonCodec(),
) {
    private val log = LoggerFactory.getLogger(DataSeeder::class.java)

    /** Обратно-совместимая обёртка над per-code seeding. */
    fun seedIfEmpty() = seedAllIfMissing()

    fun seedAllIfMissing() {
        try {
            dataSource.connection.use { conn ->
                conn.autoCommit = false
                try {
                    val seeders: List<Pair<String, (Connection) -> Unit>> = listOf(
                        "SIR" to ::seedSir,
                        "LOTKA_VOLTERRA" to ::seedLotkaVolterra,
                        "LOGISTIC" to ::seedLogistic,
                        "SEIRD" to ::seedSeird,
                        "FITZHUGH_NAGUMO" to ::seedFitzHughNagumo,
                        "HODGKIN_HUXLEY" to ::seedHodgkinHuxley,
                        "ROSENZWEIG_MACARTHUR" to ::seedRosenzweigMacArthur,
                    )
                    val added = mutableListOf<String>()
                    for ((code, seed) in seeders) {
                        if (existsByCode(conn, code)) continue
                        seed(conn)
                        added += code
                    }
                    conn.commit()
                    if (added.isEmpty()) {
                        log.info("БД уже содержит все эталонные модели — заполнение пропущено")
                    } else {
                        log.info("В БД добавлены модели: {}", added.joinToString(", "))
                    }
                } catch (e: SQLException) {
                    conn.rollback()
                    log.error("Ошибка заполнения БД, транзакция откатана: {}", e.message, e)
                    throw PersistenceException("Failed to seed model templates: ${e.message}", e)
                } finally {
                    conn.autoCommit = true
                }
            }
        } catch (e: SQLException) {
            log.error("Не удалось получить соединение для заполнения БД: {}", e.message, e)
            throw PersistenceException("Failed to obtain connection for seeding: ${e.message}", e)
        }
    }

    private fun existsByCode(conn: Connection, code: String): Boolean =
        conn.prepareStatement("SELECT 1 FROM model_templates WHERE code = ?").use { ps ->
            ps.setString(1, code)
            ps.executeQuery().use { it.next() }
        }

    private fun seedSir(conn: Connection) {
        val id = UUID.randomUUID()
        insertModel(
            conn, id, "SIR",
            name = "SIR — модель эпидемии",
            description = "S/I/R: восприимчивые, заражённые, выздоровевшие; контактный коэффициент β и скорость выздоровления γ.",
        )
        insertRange(conn, id, "beta", min = 0.0, max = 10.0)
        insertRange(conn, id, "gamma", min = 0.0, max = 10.0)
        insertRange(conn, id, "N", min = 0.0, minExclusive = true)
        insertRange(conn, id, "S", min = 0.0)
        insertRange(conn, id, "I", min = 0.0)
        insertRange(conn, id, "R", min = 0.0)
        insertConstraint(
            conn, id, "S+I+R=N",
            mapOf(
                "type" to "SUM_EQUALS_FIELD",
                "fields" to listOf("S", "I", "R"),
                "expectedField" to "N",
                "tolerance" to 1e-6,
            ),
        )
    }

    private fun seedLotkaVolterra(conn: Connection) {
        val id = UUID.randomUUID()
        insertModel(
            conn, id, "LOTKA_VOLTERRA",
            name = "Lotka-Volterra — хищник-жертва",
            description = "Динамика популяций жертв (x) и хищников (y) с коэффициентами α/β/γ/δ.",
        )
        listOf("alpha", "beta", "gamma", "delta", "x", "y").forEach {
            insertRange(conn, id, it, min = 0.0)
        }
    }

    private fun seedLogistic(conn: Connection) {
        val id = UUID.randomUUID()
        insertModel(
            conn, id, "LOGISTIC",
            name = "Логистический рост",
            description = "Популяция P с коэффициентом роста r и ёмкостью K.",
        )
        insertRange(conn, id, "r", min = 0.0)
        insertRange(conn, id, "K", min = 0.0, minExclusive = true)
        insertRange(conn, id, "P", min = 0.0)
    }

    private fun seedSeird(conn: Connection) {
        val id = UUID.randomUUID()
        insertModel(
            conn, id, "SEIRD",
            name = "SEIRD — эпидемия с инкубацией и смертностью",
            description = "Расширение SIR: добавлены экспонированные (E) и умершие (D). " +
                "Параметры: β (контакт), σ (инкубация), γ (выздоровление), μ (смертность), N (популяция).",
        )
        listOf("beta", "sigma", "gamma", "mu").forEach {
            insertRange(conn, id, it, min = 0.0)
        }
        insertRange(conn, id, "N", min = 0.0, minExclusive = true)
        listOf("S", "E", "I", "R", "D").forEach {
            insertRange(conn, id, it, min = 0.0)
        }
        insertConstraint(
            conn, id, "S+E+I+R+D=N",
            mapOf(
                "type" to "SUM_EQUALS_FIELD",
                "fields" to listOf("S", "E", "I", "R", "D"),
                "expectedField" to "N",
                "tolerance" to 1e-6,
            ),
        )
    }

    private fun seedFitzHughNagumo(conn: Connection) {
        val id = UUID.randomUUID()
        insertModel(
            conn, id, "FITZHUGH_NAGUMO",
            name = "FitzHugh-Nagumo — упрощённая возбудимая мембрана",
            description = "Двумерная редукция Hodgkin-Huxley: V (потенциал) и w (восстановление). " +
                "Параметры: a, b, tau, Iext.",
        )
        insertRange(conn, id, "tau", min = 0.0, minExclusive = true)
    }

    private fun seedHodgkinHuxley(conn: Connection) {
        val id = UUID.randomUUID()
        insertModel(
            conn, id, "HODGKIN_HUXLEY",
            name = "Hodgkin-Huxley — потенциал действия аксона",
            description = "Каноническая модель ионных каналов Na+/K+ с воротными переменными n/m/h. " +
                "Жёсткая система — подсказки решателя: maxStep=0.01, relTol=1e-6, absTol=1e-9.",
            recommendedMaxStep = 0.01,
            recommendedRelTol = 1e-6,
            recommendedAbsTol = 1e-9,
        )
        // Проводимости — неотрицательные; ёмкость — строго положительна.
        listOf("gNa", "gK", "gL").forEach {
            insertRange(conn, id, it, min = 0.0)
        }
        insertRange(conn, id, "Cm", min = 0.0, minExclusive = true)
        // Воротные переменные ограничены [0, 1].
        listOf("n", "m", "h").forEach {
            insertRange(conn, id, it, min = 0.0, max = 1.0)
        }
    }

    private fun seedRosenzweigMacArthur(conn: Connection) {
        val id = UUID.randomUUID()
        insertModel(
            conn, id, "ROSENZWEIG_MACARTHUR",
            name = "Rosenzweig-MacArthur — хищник-жертва с насыщением",
            description = "Лог. рост жертвы + функциональная реакция II типа: f(x)=a·x/(1+a·b·x). " +
                "Параметры: r, K, a, b, eps, m.",
        )
        listOf("r", "K", "a", "b", "eps", "m").forEach {
            insertRange(conn, id, it, min = 0.0, minExclusive = true)
        }
        insertRange(conn, id, "x", min = 0.0)
        insertRange(conn, id, "y", min = 0.0)
    }

    private fun insertModel(
        conn: Connection,
        id: UUID,
        code: String,
        name: String,
        description: String?,
        solverType: String = "RK45",
        recommendedMaxStep: Double? = null,
        recommendedRelTol: Double? = null,
        recommendedAbsTol: Double? = null,
    ) {
        conn.prepareStatement(
            "INSERT INTO model_templates " +
                "(id, code, name, description, solver_type, " +
                "recommended_max_step, recommended_rel_tol, recommended_abs_tol) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
        ).use { ps ->
            ps.setObject(1, id)
            ps.setString(2, code)
            ps.setString(3, name)
            ps.setString(4, description)
            ps.setString(5, solverType)
            setNullableDouble(ps, 6, recommendedMaxStep)
            setNullableDouble(ps, 7, recommendedRelTol)
            setNullableDouble(ps, 8, recommendedAbsTol)
            ps.executeUpdate()
        }
    }

    private fun setNullableDouble(ps: java.sql.PreparedStatement, idx: Int, value: Double?) {
        if (value == null) ps.setNull(idx, Types.DOUBLE) else ps.setDouble(idx, value)
    }

    private fun insertRange(
        conn: Connection,
        modelId: UUID,
        targetField: String,
        min: Double? = null,
        max: Double? = null,
        minExclusive: Boolean = false,
        maxExclusive: Boolean = false,
    ) {
        val rules = buildMap<String, Any> {
            min?.let { put("min", it) }
            max?.let { put("max", it) }
            if (minExclusive) put("minExclusive", true)
            if (maxExclusive) put("maxExclusive", true)
        }
        insertValidator(
            conn,
            modelId = modelId,
            name = "range[$targetField]",
            checkType = "RANGE",
            targetField = targetField,
            rules = rules,
        )
    }

    private fun insertConstraint(
        conn: Connection,
        modelId: UUID,
        name: String,
        rules: Map<String, Any>,
        targetField: String = "*",
    ) {
        insertValidator(
            conn,
            modelId = modelId,
            name = name,
            checkType = "CONSTRAINT",
            targetField = targetField,
            rules = rules,
        )
    }

    private fun insertValidator(
        conn: Connection,
        modelId: UUID,
        name: String,
        checkType: String,
        targetField: String,
        rules: Map<String, Any>,
    ) {
        conn.prepareStatement(
            "INSERT INTO validators (id, name, check_type, target_kind, target_field, rules, model_template_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)"
        ).use { ps ->
            ps.setObject(1, UUID.randomUUID())
            ps.setString(2, name)
            ps.setString(3, checkType)
            ps.setString(4, "PARAMETER")
            ps.setString(5, targetField)
            ps.setString(6, codec.encode(rules))
            ps.setObject(7, modelId)
            ps.executeUpdate()
        }
    }
}
