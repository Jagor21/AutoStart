package com.sgvdev.autostart.receivers

import android.app.admin.DevicePolicyManager
import android.content.*
import androidx.core.content.ContextCompat
import com.sgvdev.autostart.DeviceAdmin
import com.sgvdev.autostart.LogWriter
import kotlin.properties.Delegates

private const val TIME_LOCK_23_00 = 23

private const val WAKE_LOCK_9_HOURS_INTERVAL_MILLIS = 32400000
private const val WAKE_LOCK_1_HOUR_INTERVAL_MILLIS = 3600000

class LockReceiver() : BroadcastReceiver() {

    private lateinit var deviceManager: DevicePolicyManager
    private lateinit var compName: ComponentName

//    var hour: Int by Delegates.vetoable(0) { _, old, new ->
//        new > old
//    }
//
//    var minute: Int by Delegates.vetoable(0) { _, old, new ->
//        new > old
//    }

    override fun onReceive(context: Context?, intent: Intent?) {
//        context?.let {
//            deviceManager =
//                ContextCompat.getSystemService(
//                    context,
//                    DevicePolicyManager::class.java
//                ) as DevicePolicyManager
//            compName = ComponentName(it, DeviceAdmin::class.java)
//        }
//        if (deviceManager.isAdminActive(compName)) {
//            context?.let {
//                LogWriter.log(
//                    it,
//                    "LockReceiver: need to go bed. Good night!\nScreen lock"
//                )
//            }
//            deviceManager.lockNow()
//        }
    }

//    override fun onReceive(context: Context?, intent: Intent?) {
//        if (intent?.action == Intent.ACTION_TIME_TICK) {
//            val calendar = Calendar.getInstance()
//            if (calendar.get(Calendar.HOUR_OF_DAY) == 16) {
////            if (minute < calendar.get(Calendar.MINUTE)) {
//                context?.let {
//                    deviceManager =
//                        ContextCompat.getSystemService(
//                            context,
//                            DevicePolicyManager::class.java
//                        ) as DevicePolicyManager
//                    compName = ComponentName(it, DeviceAdmin::class.java)
//                }
//                if (deviceManager.isAdminActive(compName)) {
//                    context?.let {
//                        LogWriter.log(
//                            it,
//                            "LockReceiver: need to go bed. Good night!\nScreen lock"
//                        )
//                    }
//                    deviceManager.lockNow()
//                    setAlarm(context)
//                }
//                minute = calendar.get(Calendar.MINUTE)
//                hour = calendar.get(Calendar.HOUR_OF_DAY)
//            }
//        }
//    }
//
//    private fun setAlarm(context: Context?) {
//        val intent = Intent(context, UnlockDeviceReceiver::class.java)
//        val pendingIntent = PendingIntent.getBroadcast(
//            context?.applicationContext,
//            UnlockDeviceReceiver.REQUEST_CODE,
//            intent,
//            0
//        )
//        var alarmManager: AlarmManager? = null
//        context?.let {
//            alarmManager = getSystemService(it, AlarmManager::class.java) as AlarmManager
//        }
////        alarmManager.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + WAKE_LOCK_INTERVAL_MILLIS, pendingIntent)
//        context?.let {
//            LogWriter.log(
//                it,
//                "LockReceiver: setting alarm for ${createHMSDateString(Date(System.currentTimeMillis() + WAKE_LOCK_1_HOUR_INTERVAL_MILLIS))}"
//            )
//        }
//        alarmManager?.setExact(
//            AlarmManager.RTC_WAKEUP,
//            System.currentTimeMillis() + WAKE_LOCK_1_HOUR_INTERVAL_MILLIS,
//            pendingIntent
//        )
//    }
}