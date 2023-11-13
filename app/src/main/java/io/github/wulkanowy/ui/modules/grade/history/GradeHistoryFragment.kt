package io.github.wulkanowy.ui.modules.grade.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import dagger.hilt.android.AndroidEntryPoint
import io.github.wulkanowy.R
import io.github.wulkanowy.data.db.dao.GradeHistoryDao
import io.github.wulkanowy.data.db.entities.GradeHistoryEntry
import io.github.wulkanowy.data.flatResourceFlow
import io.github.wulkanowy.data.onResourceData
import io.github.wulkanowy.data.onResourceError
import io.github.wulkanowy.data.onResourceIntermediate
import io.github.wulkanowy.data.onResourceNotLoading
import io.github.wulkanowy.data.repositories.GradeStatisticsRepository
import io.github.wulkanowy.data.repositories.PreferencesRepository
import io.github.wulkanowy.data.repositories.SemesterRepository
import io.github.wulkanowy.data.repositories.StudentRepository
import io.github.wulkanowy.databinding.FragmentGradeHistoryBinding
import io.github.wulkanowy.ui.base.BaseFragment
import io.github.wulkanowy.ui.base.ErrorHandler
import io.github.wulkanowy.ui.modules.grade.GradeFragment
import io.github.wulkanowy.ui.modules.grade.GradeView
import io.github.wulkanowy.utils.getThemeAttrColor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class GradeHistoryFragment :
    BaseFragment<FragmentGradeHistoryBinding>(R.layout.fragment_grade_history),
    GradeView.GradeChildView {
    @Inject
    lateinit var studentRepository: StudentRepository

    @Inject
    lateinit var semesterRepository: SemesterRepository

    @Inject
    lateinit var gradeStatisticsRepository: GradeStatisticsRepository

    @Inject
    lateinit var gradeHistoryDb: GradeHistoryDao

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    @Inject
    lateinit var gradeHistoryAdapter: GradeHistoryAdapter

    @Inject
    lateinit var errorHandler: ErrorHandler

    private lateinit var swipe: SwipeRefreshLayout
    private lateinit var recyclerView: RecyclerView
    private var scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_grade_history, container, false)
        recyclerView = view.findViewById(R.id.gradeHistoryRecycler)
        recyclerView.addItemDecoration(
            DividerItemDecoration(
                context, DividerItemDecoration.VERTICAL
            )
        )
        swipe = view.findViewById(R.id.gradeHistorySwipe)
        swipe.setColorSchemeColors(requireContext().getThemeAttrColor(R.attr.colorPrimary))
        swipe.setProgressBackgroundColorSchemeColor(
            requireContext().getThemeAttrColor(R.attr.colorSwipeRefresh)
        )
        swipe.setOnRefreshListener {
            (parentFragment as? GradeFragment)?.onChildRefresh()
        }
        recyclerView.adapter = gradeHistoryAdapter
        scope.launch {
            refreshData(null)
        }

        return view
    }

    companion object {
        @JvmStatic
        fun newInstance() = GradeHistoryFragment()
    }

    private fun showNewData(data: List<GradeHistoryEntry>) {
        val oldItemCount = gradeHistoryAdapter.itemCount
        gradeHistoryAdapter.items = data
        gradeHistoryAdapter.notifyItemRangeInserted(0, data.size - oldItemCount)
        recyclerView.layoutManager = LinearLayoutManager(context)
        gradeHistoryAdapter.gradeColorTheme = preferencesRepository.gradeColorTheme
    }

    private suspend fun refreshData(semesterId: Int?, forceRefresh: Boolean = false) {
        val student = studentRepository.getCurrentStudent(false)
        val semester = semesterRepository.getSemesters(student).find { it.semesterId == semesterId }
            ?: semesterRepository.getCurrentSemester(student)
        flatResourceFlow {
            gradeStatisticsRepository.getGradesPartialStatistics(
                student, semester, "Wszystkie", forceRefresh
            )
        }.onResourceData {
            val data = gradeHistoryDb.loadAll(
                semester.semesterId, student.studentId
            )
            activity?.runOnUiThread {
                showNewData(data)
            }
        }.catch { println(it) }.onResourceIntermediate {
            swipe.isRefreshing = true
        }.onResourceNotLoading {
            swipe.isRefreshing = false
        }.onResourceError(errorHandler::dispatch)
            .launchIn(scope)
    }

    override fun onParentChangeSemester() {
        Toast.makeText(context, "ran onParentChangeSemester", Toast.LENGTH_SHORT).show()
    }

    override fun onParentLoadData(semesterId: Int, forceRefresh: Boolean) {
        scope.launch {
            refreshData(semesterId, forceRefresh)
        }
        (parentFragment as? GradeFragment)?.onChildFragmentLoaded(semesterId)
    }

    override fun onParentReselected() {
        recyclerView.scrollToPosition(0)
    }

}
