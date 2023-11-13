package io.github.wulkanowy.data.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "GradeStatisticsHistory")
data class GradeHistoryEntry(
    val timestamp: Instant,

    val subject: String,

    @ColumnInfo(name = "student_id")
    val studentId: Int,

    @ColumnInfo(name = "semester_id")
    val semesterId: Int,
    @ColumnInfo(name = "amounts_difference")
    val amountsDifference: List<Int>,
) {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0
}

