package dev.pukan.metroprague.data.repository

import dev.pukan.metroprague.data.golemio.GolemioBoardService
import dev.pukan.metroprague.data.golemio.GolemioDepartureBoardParser
import dev.pukan.metroprague.data.golemio.MetroStopIds
import dev.pukan.metroprague.domain.model.DepartureBoard
import dev.pukan.metroprague.domain.repository.DepartureRepository
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive

internal class GolemioDepartureRepository @Inject constructor(
    private val service: GolemioBoardService,
    private val parser: GolemioDepartureBoardParser,
) : DepartureRepository {
    override fun getDepartureBoard(stationId: String): Flow<DepartureBoard> = flow {
        val stopIds = MetroStopIds.idsFor(stationId)
        if (stopIds.isEmpty()) {
            emit(DepartureBoard(stationId, emptyList(), isUnavailable = true))
            return@flow
        }

        while (currentCoroutineContext().isActive) {
            val board = try {
                parser.parse(stationId, service.getDepartures(stopIds))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                DepartureBoard(stationId, emptyList(), isUnavailable = true)
            }
            emit(board)
            delay(REFRESH_INTERVAL_MILLIS)
        }
    }

    private companion object {
        const val REFRESH_INTERVAL_MILLIS = 30_000L
    }
}
