package com.whattyu.tasks.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [IlacGorev::class], version = 1, exportSchema = false)
abstract class IlacDatabase : RoomDatabase() {

    abstract fun ilacDao(): IlacDao

    companion object {
        @Volatile
        private var INSTANCE: IlacDatabase? = null

        fun getDatabase(context: Context): IlacDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    IlacDatabase::class.java,
                    "ilac_veritabani"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}