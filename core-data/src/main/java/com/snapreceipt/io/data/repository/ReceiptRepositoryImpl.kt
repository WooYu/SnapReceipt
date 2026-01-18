package com.snapreceipt.io.data.repository

import com.snapreceipt.io.data.base.BaseRepository
import com.snapreceipt.io.data.local.datasource.ReceiptLocalDataSource
import com.snapreceipt.io.data.mapper.ReceiptDomainToEntityMapper
import com.snapreceipt.io.data.mapper.ReceiptEntityToDomainMapper
import com.snapreceipt.io.domain.model.ReceiptEntity
import com.snapreceipt.io.domain.repository.ReceiptRepository
import com.skybound.space.core.dispatcher.CoroutineDispatchersProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ReceiptRepositoryImpl @Inject constructor(
    private val localDataSource: ReceiptLocalDataSource,
    private val entityToDomainMapper: ReceiptEntityToDomainMapper,
    private val domainToEntityMapper: ReceiptDomainToEntityMapper,
    dispatchers: CoroutineDispatchersProvider
) : BaseRepository(dispatchers), ReceiptRepository {
    override fun getAllReceipts(): Flow<List<ReceiptEntity>> {
        return localDataSource.getAllReceipts().map { entityToDomainMapper.mapList(it) }
    }

    override suspend fun getReceiptById(id: Int): ReceiptEntity? {
        return localDataSource.getReceiptById(id)?.let { entityToDomainMapper.map(it) }
    }

    override suspend fun insertReceipt(receipt: ReceiptEntity): Long {
        return localDataSource.insertReceipt(domainToEntityMapper.map(receipt))
    }

    override suspend fun updateReceipt(receipt: ReceiptEntity) {
        localDataSource.updateReceipt(domainToEntityMapper.map(receipt))
    }

    override suspend fun deleteReceipt(receipt: ReceiptEntity) {
        localDataSource.deleteReceipt(domainToEntityMapper.map(receipt))
    }

    override suspend fun deleteReceipts(ids: List<Int>) {
        localDataSource.deleteReceipts(ids)
    }

    override fun getReceiptsByDateRange(startDate: Long, endDate: Long): Flow<List<ReceiptEntity>> {
        return localDataSource.getReceiptsByDateRange(startDate, endDate)
            .map { entityToDomainMapper.mapList(it) }
    }

    override fun getReceiptsByType(type: String): Flow<List<ReceiptEntity>> {
        return localDataSource.getReceiptsByType(type).map { entityToDomainMapper.mapList(it) }
    }
}
