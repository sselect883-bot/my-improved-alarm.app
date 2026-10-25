package jp.sselect883.myalarm

import android.app.*
import android.content.*
import android.os.Build
import org.json.JSONArray
import org.json.JSONObject

data class Alarm(val id: Int, val hour: Int, val minute: Int, val at: Long) {
    fun json() = JSONObject().put("id", id).put("hour", hour).put("minute", minute).put("at", at)
}

object AlarmStore {
    private fun prefs(c: Context) = c.getSharedPreferences("alarms", Context.MODE_PRIVATE)
    @Synchronized fun list(c: Context): List<Alarm> {
        val a = JSONArray(prefs(c).getString("items", "[]"))
        return (0 until a.length()).map { i ->
            val x = a.getJSONObject(i)
            Alarm(x.getInt("id"), x.getInt("hour"), x.getInt("minute"), x.getLong("at"))
        }.sortedBy { it.at }
    }
    private fun save(c: Context, items: List<Alarm>) {
        check(prefs(c).edit().putString("items", JSONArray(items.map { it.json() }).toString()).commit())
    }
    fun permitted(c: Context): Boolean =
        Build.VERSION.SDK_INT < 31 || c.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
    private fun pending(c: Context, id: Int): PendingIntent = PendingIntent.getBroadcast(
        c, id, Intent(c, AlarmReceiver::class.java).putExtra("id", id),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    @android.annotation.SuppressLint("ScheduleExactAlarm")
    fun schedule(c: Context, a: Alarm) {
        check(permitted(c)) { "アラーム権限がありません" }
        val show = PendingIntent.getActivity(c, 0, Intent(c, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        c.getSystemService(AlarmManager::class.java)
            .setAlarmClock(AlarmManager.AlarmClockInfo(a.at, show), pending(c, a.id))
    }
    @Synchronized fun add(c: Context, hour: Int, minute: Int): Alarm {
        require(hour in 0..23 && minute in 0..59)
        check(permitted(c)) { "「アラームとリマインダー」を許可してください" }
        val old = list(c)
        check(old.none { it.hour == hour && it.minute == minute }) { "その時刻は設定済みです" }
        val id = prefs(c).getInt("nextId", 1)
        check(id < Int.MAX_VALUE) { "予約IDの上限です" }
        check(prefs(c).edit().putInt("nextId", id + 1).commit())
        val a = Alarm(id, hour, minute, AlarmTime.next(hour, minute))
        // Persist before scheduling so a receiver can always find the reservation.
        save(c, old + a)
        try { schedule(c, a) } catch (e: Exception) { save(c, old); throw e }
        return a
    }
    @Synchronized fun remove(c: Context, id: Int) {
        c.getSystemService(AlarmManager::class.java).cancel(pending(c, id))
        save(c, list(c).filterNot { it.id == id })
    }
    @Synchronized fun consume(c: Context, id: Int): Alarm? {
        val a = list(c).find { it.id == id } ?: return null
        save(c, list(c).filterNot { it.id == id })
        return a
    }
    @Synchronized fun restore(c: Context, clockChanged: Boolean = false) {
        if (!permitted(c)) return
        val now = System.currentTimeMillis()
        val items = list(c).mapNotNull { a ->
            if (clockChanged) a.copy(at = AlarmTime.next(a.hour, a.minute, now))
            else a.takeIf { it.at > now }
        }
        save(c, items)
        items.forEach { schedule(c, it) }
    }
}

class RestoreReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val known = setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED)
        if (intent.action !in known) return
        try {
            AlarmStore.restore(context, intent.action == Intent.ACTION_TIME_CHANGED ||
                intent.action == Intent.ACTION_TIMEZONE_CHANGED)
        } catch (e: Exception) {
            android.util.Log.e("MyAlarm", "Could not restore alarms", e)
        }
    }
}
