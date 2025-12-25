package com.whattyu.tasks.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface IlacDao {

    @Query("SELECT * FROM ilac_tablosu WHERE zamanDilimi NOT IN ('Sabah', 'Akşam') ORDER BY seciliMi ASC")
    fun getDigerGorevleri(): Flow<List<IlacGorev>>
    @Query("SELECT * FROM ilac_tablosu WHERE zamanDilimi = 'Sabah' ORDER BY seciliMi ASC")
    fun getSabahGorevleri(): Flow<List<IlacGorev>>

    @Query("SELECT * FROM ilac_tablosu WHERE zamanDilimi = 'Akşam' ORDER BY seciliMi ASC")
    fun getAksamGorevleri(): Flow<List<IlacGorev>>

    @Query("SELECT * FROM ilac_tablosu ORDER BY seciliMi ASC")
    fun getWidgetListesi(): List<IlacGorev>

    @Query("SELECT * FROM ilac_tablosu WHERE id = :id LIMIT 1")
    suspend fun getIlacById(id: Int): IlacGorev?

    @Insert
    suspend fun ilacEkle(ilac: IlacGorev)

    @Update
    suspend fun ilacGuncelle(ilac: IlacGorev)

    @Delete
    suspend fun ilacSil(ilac: IlacGorev)
}