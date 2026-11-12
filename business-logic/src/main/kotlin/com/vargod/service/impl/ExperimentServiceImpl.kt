package com.vargod.service.impl

import com.vargod.domain.AuthorizationException
import com.vargod.domain.Experiment
import com.vargod.domain.ExperimentStatus
import com.vargod.domain.IllegalExperimentStateException
import com.vargod.domain.NotFoundException
import com.vargod.domain.ParameterSet
import com.vargod.domain.SimulationResult
import com.vargod.domain.ValidationException
import com.vargod.domain.ValidationStatus
import com.vargod.domain.dto.CreateExperimentRequest
import com.vargod.domain.repository.ExperimentRepository
import com.vargod.domain.repository.ModelTemplateRepository
import com.vargod.engine.SimulationEngine
import com.vargod.engine.ValidationEngine
import com.vargod.service.ExperimentService
import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.Instant
import java.util.UUID

class ExperimentServiceImpl(
    private val experimentRepository: ExperimentRepository,
    private val modelRepository: ModelTemplateRepository,
    private val simulationEngine: SimulationEngine,
    private val validationEngine: ValidationEngine,
    /**
     * Источник текущего времени для createdAt. Внедряется ради детерминированных
     * тестов (ТиОПО ЛР1): с Instant.now() момент создания нельзя проверить точно.
     */
    private val clock: Clock = Clock.systemUTC(),
) : ExperimentService {

    private val log = LoggerFactory.getLogger(ExperimentServiceImpl::class.java)

    override fun create(req: CreateExperimentRequest, userId: UUID): Experiment {
        log.info("Создание эксперимента '{}' (модель {}) пользователем {}", req.name, req.modelTemplateId, userId)
        require(req.tEnd > req.tStart) { "tEnd (${req.tEnd}) must be greater than tStart (${req.tStart})" }
        require(req.step > 0.0) { "step must be positive, got ${req.step}" }

        val model = modelRepository.findById(req.modelTemplateId)
            ?: throw NotFoundException("ModelTemplate", req.modelTemplateId)

        val parameterValues: Map<String, Any> = req.parameters
        val results = validationEngine.validate(parameterValues, model.validators)
        if (results.any { it.status == ValidationStatus.FAIL }) {
            log.warn("Параметры эксперимента '{}' не прошли валидацию", req.name)
            throw ValidationException(results)
        }

        val parameterSet = ParameterSet(
            id = UUID.randomUUID(),
            values = req.parameters,
            tStart = req.tStart,
            tEnd = req.tEnd,
            step = req.step,
        )
        val experiment = Experiment(
            id = UUID.randomUUID(),
            name = req.name,
            status = ExperimentStatus.PENDING,
            isPublic = false,
            userId = userId,
            modelTemplateId = model.id,
            parameterSet = parameterSet,
            result = null,
            createdAt = Instant.now(clock),
            // Snapshot валидаторов на момент создания (P2). Если admin позже
            // отредактирует или удалит валидатор — этот эксперимент сохранит
            // тот набор правил, которому соответствуют его параметры.
            validatorSnapshot = model.validators,
        )
        val saved = experimentRepository.save(experiment)
        log.info("Эксперимент {} создан", saved.id)
        return saved
    }

    override fun getById(id: UUID): Experiment {
        log.debug("Запрос эксперимента {}", id)
        return experimentRepository.findById(id) ?: throw NotFoundException("Experiment", id)
    }

    override fun listByUser(userId: UUID): List<Experiment> {
        log.debug("Список экспериментов пользователя {}", userId)
        return experimentRepository.findAllByUserId(userId)
    }

    override fun listPublic(): List<Experiment> {
        log.debug("Список публичных экспериментов")
        return experimentRepository.findAllPublic()
    }

    override fun runSimulation(experimentId: UUID): SimulationResult {
        log.info("Запуск симуляции эксперимента {}", experimentId)
        val experiment = getById(experimentId)
        if (experiment.status != ExperimentStatus.PENDING) {
            throw IllegalExperimentStateException(
                "Cannot run simulation for experiment ${experiment.id} in state ${experiment.status}"
            )
        }
        val params = experiment.parameterSet
            ?: throw IllegalExperimentStateException("Experiment ${experiment.id} has no parameter set")
        val model = modelRepository.findById(experiment.modelTemplateId)
            ?: throw NotFoundException("ModelTemplate", experiment.modelTemplateId)

        experimentRepository.save(experiment.copy(status = ExperimentStatus.RUNNING))

        return try {
            val result = simulationEngine.solve(model, params)
            experimentRepository.save(
                experiment.copy(status = ExperimentStatus.COMPLETED, result = result)
            )
            log.info("Симуляция эксперимента {} завершена успешно", experimentId)
            result
        } catch (e: Exception) {
            log.error("Симуляция эксперимента {} завершилась ошибкой: {}", experimentId, e.message, e)
            experimentRepository.save(experiment.copy(status = ExperimentStatus.FAILED))
            throw e
        }
    }

    override fun publish(id: UUID, userId: UUID): Experiment {
        log.info("Публикация эксперимента {} пользователем {}", id, userId)
        val experiment = getById(id)
        if (experiment.userId != userId) {
            throw AuthorizationException("User $userId is not the owner of experiment $id")
        }
        if (experiment.isPublic) return experiment
        return experimentRepository.save(experiment.copy(isPublic = true))
    }

    override fun unpublish(id: UUID, userId: UUID): Experiment {
        log.info("Снятие публичности эксперимента {} пользователем {}", id, userId)
        val experiment = getById(id)
        if (experiment.userId != userId) {
            throw AuthorizationException("User $userId is not the owner of experiment $id")
        }
        if (!experiment.isPublic) return experiment
        return experimentRepository.save(experiment.copy(isPublic = false))
    }
}
