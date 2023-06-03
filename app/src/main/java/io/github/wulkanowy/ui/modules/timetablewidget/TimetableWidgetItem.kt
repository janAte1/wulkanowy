package io.github.wulkanowy.ui.modules.timetablewidget

import io.github.wulkanowy.data.db.entities.Timetable

sealed class TimetableWidgetItem(val type: TimetableWidgetItemType) {

    data class Normal(
        val lesson: Timetable,
    ) : TimetableWidgetItem(TimetableWidgetItemType.NORMAL)

    data class Empty(
        val numFrom: Int,
        val numTo: Int
    ) : TimetableWidgetItem(TimetableWidgetItemType.EMPTY)
}

enum class TimetableWidgetItemType {
    NORMAL,
    EMPTY
}
