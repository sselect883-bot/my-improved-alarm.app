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
    @Before fun clearBefore() {
        if (android.os.Build.VERSION.SDK_INT >= 31)
            shell("appops set " + context.packageName + " SCHEDULE_EXACT_ALARM allow")
        if (android.os.Build.VERSION.SDK_INT >= 33)
            shell("pm grant " + context.packageName + " android.permission.POST_NOTIFICATIONS")
        clear()
    }
    @After fun clearAfter() {
        shell("dumpsys deviceidle unforce")
        shell("dumpsys battery reset")
        shell("input keyevent KEYCODE_WAKEUP")
        context.stopService(android.content.Intent(context, AlarmService::class.java))
        clear()
    }
    private fun shell(command: String) {
        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
            .use { descriptor ->
                android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor).bufferedReader().use { it.readText() }
            }
    }

    @Test fun scheduledAlarmStartsWhileScreenIsOffAndDeviceIsIdle() {
        val a = Alarm(9999, 12, 0, System.currentTimeMillis() + 20_000)
        context.getSharedPreferences("alarms", 0).edit()
            .putString("items", org.json.JSONArray(listOf(a.json())).toString()).commit()
        AlarmStore.schedule(context, a)
        shell("input keyevent KEYCODE_HOME")
        shell("input keyevent KEYCODE_SLEEP")
        shell("dumpsys battery unplug")
        shell("dumpsys deviceidle force-idle")
        val notifications = context.getSystemService(android.app.NotificationManager::class.java)
        val deadline = android.os.SystemClock.elapsedRealtime() + 40_000
        while (android.os.SystemClock.elapsedRealtime() < deadline &&
            notifications.activeNotifications.none { it.id == 1 }) Thread.sleep(250)
        assertTrue("Alarm foreground notification did not appear while idle",
            notifications.activeNotifications.any { it.id == 1 })
        Thread.sleep(1000)
        assertTrue(notifications.activeNotifications.any { it.id == 1 })
        assertTrue("Fired reservation was not consumed", AlarmStore.list(context).isEmpty())
    }
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
