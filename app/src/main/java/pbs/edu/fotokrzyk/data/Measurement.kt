package pbs.edu.fotokrzyk.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "measurements")
data class Measurement(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampMs: Long,
    val lat: Double?,
    val lng: Double?,
    val approxDb: Double?,
    val photoUri: String?
)
