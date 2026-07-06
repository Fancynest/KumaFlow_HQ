package com.bearbones.kumaflow

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class KumaStreakWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                for (appWidgetId in appWidgetIds) {
                    updateAppWidget(context, appWidgetManager, appWidgetId)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult?.finish()
            }
        }
    }

    private suspend fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.widget_streak)

        // Launch app on click
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        views.setOnClickPendingIntent(R.id.widget_streak_root, pendingIntent)
        
        val messagesId = listOf(
            "Tetap semangat!",
            "Catat terus!",
            "Yuk, catat!",
            "Jangan lupa catat!",
            "Kuma menunggumu!",
            "Jaga apinya!",
            "Hemat pangkal kaya.",
            "Atur uangmu!",
            "Terus beruntun!",
            "Konsisten keren!"
        )
        val messagesEn = listOf(
            "Keep it up!",
            "Keep tracking!",
            "Let's track!",
            "Don't forget!",
            "Kuma is waiting!",
            "Keep the fire!",
            "Save more today.",
            "Manage your cash!",
            "Keep the streak!",
            "Consistency is key!"
        )
        val dayOfYear = java.time.LocalDate.now().dayOfYear
        val message = if (AppStr.isId) messagesId[dayOfYear % 10] else messagesEn[dayOfYear % 10]
        views.setTextViewText(R.id.widget_streak_message, message)
        
        val db = KumaDatabase.getDatabase(context)
        val profile = db.transactionDao().getProfileSync()
        if (profile != null) {
            views.setTextViewText(R.id.widget_streak_count, "🔥 ${profile.currentStreak}")
            
            val todayStr = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
            val isActiveToday = profile.lastActiveDate == todayStr
            
            val iconRes = if (isActiveToday) R.drawable.ic_fire_active else R.drawable.ic_fire_inactive
            val drawable = androidx.core.content.ContextCompat.getDrawable(context, iconRes)
            if (drawable != null) {
                val bitmap = android.graphics.Bitmap.createBitmap(
                    drawable.intrinsicWidth.takeIf { it > 0 } ?: 144,
                    drawable.intrinsicHeight.takeIf { it > 0 } ?: 144,
                    android.graphics.Bitmap.Config.ARGB_8888
                )
                val canvas = android.graphics.Canvas(bitmap)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
                views.setImageViewBitmap(R.id.widget_streak_icon, bitmap)
            } else {
                views.setImageViewResource(R.id.widget_streak_icon, iconRes)
            }
        }
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
