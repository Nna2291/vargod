package com.vargod.ui.cli

import com.vargod.engine.ModelRegistry
import com.vargod.engine.ode.LogisticGrowthModel
import com.vargod.engine.ode.LotkaVolterraModel
import com.vargod.engine.ode.MapModelRegistry
import com.vargod.engine.ode.OdeSimulationEngine
import com.vargod.engine.ode.SirModel
import com.vargod.engine.validation.MapConstraintRuleRegistry
import com.vargod.engine.validation.OrderedRule
import com.vargod.engine.validation.RuleValidationEngine
import com.vargod.engine.validation.SumEqualsFieldRule
import com.vargod.engine.validation.SumEqualsValueRule
import com.vargod.repository.jdbc.DataSourceFactory
import com.vargod.repository.jdbc.JdbcComparisonRepository
import com.vargod.repository.jdbc.JdbcExperimentRepository
import com.vargod.repository.jdbc.JdbcModelTemplateRepository
import com.vargod.repository.jdbc.SchemaInitializer
import com.vargod.service.ComparisonService
import com.vargod.service.ExperimentService
import com.vargod.service.ModelService
import com.vargod.service.impl.ComparisonServiceImpl
import com.vargod.service.impl.ExperimentServiceImpl
import com.vargod.service.impl.ModelServiceImpl
import com.vargod.ui.cli.bootstrap.DataSeeder
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

data class CliConfig(
    val dbUrl: String,
    val dbUser: String,
    val dbPassword: String,
) {
    companion object {
        fun fromEnv(env: Map<String, String> = System.getenv()): CliConfig = CliConfig(
            dbUrl = env["DB_URL"] ?: "jdbc:postgresql://localhost:5432/vargod",
            dbUser = env["DB_USER"] ?: "vargod",
            dbPassword = env["DB_PASSWORD"] ?: "vargod",
        )
    }
}

class CompositionRoot(
    private val config: CliConfig,
    private val console: Console = SystemConsole(),
) {

    fun bootstrap(): Pair<MainLoop, Console> {
        console.write("Connecting to ${config.dbUrl} as ${config.dbUser} ...")
        val dataSource = DataSourceFactory.postgres(config.dbUrl, config.dbUser, config.dbPassword)
        SchemaInitializer(dataSource).initialize()
        DataSeeder(dataSource).seedIfEmpty()

        val modelRegistry: ModelRegistry = MapModelRegistry(
            listOf(SirModel(), LotkaVolterraModel(), LogisticGrowthModel())
        )
        val constraintRegistry = MapConstraintRuleRegistry(
            listOf(SumEqualsValueRule(), SumEqualsFieldRule(), OrderedRule())
        )
        val simulationEngine = OdeSimulationEngine(modelRegistry)
        val validationEngine = RuleValidationEngine(constraintRegistry)

        val experimentRepo = JdbcExperimentRepository(dataSource)
        val modelTemplateRepo = JdbcModelTemplateRepository(dataSource)
        val comparisonRepo = JdbcComparisonRepository(dataSource, experimentRepo)

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
            GuestCommand(),
            WhoamiCommand(),
            helpCommand,
            QuitCommand(),
        )
        router = CommandRouter(commands)

        return MainLoop(router, ctx) to console
    }
}
