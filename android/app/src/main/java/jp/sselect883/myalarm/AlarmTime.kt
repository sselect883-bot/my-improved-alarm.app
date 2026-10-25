package jp.sselect883.myalarm

import java.util.Calendar

object AlarmTime {
    fun next(hour: Int, minute: Int, now: Long = System.currentTimeMillis()): Long {
        require(hour in 0..23 && minute in 0..59)
        return Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= now) add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis
    }
}
