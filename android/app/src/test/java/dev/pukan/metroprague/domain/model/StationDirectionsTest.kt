package dev.pukan.metroprague.domain.model

import dev.pukan.metroprague.data.repository.MockStationRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class StationDirectionsTest {

    private val repository = MockStationRepository()

    @Test
    fun `terminiOf returns both termini for every metro line`() = runTest {
        val stations = repository.getStations().first()

        assertEquals(
            listOf("Nemocnice Motol", "Depo Hostivař"),
            stations.terminiOf(Line.A).map { it.name },
        )
        assertEquals(
            listOf("Zličín", "Černý Most"),
            stations.terminiOf(Line.B).map { it.name },
        )
        assertEquals(
            listOf("Letňany", "Háje"),
            stations.terminiOf(Line.C).map { it.name },
        )
    }

    @Test
    fun `directionsAt mid-line station returns both line termini`() = runTest {
        val stations = repository.getStations().first()
        val andel = stations.first { it.id == "andel" }

        assertEquals(
            listOf(
                Direction(Line.B, "zlicin", "Zličín"),
                Direction(Line.B, "cerny-most", "Černý Most"),
            ),
            stations.directionsAt(andel),
        )
    }

    @Test
    fun `directionsAt terminus returns only opposite terminus`() = runTest {
        val stations = repository.getStations().first()
        val haje = stations.first { it.id == "haje" }

        assertEquals(
            listOf(Direction(Line.C, "letnany", "Letňany")),
            stations.directionsAt(haje),
        )
    }

    @Test
    fun `directionsAt interchange returns all directions in line and terminus order`() = runTest {
        val stations = repository.getStations().first()
        val muzeum = stations.first { it.id == "muzeum" }

        assertEquals(
            listOf(
                Direction(Line.A, "nemocnice-motol", "Nemocnice Motol"),
                Direction(Line.A, "depo-hostivar", "Depo Hostivař"),
                Direction(Line.C, "letnany", "Letňany"),
                Direction(Line.C, "haje", "Háje"),
            ),
            stations.directionsAt(muzeum),
        )
    }

    @Test
    fun `directionsAt output order is stable across repeated calls`() = runTest {
        val stations = repository.getStations().first()
        val muzeum = stations.first { it.id == "muzeum" }
        val firstResult = stations.directionsAt(muzeum)

        repeat(10) {
            assertEquals(firstResult, stations.directionsAt(muzeum))
        }
    }
}
