package jp.sselect883.myalarm

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.webkit.*
import android.widget.*
import androidx.webkit.WebViewAssetLoader
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : Activity() {
    private lateinit var web: WebView
    private lateinit var status: TextView
    private val pages = setOf("/assets/index.html", "/assets/index_w.html", "/assets/setting.html")
    private fun trusted(uri: Uri) = uri.scheme == "https" &&
        uri.host == "appassets.androidplatform.net" && uri.path in pages
    private fun notificationsAllowed(): Boolean =
        getSystemService(NotificationManager::class.java).areNotificationsEnabled() &&
        getSystemService(NotificationManager::class.java).getNotificationChannel("ringing")?.importance != NotificationManager.IMPORTANCE_NONE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; fitsSystemWindows = true }
        status = TextView(this).apply { setPadding(16, 16, 16, 8) }
        root.addView(status)
        root.addView(LinearLayout(this).apply {
            addView(Button(this@MainActivity).apply {
                text = "アラーム許可"
                setOnClickListener {
                    if (Build.VERSION.SDK_INT >= 31 && !AlarmStore.permitted(this@MainActivity))
                        startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                            Uri.parse("package:$packageName")))
                    else Toast.makeText(this@MainActivity, "時刻どおりのアラームは許可済みです", Toast.LENGTH_SHORT).show()
                }
            }, LinearLayout.LayoutParams(0, -2, 1f))
            addView(Button(this@MainActivity).apply {
                text = "通知許可"
                setOnClickListener {
                    if (Build.VERSION.SDK_INT >= 33 &&
                        checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                        requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 10)
                    else startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
                }
            }, LinearLayout.LayoutParams(0, -2, 1f))
        })
        web = WebView(this)
        root.addView(web, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        val loader = WebViewAssetLoader.Builder().addPathHandler("/assets/",
            WebViewAssetLoader.AssetsPathHandler(this)).build()
        web.settings.apply {
            javaScriptEnabled = true
            allowFileAccess = false
            allowContentAccess = false
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        }
        web.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse {
                // Never expose the native bridge to remote HTML, frames or redirects.
                return loader.shouldInterceptRequest(request.url)
                    ?: WebResourceResponse("text/plain", "utf-8", 403, "Blocked", emptyMap(), "".byteInputStream())
            }
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
                !trusted(request.url)
        }
        web.addJavascriptInterface(Bridge(), "NativeAlarm")
        web.loadUrl("https://appassets.androidplatform.net/assets/index.html")
    }
    override fun onResume() {
        super.onResume()
        if (::status.isInitialized) status.text =
            "時刻指定: " + (if (AlarmStore.permitted(this)) "許可済み" else "要許可") +
            " ／ 通知: " + (if (notificationsAllowed()) "許可済み" else "要許可") +
            "\n音量は端末の「アラーム音量」で調整してください。"
    }
    override fun onDestroy() {
        web.removeJavascriptInterface("NativeAlarm")
        web.destroy()
        super.onDestroy()
    }
    @Deprecated("Platform back navigation")
    override fun onBackPressed() {
        if (web.canGoBack()) web.goBack() else super.onBackPressed()
    }

    inner class Bridge {
        private fun result(action: () -> Unit): String = try {
            action()
            JSONObject().put("ok", true).toString()
        } catch (e: Exception) {
            JSONObject().put("ok", false).put("error", e.message ?: "操作できませんでした").toString()
        }
        @JavascriptInterface fun list(): String =
            JSONArray(AlarmStore.list(this@MainActivity).map { it.json() }).toString()
        @JavascriptInterface fun add(hour: Int, minute: Int): String = result {
            check(notificationsAllowed()) { "先に上部の「通知許可」を設定してください" }
            AlarmStore.add(this@MainActivity, hour, minute)
        }
        @JavascriptInterface fun remove(id: Int): String = result { AlarmStore.remove(this@MainActivity, id) }
        @JavascriptInterface fun stop(): String = result {
            stopService(Intent(this@MainActivity, AlarmService::class.java))
        }
    }
}
