package pbs.edu.fotokrzyk.data

class Repository(private val dao: MeasurementDao) {
    fun observeAll() = dao.observeAll()
    suspend fun insert(item: Measurement) = dao.insert(item)
    suspend fun clearAll() = dao.clearAll()
}
