package io.github.wulkanowy.ui.modules.grade.history

import android.content.Context
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import io.github.wulkanowy.R
import io.github.wulkanowy.data.db.entities.GradeHistoryEntry
import io.github.wulkanowy.data.enums.GradeColorTheme
import io.github.wulkanowy.utils.toFormattedString
import javax.inject.Inject

class GradeHistoryAdapter @Inject constructor() :
    RecyclerView.Adapter<GradeHistoryAdapter.ViewHolder>() {
    lateinit var gradeColorTheme: GradeColorTheme
    private lateinit var context: Context
    var items = emptyList<GradeHistoryEntry>()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val subject: TextView
        val timestamp: TextView
        val grades: TextView

        init {
            subject = view.findViewById(R.id.gradeHistorySubject)
            timestamp = view.findViewById(R.id.gradeHistoryTimestamp)
            grades = view.findViewById(R.id.gradeHistoryGrades)
        }
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.item_grade_history, viewGroup, false)
        context = view.context
        return ViewHolder(view)
    }

    private val vulcanGradeColors = listOf(
        R.color.grade_vulcan_one,
        R.color.grade_vulcan_two,
        R.color.grade_vulcan_three,
        R.color.grade_vulcan_four,
        R.color.grade_vulcan_five,
        R.color.grade_vulcan_six
    )

    private val materialGradeColors = listOf(
        R.color.grade_material_one,
        R.color.grade_material_two,
        R.color.grade_material_three,
        R.color.grade_material_four,
        R.color.grade_material_five,
        R.color.grade_material_six
    )

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {

        val gradeColors = when (gradeColorTheme) {
            GradeColorTheme.VULCAN -> vulcanGradeColors
            else -> materialGradeColors
        }

        val entry = items[position]
        viewHolder.subject.text = entry.subject
        entry.timestamp.toFormattedString()
        viewHolder.timestamp.text = entry.timestamp.toFormattedString("yyyy-MM-dd HH:mm:ss")

        val gradesText = SpannableStringBuilder()
        entry.amountsDifference.forEachIndexed { grade: Int, amount: Int ->
            val col = ContextCompat.getColor(context, gradeColors[grade])
            gradesText.append("$amount ", ForegroundColorSpan(col), Spanned.SPAN_COMPOSING)
        }
        viewHolder.grades.text = gradesText
    }

    override fun getItemCount() = items.size

}

