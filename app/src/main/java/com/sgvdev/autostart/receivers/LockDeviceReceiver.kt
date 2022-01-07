package com.sgvdev.autostart.receivers

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.admin.DevicePolicyManager
import android.content.*
import android.util.Log
import androidx.core.content.ContextCompat
import com.sgvdev.autostart.DeviceAdmin
import com.sgvdev.autostart.LogWriter
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
    }

    override fun onReceive(context: Context?, intent: Intent?) {
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
            deviceManager.lockNow()
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