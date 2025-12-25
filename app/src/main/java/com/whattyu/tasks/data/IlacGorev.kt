package com.whattyu.tasks.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ilac_tablosu")
data class IlacGorev(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val isim: String,
    val zamanDilimi: String,

    var kalanDoz: Int,
    val tekrarAraligi: Int,
    val tekrarBirimi: String,

    var seciliMi: Boolean = false,

    var sonIslemTarihi: String? = null
)