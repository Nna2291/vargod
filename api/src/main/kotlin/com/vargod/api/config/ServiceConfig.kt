package com.vargod.api.config

import com.vargod.config.AppConfig
import com.vargod.config.ConfigLoader
import com.vargod.domain.repository.AuditLogRepository
import com.vargod.domain.repository.ComparisonRepository
import com.vargod.domain.repository.ExperimentRepository
import com.vargod.domain.repository.ModelTemplateRepository
import com.vargod.domain.repository.UserRepository
import com.vargod.domain.repository.ValidatorRepository
import com.vargod.engine.ModelRegistry
import com.vargod.engine.SimulationEngine
import com.vargod.engine.ValidationEngine
import com.vargod.engine.ode.FitzHughNagumoModel
import com.vargod.engine.ode.HodgkinHuxleyModel
import com.vargod.engine.ode.LogisticGrowthModel
import com.vargod.engine.ode.LotkaVolterraModel
import com.vargod.engine.ode.MapModelRegistry
import com.vargod.engine.ode.OdeSimulationEngine
import com.vargod.engine.ode.RosenzweigMacArthurModel
import com.vargod.engine.ode.SeirdModel
import com.vargod.engine.ode.SirModel
import com.vargod.engine.validation.MapConstraintRuleRegistry
import com.vargod.engine.validation.OrderedRule
import com.vargod.engine.validation.RangeRule
import com.vargod.engine.validation.RuleValidationEngine
import com.vargod.engine.validation.SumEqualsFieldRule
import com.vargod.engine.validation.SumEqualsValueRule
import com.vargod.repository.jdbc.DataSeeder
import com.vargod.repository.jdbc.DataSourceFactory
import com.vargod.repository.jdbc.JdbcAuditLogRepository
import com.vargod.repository.jdbc.JdbcComparisonRepository
import com.vargod.repository.jdbc.JdbcExperimentRepository
import com.vargod.repository.jdbc.JdbcModelTemplateRepository
import com.vargod.repository.jdbc.JdbcUserRepository
import com.vargod.repository.jdbc.JdbcValidatorRepository
import com.vargod.repository.jdbc.SchemaInitializer
import com.vargod.service.AdminUserService
import com.vargod.service.AuditLogService
import com.vargod.service.AuthService
import com.vargod.service.ComparisonService
import com.vargod.service.ExperimentService
import com.vargod.service.ModelService
import com.vargod.service.UserService
import com.vargod.service.ValidatorAdminService
import com.vargod.service.impl.AdminUserServiceImpl
import com.vargod.service.impl.AuditLogServiceImpl
import com.vargod.service.impl.AuthServiceImpl
import com.vargod.service.impl.ComparisonServiceImpl
import com.vargod.service.impl.ExperimentServiceImpl
import com.vargod.service.impl.ModelServiceImpl
import com.vargod.service.impl.UserServiceImpl
import com.vargod.service.impl.ValidatorAdminServiceImpl
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

/**
 * Корень композиции API-сервера — Spring-аналог CompositionRoot из CLI.
 *
 * Связывает отдельно собранные компоненты (доступ к данным, бизнес-логика) в
 * единый граф bean-ов. Параметры берутся из общей конфигурации [ConfigLoader].
 */
@Configuration
class ServiceConfig {

    private val log = LoggerFactory.getLogger(ServiceConfig::class.java)

    /** Единая загрузка AppConfig — потом инжектится во все, кому надо. */
    @Bean
    fun appConfig(): AppConfig = ConfigLoader.load()

    @Bean
    fun dataSource(config: AppConfig): DataSource {
        log.info("Инициализация: подключение к БД {}", config.dbUrl)
        val ds = DataSourceFactory.postgres(config.dbUrl, config.dbUser, config.dbPassword)
        SchemaInitializer(ds).initialize()
        DataSeeder(ds).seedIfEmpty()
        log.info("Хранилище данных готово")
        return ds
    }

