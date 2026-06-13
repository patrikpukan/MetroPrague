package dev.pukan.metroprague.domain.model

data class Station(
    val id: String,
    val name: String,
    val line: Line
)