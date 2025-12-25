package com.whattyu.tasks.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.whattyu.tasks.data.IlacDatabase
import com.whattyu.tasks.data.IlacGorev
import com.whattyu.tasks.data.IlacRepository
import com.whattyu.tasks.widget.IlacWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class IlacViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: IlacRepository

    // Formatter for database date strings (yyyy-MM-dd)
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    init {
        val dao = IlacDatabase.getDatabase(application).ilacDao()
        repository = IlacRepository(dao)
        gunlukTemizlikYap()
    }

    val sabahGorevleri: LiveData<List<IlacGorev>> = repository.sabahGorevleri
        .map { list -> list.filter { shouldShowTask(it) } }
        .asLiveData()

    val aksamGorevleri: LiveData<List<IlacGorev>> = repository.aksamGorevleri
        .map { list -> list.filter { shouldShowTask(it) } }
        .asLiveData()

    val digerGorevleri: LiveData<List<IlacGorev>> = repository.digerGorevleri
        .map { list -> list.filter { shouldShowTask(it) } }
        .asLiveData()

    /**
     * Determines if a task should be visible today based on its interval.
     */
    private fun shouldShowTask(ilac: IlacGorev): Boolean {
        // 1. If never processed, show it.
        val lastDateStr = ilac.sonIslemTarihi ?: return true

        try {
            val today = LocalDate.now()
            val lastDate = LocalDate.parse(lastDateStr, dateFormatter)

            // 2. If processed today, keep showing it (so the user sees it's done/checked)
            if (lastDate.isEqual(today)) return true

            // 3. Calculate interval in days
            val daysPassed = ChronoUnit.DAYS.between(lastDate, today)

            val intervalDays = when (ilac.tekrarBirimi) {
                "Hafta", "Week" -> ilac.tekrarAraligi * 7L
                "Ay", "Month" -> ilac.tekrarAraligi * 30L
                else -> ilac.tekrarAraligi.toLong() // "Gün", "Day"
            }

            // 4. Show only if enough time has passed
            return daysPassed >= intervalDays

        } catch (e: Exception) {
            e.printStackTrace()
            return true // Fail safe: show task if date parsing fails
        }
    }

    private fun gunlukTemizlikYap() = viewModelScope.launch(Dispatchers.IO) {
        val todayStr = LocalDate.now().format(dateFormatter)
        val widgetList = repository.getWidgetListesi()

        // Uncheck items that were checked on previous days
        widgetList.forEach { ilac ->
            if (ilac.seciliMi && ilac.sonIslemTarihi != todayStr) {
                ilac.seciliMi = false
                repository.update(ilac)
            }
        }
        updateWidgets()
    }

    fun ekle(isim: String, zaman: String, aralik: Int, birim: String, toplamTekrar: Int) = viewModelScope.launch {
        val yeniIlac = IlacGorev(
            isim = isim,
            zamanDilimi = zaman,
            kalanDoz = toplamTekrar,
            tekrarAraligi = aralik,
            tekrarBirimi = birim,
            sonIslemTarihi = null
        )
        repository.insert(yeniIlac)
        updateWidgets()
    }

    fun ilacDurumunuGuncelle(ilac: IlacGorev, isChecked: Boolean) = viewModelScope.launch {
        val todayStr = LocalDate.now().format(dateFormatter)
        val guncelIlac = ilac.copy(seciliMi = isChecked)

        if (isChecked) {
            // Checked: Decrease dose, update date
            if (guncelIlac.kalanDoz != -1) guncelIlac.kalanDoz -= 1
            guncelIlac.sonIslemTarihi = todayStr
        } else {
            // Unchecked: Increase dose back (undo)
            if (guncelIlac.kalanDoz != -1) guncelIlac.kalanDoz += 1
            // Note: We don't revert the date to null because we want to know it was interacted with recently,
            // but if you strictly want to undo date, you'd need the previous date logic.
            // Keeping date as todayStr is safer to prevent it from disappearing if 'shouldShowTask' relies on it.
        }

        if (guncelIlac.kalanDoz != -1 && guncelIlac.kalanDoz <= 0) {
            repository.delete(guncelIlac)
        } else {
            repository.update(guncelIlac)
        }
        updateWidgets()
    }

    fun sil(ilac: IlacGorev) = viewModelScope.launch {
        repository.delete(ilac)
        updateWidgets()
    }

    private fun updateWidgets() {
        IlacWidgetProvider.widgetlariGuncelle(getApplication())
    }

    // IlacViewModel.kt içine ekle:
    suspend fun getGorevById(id: Int): IlacGorev? {
        return repository.getGorevById(id)
        // Eğer repository'de yoksa, IlacDao'daki getIlacById fonksiyonunu repository'e ekle.
    }
}