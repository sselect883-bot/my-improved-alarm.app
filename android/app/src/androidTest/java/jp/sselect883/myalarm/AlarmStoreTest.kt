package jp.sselect883.myalarm

import android.app.AlarmManager
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AlarmStoreTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    @Before fun clearBefore() { clear() }
    @After fun clearAfter() { clear() }
    private fun clear() { AlarmStore.list(context).forEach { AlarmStore.remove(context, it.id) } }

    @Test fun reservationPersistsAndIsRegisteredWithOs() {
        val alarm = AlarmStore.add(context, 23, 55)
        assertEquals(alarm, AlarmStore.list(context).single())
        assertEquals(alarm.at, context.getSystemService(AlarmManager::class.java).nextAlarmClock.triggerTime)
        AlarmStore.remove(context, alarm.id)
        assertTrue(AlarmStore.list(context).isEmpty())
        assertNull(context.getSystemService(AlarmManager::class.java).nextAlarmClock)
    }
    @Test fun consumingOneAlarmLeavesOthers() {
        val a = AlarmStore.add(context, 10, 0)
        val b = AlarmStore.add(context, 11, 0)
        assertEquals(a, AlarmStore.consume(context, a.id))
        assertNull(AlarmStore.consume(context, a.id))
        assertEquals(listOf(b), AlarmStore.list(context))
        // consume models a fired one-shot alarm; clear its OS registration for test isolation.
        AlarmStore.remove(context, a.id)
    }
    @Test fun duplicateTimeIsRejected() {
        AlarmStore.add(context, 10, 0)
        try { AlarmStore.add(context, 10, 0); fail("duplicate accepted") }
        catch (_: IllegalStateException) { }
        assertEquals(1, AlarmStore.list(context).size)
    }
}
