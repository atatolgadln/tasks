package com.whattyu.tasks.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.whattyu.tasks.data.IlacDatabase
import com.whattyu.tasks.data.IlacGorev
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import com.whattyu.tasks.widget.IlacWidgetProvider

class IlacViewModel(application: Application) : AndroidViewModel(application) {

    private val ilacDao = IlacDatabase.getDatabase(application).ilacDao()

    val sabahGorevleri: LiveData<List<IlacGorev>> = ilacDao.getSabahGorevleri()
        .map { liste -> liste.filter { gorevGosterilmeliMi(it) } }
        .asLiveData()

    val aksamGorevleri: LiveData<List<IlacGorev>> = ilacDao.getAksamGorevleri()
        .map { liste -> liste.filter { gorevGosterilmeliMi(it) } }
        .asLiveData()

    val digerGorevleri: LiveData<List<IlacGorev>> = ilacDao.getDigerGorevleri()
        .map { liste -> liste.filter { gorevGosterilmeliMi(it) } }
        .asLiveData()

    init {
        gunlukTemizlikYap()
    }

    private fun widgetiTetikle() {
        IlacWidgetProvider.widgetlariGuncelle(getApplication())
    }

    private fun gorevGosterilmeliMi(ilac: IlacGorev): Boolean {
        if (ilac.sonIslemTarihi == null) return true

        val bugun = bugununTarihi()

        if (ilac.sonIslemTarihi == bugun) return true

        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        try {
            val sonTarih = format.parse(ilac.sonIslemTarihi!!)
            val bugunDate = format.parse(bugun) ?: return true

            val farkMillis = bugunDate.time - sonTarih!!.time
            val gecenGun = TimeUnit.MILLISECONDS.toDays(farkMillis)

            val hedefGun = when (ilac.tekrarBirimi) {
                "Hafta", "Week" -> ilac.tekrarAraligi * 7
                "Ay", "Month" -> ilac.tekrarAraligi * 30
                else -> ilac.tekrarAraligi // "Gün", "Day"
            }

            return gecenGun >= hedefGun

        } catch (e: Exception) {
            return true
        }
    }

    private fun bugununTarihi(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    private fun gunlukTemizlikYap() = viewModelScope.launch(Dispatchers.IO) {
        val bugun = bugununTarihi()

        val widgetListesi = ilacDao.getWidgetListesi()

        widgetListesi.forEach { ilac ->
            if (ilac.seciliMi && ilac.sonIslemTarihi != bugun) {
                ilac.seciliMi = false
                ilacDao.ilacGuncelle(ilac)
            }
        }
        widgetiTetikle()
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
        ilacDao.ilacEkle(yeniIlac)
        widgetiTetikle()
    }

    fun guncelle(ilac: IlacGorev) = viewModelScope.launch(Dispatchers.IO) {
        ilacDao.ilacGuncelle(ilac)
        com.whattyu.tasks.widget.IlacWidgetProvider.widgetlariGuncelle(getApplication())
    }

    fun ilacDurumunuGuncelle(ilac: IlacGorev, isChecked: Boolean) = viewModelScope.launch {
        val guncelIlac = ilac.copy(seciliMi = isChecked)
        val bugun = bugununTarihi()

        if (isChecked) {
            if (guncelIlac.kalanDoz != -1) guncelIlac.kalanDoz -= 1
            guncelIlac.sonIslemTarihi = bugun
        } else {
            if (guncelIlac.kalanDoz != -1) guncelIlac.kalanDoz += 1
        }

        if (guncelIlac.kalanDoz != -1 && guncelIlac.kalanDoz <= 0) {
            ilacDao.ilacSil(guncelIlac)
        } else {
            ilacDao.ilacGuncelle(guncelIlac)
        }
        widgetiTetikle()
    }

    fun topluSil(silinecekler: List<IlacGorev>) = viewModelScope.launch {
        silinecekler.forEach { ilacDao.ilacSil(it) }
        widgetiTetikle()
    }

    fun sil(ilac: IlacGorev) = viewModelScope.launch {
        ilacDao.ilacSil(ilac)
        widgetiTetikle()
    }
}