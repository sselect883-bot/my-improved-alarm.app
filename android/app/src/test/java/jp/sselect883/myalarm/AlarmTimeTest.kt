package jp.sselect883.myalarm

import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class AlarmTimeTest {
    private fun time(hour: Int, minute: Int, second: Int = 0) =
        Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 26, hour, minute, second)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    @Test fun upcomingTimeIsToday() {
        assertEquals(time(8, 30), AlarmTime.next(8, 30, time(8, 29, 59)))
    }
    @Test fun matchingMinuteSchedulesTomorrow() {
        val expected = Calendar.getInstance().apply {
            timeInMillis = time(8, 30); add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis
        assertEquals(expected, AlarmTime.next(8, 30, time(8, 30)))
    }
    @Test fun midnightRollsToNextDay() {
        val expected = Calendar.getInstance().apply {
            timeInMillis = time(0, 0); add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis
        assertEquals(expected, AlarmTime.next(0, 0, time(23, 59, 59)))
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsInvalidHour() { AlarmTime.next(24, 0) }
    @Test(expected = IllegalArgumentException::class) fun rejectsInvalidMinute() { AlarmTime.next(0, 60) }
}
