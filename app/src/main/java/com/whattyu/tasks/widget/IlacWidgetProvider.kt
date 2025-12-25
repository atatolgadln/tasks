package com.whattyu.tasks.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import android.widget.Toast
import com.whattyu.tasks.MainActivity
import com.whattyu.tasks.R
import com.whattyu.tasks.data.IlacDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class IlacWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_ILAC_GUNCELLE = "com.whattyu.tasks.ACTION_GUNCELLE"

        fun widgetlariGuncelle(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, IlacWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_list_view)

            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (intent.action == ACTION_ILAC_GUNCELLE) {
            val ilacId = intent.getIntExtra("ILAC_ID", -1)
            val mevcutDurum = intent.getBooleanExtra("MEVCUT_DURUM", false)

            if (ilacId != -1) {
                val goAsync = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    val dao = IlacDatabase.getDatabase(context).ilacDao()
                    val ilac = dao.getIlacById(ilacId)

                    if (ilac != null) {
                        ilac.seciliMi = !mevcutDurum
                        val bugun = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                        if (ilac.seciliMi) {
                            // Checked: Decrease dose (if not infinite) and UPDATE DATE
                            if (ilac.kalanDoz != -1) ilac.kalanDoz -= 1
                            ilac.sonIslemTarihi = bugun // FIX: Critical for recurrence logic
                        } else {
                            // Unchecked: Increase dose (if not infinite)
                            if (ilac.kalanDoz != -1) ilac.kalanDoz += 1
                        }

                        if (ilac.kalanDoz != -1 && ilac.kalanDoz <= 0) {
                            dao.ilacSil(ilac)
                        } else {
                            dao.ilacGuncelle(ilac)
                        }

                        val appWidgetManager = AppWidgetManager.getInstance(context)
                        val componentName = ComponentName(context, IlacWidgetProvider::class.java)
                        val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_list_view)
                    }
                    goAsync.finish()
                }
            }
        }
    }
}

internal fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
    val intent = Intent(context, IlacWidgetService::class.java).apply {
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
    }
    val views = RemoteViews(context.packageName, R.layout.widget_layout)
    views.setRemoteAdapter(R.id.widget_list_view, intent)

    val clickIntent = Intent(context, IlacWidgetProvider::class.java).apply {
        action = IlacWidgetProvider.ACTION_ILAC_GUNCELLE
    }
    val clickPendingIntent = PendingIntent.getBroadcast(
        context, 0, clickIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
    )
    val appIntent = Intent(context, MainActivity::class.java)

    val pendingIntent = PendingIntent.getActivity(
        context,
        0,
        appIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    views.setPendingIntentTemplate(R.id.widget_list_view, clickPendingIntent)
    views.setEmptyView(R.id.widget_list_view, R.id.empty_view)
    views.setOnClickPendingIntent(R.id.empty_view, pendingIntent)

    appWidgetManager.updateAppWidget(appWidgetId, views)
}