package com.snapreceipt.io.di

import android.content.Context
import androidx.room.Room
import com.snapreceipt.io.data.db.ReceiptDao
import com.snapreceipt.io.data.db.SnapReceiptDatabase
import com.snapreceipt.io.data.db.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Singleton
    @Provides
    fun provideDatabase(
        @ApplicationContext context: Context
    ): SnapReceiptDatabase {
        return Room.databaseBuilder(
            context,
            SnapReceiptDatabase::class.java,
            "snapreceipt.db"
        ).build()
    }

    @Singleton
    @Provides
    fun provideReceiptDao(database: SnapReceiptDatabase): ReceiptDao {
        return database.receiptDao()
    }

    @Singleton
    @Provides
    fun provideUserDao(database: SnapReceiptDatabase): UserDao {
        return database.userDao()
    }
}
