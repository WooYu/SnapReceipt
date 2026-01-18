package com.snapreceipt.io.data.di

import com.snapreceipt.io.data.repository.AuthRepositoryImpl
import com.snapreceipt.io.data.repository.ReceiptRepositoryImpl
import com.snapreceipt.io.data.repository.UserRepositoryImpl
import com.snapreceipt.io.domain.repository.AuthRepository
import com.snapreceipt.io.domain.repository.ReceiptRepository
import com.snapreceipt.io.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindReceiptRepository(
        impl: ReceiptRepositoryImpl
    ): ReceiptRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(
        impl: UserRepositoryImpl
    ): UserRepository
}
