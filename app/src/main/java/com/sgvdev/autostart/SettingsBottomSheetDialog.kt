package com.sgvdev.autostart

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import android.widget.*
import androidx.annotation.ArrayRes
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.sgvdev.autostart.receivers.LockDeviceReceiver
import com.sgvdev.autostart.receivers.UnlockDeviceReceiver
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import java.lang.Exception
import java.util.*

private const val CONNECT_MONITOR_URL = "https://vlifte.by/_tv"
private const val VLIFTE = "vlifte"
private const val VLIFTE_TV = "_tv"
private const val GOOGLE_URL = "https://google.com"
private const val HTTPS = "https"

private const val LOCK_HOUR = 23
private const val LOCK_MINUTE = 0
private const val UNLOCK_HOUR = 8
private const val UNLOCK_MINUTE = 0
private const val MOCK_LOCK_HOUR = 12
private const val MOCK_LOCK_MINUTE = 10
private const val MOCK_UNLOCK_HOUR = 12
private const val MOCK_UNLOCK_MINUTE = 15

class SettingsBottomSheetDialog(
    context: Context,
    private var appSettings: AppSettings/*,
    private val scope: CoroutineScope*/
) : BottomSheetDialog(context) {

    private val _webViewUrl = MutableStateFlow(UrlData(false, ""))
    val webViewUrl: StateFlow<UrlData> = _webViewUrl.asStateFlow()

    private var adUrl: String = ""
    private fun createJob() {
        job = SupervisorJob()
        localFragmentScope = CoroutineScope(Dispatchers.IO + job) + loggingExceptionHandler
    }
    lateinit var job: Job
    private val loggingExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.d("CoroutineException", "$throwable ${throwable.printStackTrace()}")
    }
    lateinit var localFragmentScope: CoroutineScope

    private fun setWebViewUrl(data: UrlData) {
        _webViewUrl.value = UrlData(false, "")
        _webViewUrl.value = data
    }

    init {
        setContentView(R.layout.set_alarms_layout)
        createJob()
    }

    fun open() {

        val etLockHour: EditText? = findViewById(R.id.et_lock_hour)
        val etLockMinute: EditText? = findViewById(R.id.et_lock_minute)
        val btnSetAlarms: MaterialButton? = findViewById(R.id.set_alarms)

        val etUrl: EditText? = findViewById(R.id.et_url)
        val btnSetUrl: MaterialButton? = findViewById(R.id.btn_set_url)
        val btnClose: ImageView? = findViewById(R.id.iv_close)

        etUrl?.setText(adUrl)

        btnSetAlarms?.setOnClickListener {

            val lockHour = etLockHour?.text.toString().toInt()
            val lockMinute = if(etLockMinute?.text.toString().isEmpty()) 0 else etLockMinute?.text.toString().toInt()

            setLockAlarm(lockHour, lockMinute)
            Toast.makeText(context, "Lock alarm set!", Toast.LENGTH_SHORT).show()
        }

        btnSetUrl?.let {
            it.setOnClickListener {
                var newAdUrl = etUrl!!.text.toString()
                if (newAdUrl.isNotEmpty()) {
                    etUrl.error = null

                    if (!newAdUrl.contains(HTTPS)) {
                        newAdUrl = context.getString(R.string.https, newAdUrl)
                    }
                    launch {

                        if (newAdUrl.contains(VLIFTE_TV)) {
                            appSettings.saved.collect {
                                newAdUrl = "$CONNECT_MONITOR_URL/${it.deviceToken}"
                                adUrl = newAdUrl
                                appSettings.setAdUrl(newAdUrl)
                                setWebViewUrl(UrlData(false, newAdUrl))
                            }
                            return@launch
                        }
                        adUrl = newAdUrl
                        appSettings.setAdUrl(newAdUrl)
                        setWebViewUrl(UrlData(false, newAdUrl))
                    }
                    showToast(context.getString(R.string.url_changed))

                } else {
                    etUrl.error = context.getString(R.string.url_is_blank)
                }
            }
        }

        btnClose?.let {
            it.setOnClickListener {
                dismiss()
            }
        }
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        show()
    }

    private fun setLockAlarm(lockHour: Int = LOCK_HOUR, lockMinute: Int = LOCK_MINUTE) {
        val intent = Intent(context, LockDeviceReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context.applicationContext,
            LockDeviceReceiver.REQUEST_LOCK_CODE,
            intent,
            0
        )
        var alarmManager: AlarmManager =
            ContextCompat.getSystemService(context, AlarmManager::class.java) as AlarmManager
        alarmManager.cancel(pendingIntent)
        LogWriter.log(
            context,
            "MainActivity: setting lock alarm for $lockHour:$lockMinute"
        )
        Log.d(
            "MY_TAG",
            "MainActivity: setting lock alarm for $lockHour:$lockMinute"
        )

        val calendar: Calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, lockHour)
            set(Calendar.MINUTE, lockMinute)
        }
        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    fun getAdUrlFromAppSettings() {
        launch {
            appSettings.saved.collect { settings ->
                adUrl = when {
                    settings.adUrl.contains(VLIFTE) -> {
                        Log.d("SettingsDialog", "getAdUrlFromAppSettings: adUrl no token")
                        "$CONNECT_MONITOR_URL/${settings.deviceToken}"
                    }
                    settings.adUrl.isEmpty() -> {
                        Log.d("SettingsDialog", "getAdUrlFromAppSettings: adUrl.isEmpty()")
                        appSettings.setAdUrl(CONNECT_MONITOR_URL)
                        CONNECT_MONITOR_URL
                    }
                    else -> settings.adUrl
                }
                setWebViewUrl(UrlData(false, adUrl))
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    private fun launch(block: suspend () -> Unit): Job = localFragmentScope.launch(Dispatchers.Main) {
        try {
            block()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {

        }
    }
}