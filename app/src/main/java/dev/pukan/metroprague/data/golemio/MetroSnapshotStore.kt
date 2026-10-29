package dev.pukan.metroprague.data.golemio

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

internal interface MetroSnapshotStore {
    val data: Flow<MetroDataSnapshot>
    suspend fun replace(snapshot: MetroDataSnapshot)
}

internal class DataStoreMetroSnapshotStore @Inject constructor(
    private val store: DataStore<MetroDataSnapshot>,
) : MetroSnapshotStore {
    override val data = store.data
    override suspend fun replace(snapshot: MetroDataSnapshot) {
        store.updateData { snapshot }
    }
}

internal object MetroSnapshotSerializer : Serializer<MetroDataSnapshot> {
    private val json = Json { ignoreUnknownKeys = true }
    override val defaultValue = MetroDataSnapshot()

    override suspend fun readFrom(input: InputStream): MetroDataSnapshot = try {
        json.decodeFromString(input.readBytes().decodeToString())
    } catch (exception: SerializationException) {
        throw CorruptionException("Cannot read metro data", exception)
    }

    override suspend fun writeTo(t: MetroDataSnapshot, output: OutputStream) {
        output.write(json.encodeToString(MetroDataSnapshot.serializer(), t).encodeToByteArray())
    }
}
