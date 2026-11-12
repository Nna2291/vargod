package com.vargod.support

import com.vargod.engine.ConstraintRuleRegistry
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

/**
 * Production-сборка движков — так же, как в api/ServiceConfig и app-cli/CompositionRoot.
 * Используется классическими тестами: SUT работает с настоящими зависимостями.
 */
object RealEngines {
    fun allOdeModels() = listOf(
        SirModel(), LotkaVolterraModel(), LogisticGrowthModel(), SeirdModel(),
        FitzHughNagumoModel(), HodgkinHuxleyModel(), RosenzweigMacArthurModel(),
    )

    fun allRules() = listOf(RangeRule(), SumEqualsValueRule(), SumEqualsFieldRule(), OrderedRule())

    fun modelRegistry(): ModelRegistry = MapModelRegistry(allOdeModels())
    fun ruleRegistry(): ConstraintRuleRegistry = MapConstraintRuleRegistry(allRules())
    fun simulationEngine() = OdeSimulationEngine(modelRegistry())
    fun validationEngine() = RuleValidationEngine(ruleRegistry())
}
