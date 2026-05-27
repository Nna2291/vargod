package com.vargod.ui.cli.command

import com.vargod.engine.ModelRegistry
import com.vargod.service.AuthService
import com.vargod.service.ComparisonService
import com.vargod.service.ExperimentService
import com.vargod.service.ModelService
import com.vargod.service.UserService
import com.vargod.ui.cli.format.ComparisonFormatter
import com.vargod.ui.cli.format.ExperimentFormatter
import com.vargod.ui.cli.format.ModelFormatter
import com.vargod.ui.cli.format.SimulationResultFormatter
import com.vargod.ui.cli.io.Console
import com.vargod.ui.cli.session.Session

data class CommandContext(
    val session: Session,
    val console: Console,
    val modelService: ModelService,
    val experimentService: ExperimentService,
    val comparisonService: ComparisonService,
    val authService: AuthService,
    val userService: UserService,
    val modelRegistry: ModelRegistry,
    val modelFormatter: ModelFormatter,
    val experimentFormatter: ExperimentFormatter,
    val simulationFormatter: SimulationResultFormatter,
    val comparisonFormatter: ComparisonFormatter,
)
