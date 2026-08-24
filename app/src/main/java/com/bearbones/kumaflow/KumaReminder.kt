/*

@file:Suppress("SpellCheckingInspection", "UNUSED_PARAMETER", "unused", "CanBeVal", "DEPRECATION", "ScheduleExactAlarm")

package com.bearbones.kumaflow

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.Calendar
import android.annotation.SuppressLint

class KumaReminder : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Maximize device wake-up mechanism to ensure alarm execution
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "KumaFlow:AlarmWakeLock")
        wakeLock.acquire(10000L) // Force device wake lock for 10 seconds to complete the operation

        try {
            Toast.makeText(context, "Alarm Triggered!", Toast.LENGTH_SHORT).show()
            showNotification(context)

            CoroutineScope(Dispatchers.IO).launch {
                val db = KumaDatabase.getDatabase(context)
                val profile = db.transactionDao().getUserProfile().firstOrNull()

                if (profile != null && profile.isReminderOn) {
                    scheduleKumaReminders(context, profile)
                }
            }
        } finally {
            if (wakeLock.isHeld) wakeLock.release()
        }
    }

    private fun showNotification(context: Context) {
        val channelId = "kumaflow_reminder_channel_v9"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val soundUri = Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/${R.raw.kumaflownotification}")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()

            val channel = NotificationChannel(
                channelId,
                "KumaFlow Reminder",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = if(com.bearbones.kumaflow.AppStr.isId) "Pengingat untuk mencatat pengeluaran" else "Expense tracking reminder"
                setSound(soundUri, audioAttributes)
                enableVibration(true)
                vibrationPattern = longArrayOf(1000, 1000, 1000, 1000, 1000)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val messages = if (com.bearbones.kumaflow.AppStr.isId) {
            listOf(
                Pair("Satu bulan saldonya ke mana? \uD83E\uDD40", "Duit abis berlebihan itu nggak baik. Yuk catat pengeluaran hari ini."),
                Pair("Hari ini jajan apa aja? \uD83E\uDD14", "Sekecil apapun itu, dicatat ya! Biar nggak kaget di akhir bulan."),
                Pair("Lantas mengapa ku masih jajan? \uD83C\uDFA7", "Mending evaluasi dulu pengeluaran kamu hari ini deh."),
                Pair("Berapa harga kewarasan ini? \uD83E\uDD7A", "Catat pengeluaran kamu sekarang yuk ah!"),
                Pair("Jangan lupa diri bestie \uD83D\uDE4F", "Ayo cek dompet kamu, masih sehat atau udah nangis?"),
                Pair("Semoga saldo aman ya \uD83E\uDEF0", "Udah nyatat pengeluaran hari ini? Yuk buka aplikasinya."),
                Pair("Pengeluaran misterius? \uD83D\uDC7B", "Jangan biarkan duit menguap gitu aja, catat sekarang!"),
                Pair("Soal hemat ternyata aku masih amatir \uD83D\uDCB8", "Saatnya evaluasi pengeluaran hari ini.")
            )
        } else {
            listOf(
                Pair("Where did this month's balance go? \uD83E\uDD40", "Overspending is bad. Let's record today's expenses."),
                Pair("What did you buy today? \uD83E\uDD14", "Record every little thing! So you won't be surprised later."),
                Pair("Why am I still spending? \uD83C\uDFA7", "Better evaluate your expenses today."),
                Pair("How much does sanity cost? \uD83E\uDD7A", "Let's record your expenses now!"),
                Pair("Don't forget yourself bestie \uD83D\uDE4F", "Check your wallet, is it healthy or crying?"),
                Pair("Hope the balance is safe \uD83E\uDEF0", "Have you recorded today's expenses? Open the app."),
                Pair("Mysterious expenses? \uD83D\uDC7B", "Don't let money evaporate, record it now!"),
                Pair("Still an amateur at saving \uD83D\uDCB8", "Time to evaluate today's expenses.")
            )
        }

        val randomMsg = messages.random()

        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val largeIconBitmap = android.graphics.BitmapFactory.decodeResource(
            context.resources,
            R.drawable.ic_kumaflow_logo
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification_small)
            .setLargeIcon(largeIconBitmap)
            .setColor(androidx.core.content.ContextCompat.getColor(context, R.color.kumaflow_notification_accent))
            .setContentTitle(randomMsg.first)
            .setContentText(randomMsg.second)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            // setDefaults removed to prevent overriding custom sound
            .setSound(soundUri)
            .setVibrate(longArrayOf(1000, 1000, 1000, 1000, 1000))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }
}

class KumaBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == "android.intent.action.QUICKBOOT_POWERON") {
            CoroutineScope(Dispatchers.IO).launch {
                val db = KumaDatabase.getDatabase(context)
                val profile = db.transactionDao().getUserProfile().firstOrNull()

                if (profile != null && profile.isReminderOn) {
                    scheduleKumaReminders(context, profile)
                }
            }
        }
    }
}


@SuppressLint("ScheduleExactAlarm")
/*
fun scheduleKumaReminders(context: Context, profile: UserProfile) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (!alarmManager.canScheduleExactAlarms()) {
            Toast.makeText(context, "Ijinkan KumaFlow buat Alarm Akurat dulu di Settings!", Toast.LENGTH_LONG).show()
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try {
                context.startActivity(intent)
            } catch (_: Exception) {
            }
            return
        }
    }

    val intent = Intent(context, KumaReminder::class.java)

    // Clear previously set alarms to prevent duplication and overlap
    for (i in 0..4) {
        val pendingIntent = PendingIntent.getBroadcast(context, i, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        alarmManager.cancel(pendingIntent)
    }

    if (!profile.isReminderOn) return

    val times = profile.reminderTimes.split(",")
    times.forEachIndexed { index, timeStr ->
        val parts = timeStr.split(":")
        if (parts.size == 2) {
            val hour = parts[0].toIntOrNull() ?: 0
            val min = parts[1].toIntOrNull() ?: 0

            val calendar = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, min)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            if (calendar.timeInMillis <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }

            val pendingIntent = PendingIntent.getBroadcast(context, index, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

            try {
                // Use setAlarmClock to bypass aggressive battery optimization (e.g., ColorOS)
                val alarmClockInfo = AlarmManager.AlarmClockInfo(calendar.timeInMillis, pendingIntent)
                alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
*/