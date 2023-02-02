package com.sgvdev.autostart.receivers

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.admin.DevicePolicyManager
import android.content.*
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.sgvdev.autostart.DeviceAdmin
import com.sgvdev.autostart.LogWriter
import com.sgvdev.autostart.ui.activity.MainActivity
import com.sgvdev.autostart.utils.createHMSDateString
import java.util.*

private const val TIME_LOCK_23_00 = 23

private const val WAKE_LOCK_9_HOURS_INTERVAL_MILLIS = 32400000
private const val WAKE_LOCK_1_HOUR_INTERVAL_MILLIS = 3600000

class LockDeviceReceiver : BroadcastReceiver() {

    private lateinit var deviceManager: DevicePolicyManager
    private lateinit var compName: ComponentName

    companion object {
        const val REQUEST_LOCK_CODE = 12345
        const val ACTION_CLOSE = "close_action"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        val manufacturer = Build.MANUFACTURER
        Toast.makeText(context, manufacturer, Toast.LENGTH_LONG).show()
        context?.let {
            deviceManager =
                ContextCompat.getSystemService(
                    context,
                    DevicePolicyManager::class.java
                ) as DevicePolicyManager
            compName = ComponentName(it, DeviceAdmin::class.java)
        }
        if (deviceManager.isAdminActive(compName)) {
            context?.let {
                LogWriter.log(
                    it,
                    "LockReceiver: need to go bed. Good night!\nScreen lock"
                )
                Log.d(
                    "MY_TAG",
                    "LockReceiver: need to go bed. Good night!\nScreen lock"
                )
            }


            when (manufacturer.lowercase()) {
                "xiaomi" -> {
                    Toast.makeText(context, "XIAOMI", Toast.LENGTH_LONG).show()
                    val intent = Intent(context, MainActivity::class.java)
                    intent.apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        putExtra(ACTION_CLOSE, true)
                    }
                    context?.applicationContext?.startActivity(intent)
                    Settings.System.putInt(
                        context?.contentResolver,
                        Settings.System.SCREEN_OFF_TIMEOUT, (10000)
                    )
                }
                else -> {
                    val intent = Intent(context, MainActivity::class.java)
                    intent.apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        putExtra(ACTION_CLOSE, true)
                    }
                    context?.applicationContext?.startActivity(intent)

//                    deviceManager.lockNow()
                }
            }
//            setAlarm(context)
        }
    }


    private fun setAlarm(context: Context?) {
        val intent = Intent(context, UnlockDeviceReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context?.applicationContext,
            UnlockDeviceReceiver.REQUEST_UNLOCK_CODE,
            intent,
            0
        )
        var alarmManager: AlarmManager? = null
        context?.let {
            alarmManager = ContextCompat.getSystemService(
                it,
                AlarmManager::class.java
            ) as AlarmManager
        }
//        alarmManager.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + WAKE_LOCK_INTERVAL_MILLIS, pendingIntent)
        context?.let {
            LogWriter.log(
                it,
                "LockReceiver: setting alarm for ${createHMSDateString(Date(System.currentTimeMillis() + WAKE_LOCK_1_HOUR_INTERVAL_MILLIS))}"
            )
        }
        val calendar: Calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 17)
            set(Calendar.MINUTE, 0)
        }
        alarmManager?.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }
}