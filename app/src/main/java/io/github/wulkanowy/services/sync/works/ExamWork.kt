package io.github.wulkanowy.services.sync.works

import io.github.wulkanowy.data.db.entities.Semester
import io.github.wulkanowy.data.db.entities.Student
import io.github.wulkanowy.data.repositories.ExamRepository
import io.github.wulkanowy.data.waitForResult
import io.github.wulkanowy.services.sync.notifications.NewExamNotification
import kotlinx.coroutines.flow.first
import java.time.LocalDate.now
import javax.inject.Inject

class ExamWork @Inject constructor(
    private val examRepository: ExamRepository,
    private val newExamNotification: NewExamNotification,
) : Work {

    override suspend fun doWork(student: Student, semester: Semester, notify: Boolean) {
        examRepository.getExams(
            student = student,
            semester = semester,
            start = now(),
            end = now(),
            forceRefresh = true,
            notify = notify,
        ).waitForResult()

        examRepository.getExamsFromDatabase(semester, now()).first()
            .filter { !it.isNotified }.let {
                if (it.isNotEmpty()) newExamNotification.notify(it, student)

                examRepository.updateExam(it.onEach { exam -> exam.isNotified = true })
            }
    }
}
