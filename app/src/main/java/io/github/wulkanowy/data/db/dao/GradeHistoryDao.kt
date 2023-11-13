package io.github.wulkanowy.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import io.github.wulkanowy.data.db.entities.GradeHistoryEntry
import javax.inject.Singleton

@Singleton
@Dao
interface GradeHistoryDao : BaseDao<GradeHistoryEntry> {
    @Query("SELECT * FROM GradeStatisticsHistory WHERE semester_id = :semesterId AND student_id = :studentId ORDER BY timestamp DESC")
    suspend fun loadAll(semesterId: Int, studentId: Int): List<GradeHistoryEntry>
}
