package com.bearbones.kumaflow

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

class KumaWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { widgetId ->
            updateWidget(context, appWidgetManager, widgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == "com.bearbones.kumaflow.UPDATE_WIDGET") {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val ids = appWidgetManager.getAppWidgetIds(ComponentName(context, KumaWidgetProvider::class.java))
            ids.forEach { id -> updateWidget(context, appWidgetManager, id) }
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        updateWidget(context, appWidgetManager, appWidgetId)
    }

    private fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.widget_kumaflow)

        val options = appWidgetManager.getAppWidgetOptions(widgetId)
        val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
        val isExpanded = minHeight >= 180

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
        views.setOnClickPendingIntent(R.id.tv_widget_balance, pendingIntent)

        views.setViewVisibility(R.id.layout_recent_transactions, if (isExpanded) View.VISIBLE else View.GONE)

        views.setTextViewText(R.id.tv_widget_balance, "...")
        views.setTextViewText(R.id.tv_widget_income, "...")
        views.setTextViewText(R.id.tv_widget_expense, "...")
        views.setTextViewText(R.id.tv_w1_name, "")
        views.setTextViewText(R.id.tv_w1_bal, "")
        views.setTextViewText(R.id.tv_w2_name, "")
        views.setTextViewText(R.id.tv_w2_bal, "")
        views.setTextViewText(R.id.tv_w3_name, "")
        views.setTextViewText(R.id.tv_w3_bal, "")
        appWidgetManager.updateAppWidget(widgetId, views)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = KumaDatabase.getDatabase(context)
                val profile = db.transactionDao().getUserProfile().firstOrNull()
                if (profile == null) {
                    views.setTextViewText(R.id.tv_widget_balance, "Rp 0")
                    views.setTextViewText(R.id.tv_widget_income, "Rp 0")
                    views.setTextViewText(R.id.tv_widget_expense, "Rp 0")
                    appWidgetManager.updateAppWidget(widgetId, views)
                    return@launch
                }

                val transactionsWithSplits = db.transactionDao().getAllTransactionsWithSplits().firstOrNull() ?: emptyList()

                val sharedPref = context.getSharedPreferences("kumaflow_prefs", Context.MODE_PRIVATE)
                val isPrivacyMode = sharedPref.getBoolean("privacy_mode", false)

                val defaultLang = Locale.getDefault().language
                val isId = defaultLang == "in" || defaultLang == "id"
                val locale = if (isId) Locale("id", "ID") else Locale.getDefault()
                val curSym = when(profile.currency) { "USD", "AUD", "CAD", "SGD" -> "$"; "EUR" -> "€"; "GBP" -> "£"; "JPY", "CNY" -> "¥"; "CHF" -> "CHF"; "MYR" -> "RM"; "THB" -> "฿"; "PHP" -> "₱"; "VND" -> "₫"; else -> "Rp" }

                val currentMonth = LocalDateTime.now().monthValue
                val currentYear = LocalDateTime.now().year

                var income = 0L
                var expenses = 0L
                val walletBalances = mutableMapOf<String, Long>()

                profile.wallets.split(",").filter { it.isNotBlank() }.forEach { walletBalances[it] = 0L }

                transactionsWithSplits.forEach { txObj ->
                    try {
                        val t = txObj.transaction
                        val dt = LocalDateTime.parse(t.timestamp, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                        val amt = t.amount.toLongOrNull() ?: 0L

                        if (txObj.splits.isNotEmpty()) {
                            txObj.splits.forEach { split ->
                                val current = walletBalances[split.splitWallet] ?: 0L
                                walletBalances[split.splitWallet] = current + (if (t.isIncome) split.splitAmount else -split.splitAmount)
                            }
                        } else {
                            val current = walletBalances[t.wallet] ?: 0L
                            walletBalances[t.wallet] = current + (if(t.isIncome) amt else -amt)
                        }

                        if (dt.monthValue == currentMonth && dt.year == currentYear) {
                            if (t.category != "Transfer") {
                                if (t.isIncome) income += amt else expenses += amt
                            }
                        }
                    } catch (_: Exception) {}
                }

                val savingsWallets = profile.savingsWallets.split(",").filter { it.isNotBlank() }.toSet()
                val totalBal = walletBalances.filterKeys { it !in savingsWallets }.values.sum()
                val top3Wallets = walletBalances.filterKeys { it !in savingsWallets }.entries.toList().take(3)

                fun formatWidget(value: Long, useAbs: Boolean = false): String {
                    val v = if (useAbs) abs(value) else value
                    val formatted = NumberFormat.getInstance(locale).format(v)
                    return if (isPrivacyMode) formatted.replace(Regex("\\d"), "*") else formatted
                }

                fun formatSmartAbbr(value: Long, useAbs: Boolean = false): String {
                    val v = if (useAbs) abs(value) else value
                    val sign = if (!useAbs && value < 0) "- " else ""
                    val absV = abs(v)

                    val (numStr, unit) = when {
                        absV >= 1_000_000_000_000L -> {
                            val d = absV.toDouble() / 1_000_000_000_000.0
                            val formatted = if (d % 1.0 == 0.0) "%.0f".format(locale, d)
                                            else "%.2f".format(locale, d).trimEnd('0').trimEnd(',').trimEnd('.')
                            formatted to " T"
                        }
                        absV >= 1_000_000_000L -> {
                            val d = absV.toDouble() / 1_000_000_000.0
                            val formatted = if (d % 1.0 == 0.0) "%.0f".format(locale, d)
                                            else "%.2f".format(locale, d).trimEnd('0').trimEnd(',').trimEnd('.')
                            formatted to (if (isId) " M" else " B")
                        }
                        absV >= 1_000_000L -> {
                            val d = absV.toDouble() / 1_000_000.0
                            val formatted = if (d % 1.0 == 0.0) "%.0f".format(locale, d)
                                            else if (d >= 100) "%.1f".format(locale, d).trimEnd('0').trimEnd(',').trimEnd('.')
                                            else "%.2f".format(locale, d).trimEnd('0').trimEnd(',').trimEnd('.')
                            formatted to (if (isId) " Jt" else " M")
                        }
                        absV >= 100_000L -> {
                            val d = absV.toDouble() / 1_000.0
                            val formatted = if (d % 1.0 == 0.0) "%.0f".format(locale, d)
                                            else "%.1f".format(locale, d).trimEnd('0').trimEnd(',').trimEnd('.')
                            formatted to (if (isId) " rb" else " k")
                        }
                        else -> {
                            NumberFormat.getInstance(locale).format(absV) to ""
                        }
                    }

                    val result = "$sign$numStr$unit"
                    return if (isPrivacyMode) result.replace(Regex("\\d"), "*") else result
                }

                views.setTextViewText(R.id.tv_widget_total_balance_title, if (isId) "Total Saldo" else "Total Balance")
                val balPref = if (totalBal < 0) "- " else ""
                val totalBalFormatted = "$balPref$curSym ${formatWidget(totalBal, true)}"
                views.setTextViewText(R.id.tv_widget_balance, totalBalFormatted)

                if (totalBalFormatted.length > 18) {
                    views.setTextViewTextSize(R.id.tv_widget_balance, TypedValue.COMPLEX_UNIT_SP, 20f)
                } else if (totalBalFormatted.length > 14) {
                    views.setTextViewTextSize(R.id.tv_widget_balance, TypedValue.COMPLEX_UNIT_SP, 24f)
                } else {
                    views.setTextViewTextSize(R.id.tv_widget_balance, TypedValue.COMPLEX_UNIT_SP, 28f)
                }

                val formattedInc = if (abs(income) >= 1_000_000_000L) formatSmartAbbr(income) else formatWidget(income)
                val formattedExp = if (abs(expenses) >= 1_000_000_000L) formatSmartAbbr(expenses) else formatWidget(expenses)
                views.setTextViewText(R.id.tv_widget_income, "$curSym $formattedInc")
                views.setTextViewText(R.id.tv_widget_expense, "$curSym $formattedExp")

                if (top3Wallets.isNotEmpty()) {
                    views.setTextViewText(R.id.tv_w1_name, top3Wallets[0].key)
                    views.setTextViewText(R.id.tv_w1_bal, "$curSym ${formatSmartAbbr(top3Wallets[0].value, true)}")
                }
                if (top3Wallets.size > 1) {
                    views.setTextViewText(R.id.tv_w2_name, top3Wallets[1].key)
                    views.setTextViewText(R.id.tv_w2_bal, "$curSym ${formatSmartAbbr(top3Wallets[1].value, true)}")
                }
                if (top3Wallets.size > 2) {
                    views.setTextViewText(R.id.tv_w3_name, top3Wallets[2].key)
                    views.setTextViewText(R.id.tv_w3_bal, "$curSym ${formatSmartAbbr(top3Wallets[2].value, true)}")
                }

                if (isExpanded) {
                    views.setViewVisibility(R.id.layout_recent_transactions, View.VISIBLE)
                    views.setTextViewText(R.id.tv_widget_recent_title, if (isId) "Transaksi Terakhir" else "Recent Transactions")
                    val recentTransactions = transactionsWithSplits
                        .filter { !it.transaction.isDeleted }
                        .sortedByDescending { it.transaction.timestamp }
                        .take(4)

                    if (recentTransactions.isEmpty()) {
                        views.setTextViewText(R.id.tv_widget_no_tx, if (isId) "Belum ada transaksi" else "No recent transactions")
                        views.setViewVisibility(R.id.tv_widget_no_tx, View.VISIBLE)
                        views.setViewVisibility(R.id.widget_tx_item_1, View.GONE)
                        views.setViewVisibility(R.id.widget_tx_item_2, View.GONE)
                        views.setViewVisibility(R.id.widget_tx_item_3, View.GONE)
                        views.setViewVisibility(R.id.widget_tx_item_4, View.GONE)
                    } else {
                        views.setViewVisibility(R.id.tv_widget_no_tx, View.GONE)
                        val itemConfigs = listOf(
                            Triple(R.id.widget_tx_item_1, R.id.tv_tx1_desc, R.id.tv_tx1_amount),
                            Triple(R.id.widget_tx_item_2, R.id.tv_tx2_desc, R.id.tv_tx2_amount),
                            Triple(R.id.widget_tx_item_3, R.id.tv_tx3_desc, R.id.tv_tx3_amount),
                            Triple(R.id.widget_tx_item_4, R.id.tv_tx4_desc, R.id.tv_tx4_amount)
                        )

                        for (i in itemConfigs.indices) {
                            val (containerId, descId, amtId) = itemConfigs[i]
                            if (i < recentTransactions.size) {
                                val txObj = recentTransactions[i]
                                val t = txObj.transaction
                                val amtVal = t.amount.toLongOrNull() ?: 0L
                                val desc = if (t.name.isNotBlank() && t.category.isNotBlank() && t.category != t.name) {
                                    "${t.name} · ${t.category}"
                                } else if (t.name.isNotBlank()) {
                                    t.name
                                } else {
                                    t.category
                                }

                                val sign = if (t.isIncome) "+ " else "- "
                                val formattedAmt = "$sign$curSym ${formatSmartAbbr(amtVal, true)}"
                                val color = if (t.isIncome) android.graphics.Color.parseColor("#00E676") else android.graphics.Color.parseColor("#FF1744")

                                views.setTextViewText(descId, desc)
                                views.setTextViewText(amtId, formattedAmt)
                                views.setTextColor(amtId, color)
                                views.setViewVisibility(containerId, View.VISIBLE)
                            } else {
                                views.setViewVisibility(containerId, View.GONE)
                            }
                        }
                    }
                } else {
                    views.setViewVisibility(R.id.layout_recent_transactions, View.GONE)
                }

                appWidgetManager.updateAppWidget(widgetId, views)
            } catch (e: Exception) {
                e.printStackTrace()
                appWidgetManager.updateAppWidget(widgetId, views)
            }
        }
    }
}