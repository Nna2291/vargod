package com.vargod.repository.jdbc

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import com.fasterxml.jackson.module.kotlin.readValue
import com.vargod.domain.PersistenceException

class JsonCodec(private val mapper: ObjectMapper = defaultMapper()) {

    fun encode(value: Any): String = try {
        mapper.writeValueAsString(value)
    } catch (e: JsonProcessingException) {
        throw PersistenceException("Failed to encode JSON: ${e.message}", e)
    }

    fun decodeDoubleMap(json: String): Map<String, Double> = try {
        mapper.readValue<Map<String, Number>>(json).mapValues { it.value.toDouble() }
    } catch (e: JsonProcessingException) {
        throw PersistenceException("Failed to decode Map<String, Double>: ${e.message}", e)
    }

    fun decodeAnyMap(json: String): Map<String, Any> = try {
        mapper.readValue(json, jacksonTypeRef<Map<String, Any>>())
    } catch (e: JsonProcessingException) {
        throw PersistenceException("Failed to decode Map<String, Any>: ${e.message}", e)
    }

    fun encodeSimulationData(timePoints: List<Double>, variables: Map<String, List<Double>>): String =
        encode(mapOf("timePoints" to timePoints, "variables" to variables))

    fun decodeSimulationData(json: String): Pair<List<Double>, Map<String, List<Double>>> = try {
        val raw = mapper.readValue(json, jacksonTypeRef<Map<String, Any>>())
        @Suppress("UNCHECKED_CAST")
        val times = (raw["timePoints"] as List<Number>).map { it.toDouble() }
        @Suppress("UNCHECKED_CAST")
        val vars = (raw["variables"] as Map<String, List<Number>>).mapValues { (_, v) -> v.map { it.toDouble() } }
        times to vars
    } catch (e: Exception) {
        throw PersistenceException("Failed to decode simulation data: ${e.message}", e)
    }

    companion object {
        fun defaultMapper(): ObjectMapper = jacksonObjectMapper()
    }
}
