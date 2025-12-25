package com.whattyu.tasks.data

import kotlinx.coroutines.flow.Flow

class IlacRepository(private val ilacDao: IlacDao) {

    // Veritabanı akışları (Flows)
    val sabahGorevleri: Flow<List<IlacGorev>> = ilacDao.getSabahGorevleri()
    val aksamGorevleri: Flow<List<IlacGorev>> = ilacDao.getAksamGorevleri()
    val digerGorevleri: Flow<List<IlacGorev>> = ilacDao.getDigerGorevleri()

    // Widget listesi
    suspend fun getWidgetListesi(): List<IlacGorev> = ilacDao.getWidgetListesi()

    // Tek bir görevi ID ile getirme (Düzenleme ekranı için gerekli)
    suspend fun getGorevById(id: Int): IlacGorev? {
        return ilacDao.getIlacById(id)
    }

    // Ekleme, Güncelleme, Silme
    suspend fun insert(ilac: IlacGorev) = ilacDao.ilacEkle(ilac)
    suspend fun update(ilac: IlacGorev) = ilacDao.ilacGuncelle(ilac)
    suspend fun delete(ilac: IlacGorev) = ilacDao.ilacSil(ilac)
}