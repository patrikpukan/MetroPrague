package dev.pukan.metroprague.domain.model

data class LinePosition(val line: Line, val order: Int)

data class Station(
    val id: String,
    val name: String,
    val lines: List<LinePosition>,
)
