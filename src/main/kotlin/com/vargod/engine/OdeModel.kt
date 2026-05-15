package com.vargod.engine

interface OdeModel {
    fun code(): String
    fun variables(): List<String>
    fun parameters(): List<String>
    fun derivatives(state: DoubleArray, params: Map<String, Double>, t: Double): DoubleArray
}
