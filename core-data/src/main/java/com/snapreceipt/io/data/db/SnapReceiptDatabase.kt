package com.snapreceipt.io.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ReceiptEntity::class, UserEntity::class],
    version = 1,
    exportSchema = false
)
abstract class SnapReceiptDatabase : RoomDatabase() {
    abstract fun receiptDao(): ReceiptDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var instance: SnapReceiptDatabase? = null

        fun getInstance(context: Context): SnapReceiptDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    SnapReceiptDatabase::class.java,
                    "snapreceipt.db"
                ).build().also { instance = it }
            }
    }
}
