package dev.pukan.metroprague.data.golemio

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStoreFactory
import java.io.ByteArrayInputStream
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class MetroSnapshotStoreTest {
    @get:Rule val folder = TemporaryFolder()

    @Test
    fun `stations metadata boards and timestamps survive reopening disk storage`() = runTest {
        val file = File(folder.root, "metro-data.json")
        val firstJob = SupervisorJob()
        val secondJob = SupervisorJob()
        val firstScope = CoroutineScope(Dispatchers.IO + firstJob)
        val secondScope = CoroutineScope(Dispatchers.IO + secondJob)
        val saved = MetroDataSnapshot(
            MetroTestData.catalogue,
            mapOf("andel" to SavedDepartureBoard(MetroTestData.board, 100)),
            123,
            1,
        )
        try {
            val first = DataStoreMetroSnapshotStore(DataStoreFactory.create(
                serializer = MetroSnapshotSerializer, scope = firstScope, produceFile = { file },
            ))
            first.replace(saved)
            firstScope.cancel()
            firstJob.join()
            val second = DataStoreMetroSnapshotStore(DataStoreFactory.create(
                serializer = MetroSnapshotSerializer, scope = secondScope, produceFile = { file },
            ))
            assertEquals(saved, second.data.first())
        } finally {
            firstScope.cancel()
            secondScope.cancel()
            firstJob.join()
            secondJob.join()
        }
    }

    @Test(expected = CorruptionException::class)
    fun `malformed disk data is reported as corruption`() = runTest {
        MetroSnapshotSerializer.readFrom(ByteArrayInputStream("broken-json".toByteArray()))
    }
}
