package com.snapreceipt.io.ocr.di

import com.snapreceipt.io.domain.repository.OcrRepository
import com.snapreceipt.io.ocr.MLKitOCRService
import com.snapreceipt.io.ocr.OCRService
import com.snapreceipt.io.ocr.repository.OcrRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object OcrServiceModule {
    @Provides
    @Singleton
    fun provideOcrService(): OCRService = MLKitOCRService()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class OcrRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindOcrRepository(
        impl: OcrRepositoryImpl
    ): OcrRepository
}
