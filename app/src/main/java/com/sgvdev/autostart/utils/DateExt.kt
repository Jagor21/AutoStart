package com.sgvdev.autostart.utils

import java.text.SimpleDateFormat
import java.util.*

private const val LOG_DATE_FORMAT = "yyyy.MM.dd HH:mm:ssss"
private const val DATE_FORMAT_HMS = "HH:mm:ss"

fun createLogFileName(): String {
//    return createDateString().plus("_logfile.txt")
    return System.currentTimeMillis().toString().plus("_logfile.txt")
}

fun createLogDateString(): String {
    val sdf = SimpleDateFormat(LOG_DATE_FORMAT, Locale.getDefault())
    val date = Date(System.currentTimeMillis())
    return sdf.format(date)
}

fun createHMSDateString(date: Date): String {
    val sdf = SimpleDateFormat(DATE_FORMAT_HMS, Locale.getDefault())
    return sdf.format(date)
}