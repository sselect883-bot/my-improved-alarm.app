package jp.sselect883.myalarm

import android.app.*
import android.content.*
import android.content.pm.ServiceInfo
import android.media.*
import android.os.*

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra("id", -1)
        if (AlarmStore.list(context).none { it.id == id }) return
        Handoff.acquire(context)
        try {
            context.startForegroundService(Intent(context, AlarmService::class.java).putExtra("id", id))
        } catch (e: Exception) {
            Handoff.release()
            android.util.Log.e("MyAlarm", "Could not start alarm", e)
        }
    }
}

// Hold the CPU only during the receiver-to-service handoff, never while waiting for an alarm.
private object Handoff {
    private var lock: PowerManager.WakeLock? = null
    @Synchronized fun acquire(c: Context) {
        if (lock?.isHeld == true) return
        lock = c.getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyAlarm:handoff").apply { acquire(20_000L) }
    }
    @Synchronized fun release() {
        lock?.let { if (it.isHeld) it.release() }
        lock = null
    }
}

class AlarmService : Service() {
    private var player: MediaPlayer? = null
    private var audio: AudioManager? = null
    private var focus: AudioFocusRequest? = null
    private val handler = Handler(Looper.getMainLooper())
    private val timeout = Runnable { stopSelf() }
    override fun onBind(intent: Intent?) = null
    override fun onCreate() {
        super.onCreate()
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel("ringing", "鳴動中のアラーム", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "アラームの停止操作"
                setSound(null, null) // MediaPlayer uses the alarm volume stream.
            })
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP") { stopSelf(); return START_NOT_STICKY }
        val id = intent?.getIntExtra("id", -1) ?: -1
        val alarm = AlarmStore.list(this).find { it.id == id }
        if (alarm == null) {
            Handoff.release()
            if (player == null) stopSelf()
            return START_NOT_STICKY
        }
        val stop = PendingIntent.getService(this, 0,
            Intent(this, AlarmService::class.java).setAction("STOP"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = Notification.Builder(this, "ringing")
            .setSmallIcon(R.drawable.ic_alarm).setContentTitle("アラーム")
            .setContentText("%02d:%02d — 停止をタップしてください".format(alarm.hour, alarm.minute))
            .setCategory(Notification.CATEGORY_ALARM).setOngoing(true)
            .setContentIntent(open).addAction(Notification.Action.Builder(null, "停止", stop).build())
            .build()
        try {
            if (Build.VERSION.SDK_INT >= 29)
                startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            else startForeground(1, notification)
            AlarmStore.consume(this, id)
            if (player == null) {
                val attrs = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
                audio = getSystemService(AudioManager::class.java)
                focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                    .setAudioAttributes(attrs).setOnAudioFocusChangeListener { /* Keep alarm semantics. */ }.build()
                audio?.requestAudioFocus(focus!!)
                player = MediaPlayer()
                player!!.apply {
                    setAudioAttributes(attrs)
                    resources.openRawResourceFd(R.raw.alarm).use { fd ->
                        setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
                    }
                    isLooping = true
                    setWakeMode(this@AlarmService, PowerManager.PARTIAL_WAKE_LOCK)
                    setOnErrorListener { _, _, _ -> stopSelf(); true }
                    prepare()
                    start()
                }
            }
            handler.removeCallbacks(timeout)
            handler.postDelayed(timeout, 10 * 60 * 1000L)
        } catch (e: Exception) {
            android.util.Log.e("MyAlarm", "Could not play alarm", e)
            stopSelf()
        } finally { Handoff.release() }
        return START_NOT_STICKY
    }
    override fun onDestroy() {
        handler.removeCallbacks(timeout)
        player?.release()
        player = null
        focus?.let { audio?.abandonAudioFocusRequest(it) }
        Handoff.release()
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }
}
