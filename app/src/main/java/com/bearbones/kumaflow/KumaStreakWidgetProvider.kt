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
        }
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
