package com.sgvdev.autostart.ui.activity

import android.Manifest
import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.*
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import java.util.Enumeration
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.core.view.isGone
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerView
import com.sgvdev.autostart.*
import com.sgvdev.autostart.models.AdRequest
import com.sgvdev.autostart.models.ContentX
import com.sgvdev.autostart.receivers.LockDeviceReceiver.Companion.ACTION_CLOSE
import com.sgvdev.autostart.receivers.ShutDownReceiver
import com.sgvdev.autostart.ui.view_model.MainActivityViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import java.io.File
import java.util.*
import javax.inject.Inject


private const val ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE = 2323
private const val RESULT_ENABLE_ADMIN_REQUEST_CODE = 1899
private const val RESULT_WRITE_INTERNAL_STORAGE_REQUEST_CODE = 2455

private const val LOCK_HOUR = 23
private const val LOCK_MINUTE = 0
private const val UNLOCK_HOUR = 8
private const val UNLOCK_MINUTE = 0
private const val MOCK_LOCK_HOUR = 12
private const val MOCK_LOCK_MINUTE = 10
private const val MOCK_UNLOCK_HOUR = 12
private const val MOCK_UNLOCK_MINUTE = 15

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var appSettings: AppSettings

    private val viewModel by viewModels<MainActivityViewModel>()

    private lateinit var webView: WebView
    private lateinit var imageAd: ImageView
    private lateinit var playerView: PlayerView

    private lateinit var deviceManager: DevicePolicyManager
    private lateinit var compName: ComponentName

    private lateinit var btnClearLog: View
    private lateinit var btnSendLogs: View
    private lateinit var btnOpenSettings: View

    private var clearLogClickCount = 5
    private var sendLogCount = 5

    private lateinit var shutDownReceiver: ShutDownReceiver
    private lateinit var shutDownReceiverIntentFilter: IntentFilter
    private lateinit var tvWebViewClient: TvWebViewClient

    private lateinit var settingsDialog: SettingsBottomSheetDialog

    lateinit var job: Job
    private val loggingExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.d("CoroutineException", "$throwable ${throwable.printStackTrace()}")
    }
    lateinit var localFragmentScope: CoroutineScope

    var needLoadBaseUrl = true

    private fun createJob() {
        job = SupervisorJob()
        localFragmentScope = CoroutineScope(Dispatchers.IO + job) + loggingExceptionHandler
    }

    private var adsSet = mutableSetOf<String>()
    private var mockAdsSet = mutableSetOf<String>(
        "https://vlifte.by/content/%20%20%20%201920-1032.jpg",
        "https://vlifte.by/content/TV_Screen_-_LIFT_-__-_FEB2020_-_1576x1080.jpg",
        "https://vlifte.by/content/%20.jpg",
        "https://vlifte.by/content/t/6_123_android.mp4"
    )

    private lateinit var exoPlayer: ExoPlayer

    private var repeatAd = true
    private var adPlaybackStarted = true

    private var currentCheckSum = ""

    private var currentAdList = mutableListOf<ContentX>()
    private var videoAdHashMap = hashMapOf<Int, String>()

    private var isCurrentAdVideo = false

    private lateinit var adJob: Job

    companion object {
        const val APP_TAG = "AUTOSTART:"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MY_TAG", "activity onStart")
        LogWriter.log(this, sBody = "MainActivity: onCreate")
        initShutDownReceiver()
        registerReceiver(shutDownReceiver, shutDownReceiverIntentFilter)
        setContentView(R.layout.activity_my)
        createJob()
        addWindowFlags()
        checkAdmin()
        bindViews()
        requestPermissions()
        tvWebViewClient = TvWebViewClient()
        settingsDialog = SettingsBottomSheetDialog(this, appSettings)
        getToken()
        observeViewModel()
    }

    private fun getToken() {
        lifecycleScope.launch {
            appSettings.saved.collect { settings ->
                if (settings.deviceToken.isEmpty()) {
                    webView.isGone = false
                    playerView.isGone = true
                    imageAd.isGone = true
                    initWebView()
                } else {
                    viewModel.getAd(AdRequest(settings.deviceToken))
                }
            }
        }
    }
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(null)
        intent?.let {
            if (it.getBooleanExtra(ACTION_CLOSE, false)) {
//                tvWebViewClient.isSleepLoadFinished = true
                Log.d(
                    "MainActivity: onNewIntent",
                    ""
                )
                Toast.makeText(this, "I should sleep!!", Toast.LENGTH_LONG).show()
                adJob.cancel()
                exoPlayer.stop()
                exoPlayer.release()
                tvWebViewClient.isSleepLoadFinished = true
            }
        }
    }

    private fun observeViewModel() {
        viewModel.apply {
            ad.observe(this@MainActivity) {
                Log.d("AD_RESPONSE", it.toString())
                it.success.checksum?.let { newChecksum ->
                    if (currentCheckSum != newChecksum) {
                        currentCheckSum = newChecksum
                        currentAdList.clear()
                        it.success.content?.let {
                            it.advertisement?.let {
                                currentAdList.addAll(it.content)
                                videoAdHashMap.clear()
                            }
                        }
                        playAd()
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        LogWriter.log(this, "MainActivity: onStop()")
    }

    private fun observeSettingsDialog() {
        settingsDialog.apply {
            webViewUrl.onEach { urlData ->
                loadUrl(urlData)
            }.launchWhenResumed(lifecycleScope)
        }
    }

    override fun onResume() {
        super.onResume()
        observeTvWebViewClient()
        observeSettingsDialog()
        needLoadBaseUrl = true
    }

    override fun onDestroy() {
        unregisterReceiver(
            shutDownReceiver
        )
        exoPlayer.release()
        LogWriter.log(this, "MainActivity: onDestroy()")
        super.onDestroy()
    }

    private fun initShutDownReceiver() {
        shutDownReceiver = ShutDownReceiver()
        shutDownReceiverIntentFilter =
            IntentFilter().apply {
                addAction(Intent.ACTION_SHUTDOWN)
                addAction(Intent.ACTION_POWER_DISCONNECTED)
                addAction(Intent.ACTION_POWER_CONNECTED)
                addAction("com.android.internal.intent.action.REQUEST_SHUTDOWN")
                addAction(Intent.ACTION_REBOOT)
            }
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
            RESULT_WRITE_INTERNAL_STORAGE_REQUEST_CODE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            !Settings.canDrawOverlays(this)
        ) {
            requestPermission()
        }
    }

    private fun initWebView() {

        Log.i("initWebView", "UA: " + webView.settings.userAgentString)
        webView.apply {
            webChromeClient = object : WebChromeClient() {
                override fun getDefaultVideoPoster(): Bitmap? {
                    val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    canvas.drawARGB(0, 0, 0, 0)
                    return bitmap
                }

                override fun onConsoleMessage(message: String, lineNumber: Int, sourceID: String) {
                    Log.d(
                        "initWebView", message + " -- From line "
                                + lineNumber + " of "
                                + sourceID
                    )
                }
            }
            webViewClient = tvWebViewClient
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = true
            }
            settingsDialog.getAdUrlFromAppSettings()
        }
    }

    private fun bindViews() {
        webView = findViewById(R.id.webView)
        imageAd = findViewById(R.id.iv_ad)
        btnClearLog = findViewById(R.id.btn_clear_logs)
        btnSendLogs = findViewById(R.id.btn_send_logs)

        exoPlayer = ExoPlayer.Builder(this).build()
        playerView = findViewById(R.id.player_view)
        playerView.useController = false

        btnClearLog.setOnClickListener {
            clearLogClickCount--
            if (clearLogClickCount == 0) {
                LogWriter.clearLog(this)
                Toast.makeText(this, "The log was cleared", Toast.LENGTH_SHORT).show()
                clearLogClickCount = 5
            } else {
                Toast.makeText(
                    this,
                    "Click $clearLogClickCount times to clear the log",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        val addresses = arrayOf("goshark2006@gmail.com")
        btnSendLogs.setOnClickListener {
            sendLogCount--
            if (sendLogCount == 0) {
                val i = Intent(Intent.ACTION_SEND).apply {
                    type = "*/*"
                    intent.data = Uri.parse("mailto:")
                    putExtra(Intent.EXTRA_EMAIL, addresses)
                    putExtra(Intent.EXTRA_SUBJECT, "Log files")
                    val file = File("${this@MainActivity.filesDir}/${LogWriter.LOG_FILE_NAME}")
                    val uri = FileProvider.getUriForFile(
                        this@MainActivity,
                        this@MainActivity.applicationContext.packageName + ".provider",
                        file
                    )
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                LogWriter.log(this, "MainActivity: sending logs via email")
                startActivity(Intent.createChooser(i, "Send Email Using: "))
                sendLogCount = 5
            } else {
                Toast.makeText(
                    this,
                    "Click $sendLogCount times to send the log",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        btnOpenSettings = findViewById(R.id.btn_open_settings_dialog)
        btnOpenSettings.setOnClickListener {


//            throw RuntimeException("Test Crash")
            settingsDialog.open()
        }
    }

    private fun playAd() {
        adJob = lifecycleScope.launch {
            while (true) {
                currentAdList.forEach { contentX ->
                    Log.d("exoPlayer", contentX.file.url)
                    webView.isGone = true
                    if (contentX.file.url.contains(".jpg") || contentX.file.url.contains(".jpeg")) {
                        imageAd.visibility = View.VISIBLE
                        playerView.visibility = View.INVISIBLE
                        Glide.with(imageAd).load(contentX.file.url).into(imageAd)
                        isCurrentAdVideo = false
                    } else {
                        imageAd.visibility = View.INVISIBLE
                        playerView.visibility = View.VISIBLE
                        exoPlayer = ExoPlayer.Builder(this@MainActivity).build()
                        playerView.player = exoPlayer
                        val mediaItem: MediaItem =
                            MediaItem.fromUri(contentX.file.url)
                        exoPlayer.addMediaItem(mediaItem)
                        exoPlayer.prepare()
                        exoPlayer.repeatMode = Player.REPEAT_MODE_OFF
                        exoPlayer.volume = 0f
                        exoPlayer.playWhenReady = true
                        isCurrentAdVideo = true
                        Log.d("MainActivity", "mediaItemCount ${exoPlayer.mediaItemCount}")
                    }
                    delay(/*if (isCurrentAdVideo) exoPlayer.duration + 3000L else*/ contentX.duration.toInt() * 1000L)

                    exoPlayer.clearMediaItems()
                    exoPlayer.release()
                }
            }
        }
    }

    private fun observeTvWebViewClient() {
        tvWebViewClient.apply {
            dataCount.onEach {
                if (it == 2) {
//                    playAd()
                }
            }.launchWhenResumed(lifecycleScope)
            ads.onEach {
                if (it.isNotEmpty()) {
                    adsSet.add(it)
                }
            }.launchWhenResumed(lifecycleScope)
            token.onEach { token ->
                launch {
                    if (token.isNotEmpty()) {
                        appSettings.setDeviceToken(token)
                        Log.d(
                            "MainActivity: WebView",
                            "onPageFinished: your current token = $token"
                        )
                        LogWriter.log(
                            this@MainActivity,
                            "MainActivity: WebView onPageFinished: your current token = $token"
                        )
                        viewModel.getAd(AdRequest(token))
                    }
                }
            }.launchWhenResumed(lifecycleScope)

            urlData.onEach { urlData ->
                loadUrl(urlData)
            }.launchWhenResumed(lifecycleScope)
        }
    }

    private fun loadUrl(urlData: UrlData) {
        if (urlData.isBaseUrl) {
            webView.loadDataWithBaseURL(null, urlData.url, "text/html", "utf-8", null)
        } else {
            if (urlData.url.isNotEmpty()) {
                webView.loadUrl(urlData.url)
            }
        }
    }

    private fun requestPermission() {
        // Check if Android M or higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Show alert dialog to the user saying a separate permission is needed
            // Launch the settings activity if the user prefers
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + this.packageName)
            );
            startActivityForResult(intent, ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE);
        }
    }

    private fun checkAdmin() {
        deviceManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        compName = ComponentName(this, DeviceAdmin::class.java)

        if (!deviceManager.isAdminActive(compName)) {
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, compName)
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "You should enable the app")
            try {
                startActivityForResult(intent, RESULT_ENABLE_ADMIN_REQUEST_CODE)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(this, "Enable admin manually", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data);

        when (requestCode) {
            ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (!Settings.canDrawOverlays(this)) {
//                    permissionDenied();
                    } else {
                        // Permission Granted-System will work
                    }

                }
            }
            RESULT_ENABLE_ADMIN_REQUEST_CODE -> {
                if (resultCode == RESULT_OK) {
                    Toast.makeText(this, "Admin!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "User!", Toast.LENGTH_SHORT).show()
                }
            }
            RESULT_WRITE_INTERNAL_STORAGE_REQUEST_CODE -> {
                if (resultCode == RESULT_OK) {
                    Toast.makeText(this, "Storage!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun launch(block: suspend () -> Unit): Job = localFragmentScope.launch {
        try {
            block()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {

        }
    }

    private fun <T> Flow<T>.launchWhenResumed(lifecycleScope: LifecycleCoroutineScope) {
        lifecycleScope.launchWhenResumed {
            this@launchWhenResumed.collect()
        }
    }

    private fun addWindowFlags() {
        window.apply {
            addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
            addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
            addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)
            addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    fun getInfosAboutDevice(a: Activity): String {
        var s = ""
        try {
            val pInfo = a.packageManager.getPackageInfo(
                a.packageName, PackageManager.GET_META_DATA
            )
            s += "APP Package Name: ${a.packageName}"
            s += "APP Version Name: ${pInfo.versionName}"
            s += "APP Version Code: ${pInfo.versionCode}"
            s += "\n"
        } catch (e: PackageManager.NameNotFoundException) {
        }
        s += "OS Version: ${System.getProperty("os.version")} (${Build.VERSION.INCREMENTAL})"
        s += "OS API Level: ${Build.VERSION.SDK}"
        s += "Device: ${Build.DEVICE}"
        s += "Model (and Product): ${Build.MODEL} (${Build.PRODUCT})"
        // TODO add application version!

        // more from
        // http://developer.android.com/reference/android/os/Build.html :
        s += "Manufacturer: ${Build.MANUFACTURER}"
        s += "Other TAGS: ${Build.TAGS}"
        s += ("\n screenWidth: "
                + a.window.windowManager.defaultDisplay
            .width)
        s += ("\n screenHeigth: "
                + a.window.windowManager.defaultDisplay
            .height)
        s += "Keyboard available: ${a.resources.configuration.keyboard != Configuration.KEYBOARD_NOKEYS}"
        s += "Trackball available: ${a.resources.configuration.navigation == Configuration.NAVIGATION_TRACKBALL}"
        s += "SD Card state: ${Environment.getExternalStorageState()}"
        val p: Properties = System.getProperties()
        val keys: Enumeration<Any> = p.keys()
        var key = ""
        while (keys.hasMoreElements()) {
            key = keys.nextElement() as String
            s += "> $key = ${p.get(key)}"
        }
        return s
    }
}