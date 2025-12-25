package com.whattyu.tasks.widget

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.whattyu.tasks.R
import com.whattyu.tasks.data.IlacDatabase
import com.whattyu.tasks.data.IlacGorev

class IlacWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return IlacWidgetFactory(this.applicationContext)
    }
}

sealed interface WidgetItem {
    data class Baslik(val metin: String) : WidgetItem
    data class Gorev(val veri: IlacGorev) : WidgetItem
}

class IlacWidgetFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {

    private var widgetListesi: MutableList<WidgetItem> = ArrayList()
    private val dao = IlacDatabase.getDatabase(context).ilacDao()

    override fun onCreate() {}

    override fun onDataSetChanged() {
        widgetListesi.clear()

        // 1. Fetch all data
        val tumIlaclar = dao.getWidgetListesi()

        // 2. FILTERING (Should it be shown today?)
        val gosterilecekler = tumIlaclar.filter { ilac ->
            gorevGosterilmeliMi(ilac)
        }

        // 3. Separate into Morning, Evening, and Others
        val sabahlar = gosterilecekler.filter { it.zamanDilimi == "Sabah" }
        val aksamlar = gosterilecekler.filter { it.zamanDilimi == "Akşam" }
        // FIX: Capture tasks that do not fit into Morning or Evening
        val digerleri = gosterilecekler.filter { it.zamanDilimi != "Sabah" && it.zamanDilimi != "Akşam" }

        if (sabahlar.isNotEmpty()) {
            widgetListesi.add(WidgetItem.Baslik(context.getString(R.string.header_morning)))
            sabahlar.forEach { widgetListesi.add(WidgetItem.Gorev(it)) }
        }
        if (aksamlar.isNotEmpty()) {
            widgetListesi.add(WidgetItem.Baslik(context.getString(R.string.header_evening)))
            aksamlar.forEach { widgetListesi.add(WidgetItem.Gorev(it)) }
        }
        // FIX: Add the "Other" section to the widget list
        if (digerleri.isNotEmpty()) {
            widgetListesi.add(WidgetItem.Baslik("Diğer")) // You can change this title or make it dynamic
            digerleri.forEach { widgetListesi.add(WidgetItem.Gorev(it)) }
        }
    }

    private fun gorevGosterilmeliMi(ilac: IlacGorev): Boolean {
        if (ilac.sonIslemTarihi == null) return true

        val format = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val bugun = format.format(java.util.Date())

        if (ilac.sonIslemTarihi == bugun) return true

        try {
            val sonTarih = format.parse(ilac.sonIslemTarihi!!)
            val bugunDate = format.parse(bugun) ?: return true

            val farkMillis = bugunDate.time - sonTarih!!.time
            val gecenGun = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(farkMillis)

            val hedefGun = when (ilac.tekrarBirimi) {
                "Hafta", "Week" -> ilac.tekrarAraligi * 7
                "Ay", "Month" -> ilac.tekrarAraligi * 30
                else -> ilac.tekrarAraligi
            }
            return gecenGun >= hedefGun
        } catch (e: Exception) {
            return true
        }
    }

    override fun onDestroy() {}
    override fun getCount(): Int = widgetListesi.size
    override fun getViewTypeCount(): Int = 2
    override fun getItemId(position: Int): Long = position.toLong()
    override fun hasStableIds(): Boolean = true
    override fun getLoadingView(): RemoteViews? = null

    override fun getViewAt(position: Int): RemoteViews {
        val item = widgetListesi[position]

        return when (item) {
            is WidgetItem.Baslik -> {
                val views = RemoteViews(context.packageName, R.layout.widget_header)
                views.setTextViewText(R.id.tvHeader, item.metin)
                views
            }
            is WidgetItem.Gorev -> {
                val views = RemoteViews(context.packageName, R.layout.widget_item)
                val ilac = item.veri

                views.setTextViewText(R.id.widgetIlacIsim, ilac.isim)

                val detayMetni = if (ilac.kalanDoz == -1) {
                    context.getString(R.string.widget_details_infinite_fmt, ilac.tekrarAraligi, ilac.tekrarBirimi)
                } else {
                    context.getString(R.string.widget_details_fmt, ilac.kalanDoz.toString(), ilac.tekrarAraligi, ilac.tekrarBirimi)
                }
                views.setTextViewText(R.id.widgetIlacDetay, detayMetni)

                if (ilac.seciliMi) {
                    views.setImageViewResource(R.id.widgetCheckIcon, R.drawable.ic_check_filled_purple)
                    views.setTextColor(R.id.widgetIlacIsim, android.graphics.Color.parseColor("#66FFFFFF"))
                } else {
                    views.setImageViewResource(R.id.widgetCheckIcon, R.drawable.ic_check_outline)
                    views.setTextColor(R.id.widgetIlacIsim, android.graphics.Color.WHITE)
                }

                val fillInIntent = Intent().apply {
                    putExtra("ILAC_ID", ilac.id)
                    putExtra("MEVCUT_DURUM", ilac.seciliMi)
                    putExtra("KALAN_DOZ", ilac.kalanDoz)
                }

                views.setOnClickFillInIntent(R.id.widgetCheckIcon, fillInIntent)
                views.setOnClickFillInIntent(R.id.widgetItemRoot, fillInIntent)

                views
            }
        }
    }
}