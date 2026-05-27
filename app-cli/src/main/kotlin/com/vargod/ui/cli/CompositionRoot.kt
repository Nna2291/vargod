package com.vargod.ui.cli

import com.vargod.config.AppConfig
import com.vargod.engine.ModelRegistry
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
import com.vargod.repository.jdbc.JdbcComparisonRepository
import com.vargod.repository.jdbc.JdbcExperimentRepository
import com.vargod.repository.jdbc.JdbcModelTemplateRepository
import com.vargod.repository.jdbc.JdbcUserRepository
import com.vargod.repository.jdbc.SchemaInitializer
import com.vargod.service.AuthService
import com.vargod.service.ComparisonService
import com.vargod.service.ExperimentService
import com.vargod.service.ModelService
import com.vargod.service.UserService
import com.vargod.service.impl.AuthServiceImpl
import com.vargod.service.impl.ComparisonServiceImpl
import com.vargod.service.impl.ExperimentServiceImpl
import com.vargod.service.impl.ModelServiceImpl
import com.vargod.service.impl.UserServiceImpl
import com.vargod.ui.cli.command.Command
import com.vargod.ui.cli.command.CommandContext
import com.vargod.ui.cli.command.impl.CreateComparisonCommand
import com.vargod.ui.cli.command.impl.CreateExperimentCommand
import com.vargod.ui.cli.command.impl.GuestCommand
import com.vargod.ui.cli.command.impl.HelpCommand
import com.vargod.ui.cli.command.impl.ListModelsCommand
import com.vargod.ui.cli.command.impl.ListMyComparisonsCommand
import com.vargod.ui.cli.command.impl.ListMyExperimentsCommand
import com.vargod.ui.cli.command.impl.ListPublicExperimentsCommand
import com.vargod.ui.cli.command.impl.LoginCommand
import com.vargod.ui.cli.command.impl.PublishExperimentCommand
import com.vargod.ui.cli.command.impl.QuitCommand
import com.vargod.ui.cli.command.impl.RegisterCommand
import com.vargod.ui.cli.command.impl.RunSimulationCommand
import com.vargod.ui.cli.command.impl.ShowComparisonCommand
import com.vargod.ui.cli.command.impl.ShowExperimentCommand
import com.vargod.ui.cli.command.impl.ShowModelCommand
import com.vargod.ui.cli.command.impl.WhoamiCommand
import com.vargod.ui.cli.format.ComparisonFormatter
import com.vargod.ui.cli.format.ExperimentFormatter
import com.vargod.ui.cli.format.ModelFormatter
import com.vargod.ui.cli.format.SimulationResultFormatter
import com.vargod.ui.cli.io.Console
import com.vargod.ui.cli.io.SystemConsole
import com.vargod.ui.cli.router.CommandRouter
import com.vargod.ui.cli.session.Session
import org.slf4j.LoggerFactory

/**
 * Корень композиции CLI-приложения.
 *
 * Связывает три отдельно собранных компонента — доступ к данным, бизнес-логику
 * и технологический UI — в единое работающее приложение. Параметры берутся из
 * внешней конфигурации [AppConfig].
 */
class CompositionRoot(
    private val config: AppConfig,
    private val console: Console = SystemConsole(),
) {

    private val log = LoggerFactory.getLogger(CompositionRoot::class.java)

    fun bootstrap(): Pair<MainLoop, Console> {
        log.info("Инициализация: подключение к БД {} (пользователь {})", config.dbUrl, config.dbUser)
        console.write("Connecting to ${config.dbUrl} as ${config.dbUser} ...")
        val dataSource = DataSourceFactory.postgres(config.dbUrl, config.dbUser, config.dbPassword)
        SchemaInitializer(dataSource).initialize()
        DataSeeder(dataSource).seedIfEmpty()
        log.info("Хранилище данных готово")

        val modelRegistry: ModelRegistry = MapModelRegistry(
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
        val constraintRegistry = MapConstraintRuleRegistry(
            listOf(RangeRule(), SumEqualsValueRule(), SumEqualsFieldRule(), OrderedRule())
        )
        // Ключевой параметр бизнес-логики (макс. шаг ОДУ-решателя) берётся из конфигурации.
        val simulationEngine = OdeSimulationEngine(
            modelRegistry,
            absoluteTolerance = config.simulationAbsoluteTolerance,
            relativeTolerance = config.simulationRelativeTolerance,
            maxStep = config.simulationMaxStep,
        )
        val validationEngine = RuleValidationEngine(constraintRegistry)
        log.info("Движок симуляции настроен: maxStep={}", config.simulationMaxStep)

        val experimentRepo = JdbcExperimentRepository(dataSource)
        val modelTemplateRepo = JdbcModelTemplateRepository(dataSource)
        val comparisonRepo = JdbcComparisonRepository(dataSource, experimentRepo)
        val userRepo = JdbcUserRepository(dataSource)

        val modelService: ModelService = ModelServiceImpl(modelTemplateRepo)
        val experimentService: ExperimentService = ExperimentServiceImpl(
            experimentRepository = experimentRepo,
            modelRepository = modelTemplateRepo,
            simulationEngine = simulationEngine,
            validationEngine = validationEngine,
        )
        val comparisonService: ComparisonService = ComparisonServiceImpl(
            comparisonRepository = comparisonRepo,
            experimentRepository = experimentRepo,
        )
        val userService: UserService = UserServiceImpl(userRepo)
        val authService: AuthService = AuthServiceImpl(userRepo, userService)

        val simulationFormatter = SimulationResultFormatter()
        val experimentFormatter = ExperimentFormatter(simulationFormatter)
        val modelFormatter = ModelFormatter(modelRegistry)
        val comparisonFormatter = ComparisonFormatter()

        val session = Session()
        val ctx = CommandContext(
            session = session,
            console = console,
            modelService = modelService,
            experimentService = experimentService,
            comparisonService = comparisonService,
            authService = authService,
            userService = userService,
            modelRegistry = modelRegistry,
            modelFormatter = modelFormatter,
            experimentFormatter = experimentFormatter,
            simulationFormatter = simulationFormatter,
            comparisonFormatter = comparisonFormatter,
        )

        lateinit var router: CommandRouter
        val helpCommand = HelpCommand { router.commands() }
        val commands: List<Command> = listOf(
            ListModelsCommand(),
            ShowModelCommand(),
            ListPublicExperimentsCommand(),
            ShowExperimentCommand(),
            CreateExperimentCommand(),
            RunSimulationCommand(),
            ListMyExperimentsCommand(),
            PublishExperimentCommand(),
            CreateComparisonCommand(),
            ListMyComparisonsCommand(),
            ShowComparisonCommand(),
            LoginCommand(),
            RegisterCommand(),
            GuestCommand(),
            WhoamiCommand(),
            helpCommand,
            QuitCommand(),
        )
        router = CommandRouter(commands)
        log.info("Сборка приложения завершена: зарегистрировано {} команд", commands.size)

        return MainLoop(router, ctx) to console
    }
}
