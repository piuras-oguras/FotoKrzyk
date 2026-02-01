package pbs.edu.fotokrzyk.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MeasurementDao {
    @Insert
    suspend fun insert(item: Measurement): Long

    @Query("SELECT * FROM measurements ORDER BY timestampMs DESC")
    fun observeAll(): Flow<List<Measurement>>

    @Query("DELETE FROM measurements")
    suspend fun clearAll()
}