    @Bean
    fun modelRegistry(): ModelRegistry =
        MapModelRegistry(
            listOf(
                SirModel(),
                LotkaVolterraModel(),
                LogisticGrowthModel(),
                SeirdModel(),
                FitzHughNagumoModel(),
                HodgkinHuxleyModel(),
                RosenzweigMacArthurModel(),
            )
        )

    @Bean
    fun simulationEngine(modelRegistry: ModelRegistry, config: AppConfig): SimulationEngine {
        log.info("Движок симуляции настроен: maxStep={}", config.simulationMaxStep)
        return OdeSimulationEngine(
            modelRegistry,
            absoluteTolerance = config.simulationAbsoluteTolerance,
            relativeTolerance = config.simulationRelativeTolerance,
            maxStep = config.simulationMaxStep,
        )
    }

    @Bean
    fun constraintRuleRegistry(): com.vargod.engine.ConstraintRuleRegistry =
        MapConstraintRuleRegistry(
            listOf(RangeRule(), SumEqualsValueRule(), SumEqualsFieldRule(), OrderedRule())
        )

    @Bean
    fun validationEngine(
        constraintRuleRegistry: com.vargod.engine.ConstraintRuleRegistry,
    ): ValidationEngine = RuleValidationEngine(constraintRuleRegistry)

    @Bean
    fun experimentRepository(dataSource: DataSource): ExperimentRepository =
        JdbcExperimentRepository(dataSource)

    @Bean
    fun modelTemplateRepository(dataSource: DataSource): ModelTemplateRepository =
        JdbcModelTemplateRepository(dataSource)

    @Bean
    fun comparisonRepository(
        dataSource: DataSource,
        experimentRepository: ExperimentRepository,
    ): ComparisonRepository = JdbcComparisonRepository(dataSource, experimentRepository)

    @Bean
    fun userRepository(dataSource: DataSource): UserRepository = JdbcUserRepository(dataSource)

    @Bean
    fun modelService(modelTemplateRepository: ModelTemplateRepository): ModelService =
        ModelServiceImpl(modelTemplateRepository)

    @Bean
    fun experimentService(
        experimentRepository: ExperimentRepository,
        modelTemplateRepository: ModelTemplateRepository,
        simulationEngine: SimulationEngine,
        validationEngine: ValidationEngine,
    ): ExperimentService = ExperimentServiceImpl(
        experimentRepository = experimentRepository,
        modelRepository = modelTemplateRepository,
        simulationEngine = simulationEngine,
        validationEngine = validationEngine,
    )

    @Bean
    fun comparisonService(
        comparisonRepository: ComparisonRepository,
        experimentRepository: ExperimentRepository,
    ): ComparisonService = ComparisonServiceImpl(
        comparisonRepository = comparisonRepository,
        experimentRepository = experimentRepository,
    )

    @Bean
    fun userService(userRepository: UserRepository): UserService = UserServiceImpl(userRepository)

    @Bean
    fun authService(userRepository: UserRepository, userService: UserService): AuthService =
        AuthServiceImpl(userRepository = userRepository, userService = userService)

    // --- P3a admin -------------------------------------------------------

    @Bean
    fun validatorRepository(dataSource: DataSource): ValidatorRepository =
        JdbcValidatorRepository(dataSource)

    @Bean
    fun auditLogRepository(dataSource: DataSource): AuditLogRepository =
        JdbcAuditLogRepository(dataSource)

    @Bean
    fun auditLogService(auditLogRepository: AuditLogRepository): AuditLogService =
        AuditLogServiceImpl(auditLogRepository)

    @Bean
    fun adminUserService(userService: UserService): AdminUserService =
        AdminUserServiceImpl(userService)

    @Bean
    fun validatorAdminService(
        validatorRepository: ValidatorRepository,
        constraintRuleRegistry: com.vargod.engine.ConstraintRuleRegistry,
    ): ValidatorAdminService = ValidatorAdminServiceImpl(validatorRepository, constraintRuleRegistry)
}
