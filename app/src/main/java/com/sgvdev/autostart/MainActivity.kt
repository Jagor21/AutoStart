package com.sgvdev.autostart

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.admin.DevicePolicyManager
import android.content.*
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.util.Log
import android.view.*
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.sgvdev.autostart.receivers.*
import java.io.*
import java.util.*

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

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    private lateinit var deviceManager: DevicePolicyManager
    private lateinit var compName: ComponentName

    private lateinit var btnClearLog: View
    private lateinit var btnSendLogs: View
    private lateinit var btnSetAlarmsTime: View

    private var clearLogClickCount = 5
    private var sendLogCount = 5

    private lateinit var shutDownReceiver: ShutDownReceiver
    private lateinit var shutDownReceiverIntentFilter: IntentFilter

    companion object {
        const val APP_TAG = "AUTOSTART:"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MY_TAG", "activity onStart")
        window.apply {
//            setFlags(
//                WindowManager.LayoutParams.FLAG_FULLSCREEN,
//                WindowManager.LayoutParams.FLAG_FULLSCREEN,
//            )
            addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
            addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
            addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)
            addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        initShutDownReceiver()
        registerReceiver(shutDownReceiver, shutDownReceiverIntentFilter)

        setContentView(R.layout.activity_my)
        checkAdmin()
        bindViews()

        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
            RESULT_WRITE_INTERNAL_STORAGE_REQUEST_CODE
        )


//if the user already granted the permission or the API is below Android 10 no need to ask for permission

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            !Settings.canDrawOverlays(this)
        ) {
            requestPermission()
        }

//        startAlarms()

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

    private fun bindViews() {
        webView = findViewById(R.id.webView)
        webView.apply {
            webViewClient = WebViewClient()
            loadUrl("https://vlifte.by/_tv")
            settings.javaScriptEnabled = true
        }

        btnClearLog = findViewById(R.id.btn_clear_logs)
        btnSendLogs = findViewById(R.id.btn_send_logs)

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

        btnSetAlarmsTime = findViewById(R.id.btn_set_alarms_time)
        btnSetAlarmsTime.setOnClickListener {
            showSetAlarmsBottomSheet()
        }
    }

    private fun showSetAlarmsBottomSheet() {
        val bottomSheet = BottomSheetDialog(this)
        bottomSheet.setContentView(R.layout.set_alarms_layout)

        val etLockHour: EditText? = bottomSheet.findViewById(R.id.et_lock_hour)
        val etLockMinute: EditText? = bottomSheet.findViewById(R.id.et_lock_minute)
        val etUnlockHour: EditText? = bottomSheet.findViewById(R.id.et_unlock_hour)
        val etUnlockMinute: EditText? = bottomSheet.findViewById(R.id.et_unlock_minute)
        val btnSetAlarms: MaterialButton? = bottomSheet.findViewById(R.id.set_alarms)

        val etUrl: EditText? = bottomSheet.findViewById(R.id.et_url)
        val btnSetUrl: MaterialButton? = bottomSheet.findViewById(R.id.btn_set_url)
        val btnClose: ImageView? = bottomSheet.findViewById(R.id.iv_close)


        btnSetAlarms?.setOnClickListener {

            val lockHour = etLockHour?.text.toString().toInt()
            val lockMinute = etLockMinute?.text.toString().toInt()
            val unlockHour = etUnlockHour?.text.toString().toInt()
            val unlockMinute = etUnlockMinute?.text.toString().toInt()

            setLockAlarm(lockHour, lockMinute)
            setUnlockAlarm(unlockHour, unlockMinute)
            Toast.makeText(bottomSheet.context, "Lock alarm set!", Toast.LENGTH_SHORT).show()
        }

        btnSetUrl?.setOnClickListener {
            val url = etUrl?.text.toString()

            if (url.isNotEmpty()) {
                webView.loadUrl(url)
                Toast.makeText(bottomSheet.context, "URL changed to $url", Toast.LENGTH_SHORT)
                    .show()
            } else {
                Toast.makeText(bottomSheet.context, "URL is empty!\nPlease set URL.", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        btnClose?.setOnClickListener {
            bottomSheet.dismissWithAnimation = true
            bottomSheet.dismiss()
        }
        bottomSheet.show()
    }

    private fun startAlarms() {
        setLockAlarm(MOCK_LOCK_HOUR, MOCK_LOCK_MINUTE)
        setUnlockAlarm(MOCK_UNLOCK_HOUR, MOCK_UNLOCK_MINUTE)
    }

    private fun setLockAlarm(lockHour: Int = LOCK_HOUR, lockMinute: Int = LOCK_MINUTE) {
        val intent = Intent(this, LockDeviceReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this.applicationContext,
            LockDeviceReceiver.REQUEST_LOCK_CODE,
            intent,
            0
        )
        var alarmManager: AlarmManager =
            ContextCompat.getSystemService(this, AlarmManager::class.java) as AlarmManager
        alarmManager.cancel(pendingIntent)
        LogWriter.log(
            this,
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

    private fun setUnlockAlarm(unlockHour: Int = UNLOCK_HOUR, unlockMinute: Int = UNLOCK_MINUTE) {
        val intent = Intent(this, UnlockDeviceReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this.applicationContext,
            UnlockDeviceReceiver.REQUEST_UNLOCK_CODE,
            intent,
            0
        )
        var alarmManager: AlarmManager = ContextCompat.getSystemService(
            this,
            AlarmManager::class.java
        ) as AlarmManager

        alarmManager.cancel(pendingIntent)

        LogWriter.log(
            this,
            "MainActivity: setting unlock alarm for $unlockHour:$unlockMinute"
        )
        Log.d(
            "MY_TAG",
            "MainActivity: setting unlock alarm for $unlockHour:$unlockMinute"
        )

        val calendar: Calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, unlockHour)
            set(Calendar.MINUTE, unlockMinute)
        }
        alarmManager?.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
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
            startActivityForResult(intent, RESULT_ENABLE_ADMIN_REQUEST_CODE)
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
                    Toast.makeText(this, "Huila!", Toast.LENGTH_SHORT).show()
                }
            }
            RESULT_WRITE_INTERNAL_STORAGE_REQUEST_CODE -> {
                if (resultCode == RESULT_OK) {
                    Toast.makeText(this, "Storage!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        LogWriter.log(this, "MainActivity: onStop()")
    }

    override fun onDestroy() {
        unregisterReceiver(
            shutDownReceiver
        )
        LogWriter.log(this, "MainActivity: onDestroy()")
        super.onDestroy()
    }
}