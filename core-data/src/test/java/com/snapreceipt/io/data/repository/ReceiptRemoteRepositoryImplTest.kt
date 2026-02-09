package com.snapreceipt.io.data.repository

import com.snapreceipt.io.data.network.datasource.ReceiptRemoteDataSource
import com.snapreceipt.io.domain.model.ReceiptEntity
import com.snapreceipt.io.domain.model.query.ReceiptListQueryEntity
import com.skybound.space.core.network.BasePagedResponse
import com.skybound.space.core.network.BaseResponse
import com.skybound.space.core.network.NetworkError
import com.skybound.space.core.network.NetworkResult
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ReceiptRemoteRepositoryImplTest {

    private val dataSource: ReceiptRemoteDataSource = mockk()
    private val repository = ReceiptRemoteRepositoryImpl(dataSource)

    @Test
    fun list_success_returnsMappedList() = runBlocking {
        val query = ReceiptListQueryEntity()
        val rows = listOf(
            ReceiptEntity(
                receiptId = 1L,
                categoryId = 0L,
                receiptUrl = "",
                merchant = "Shop",
                totalAmount = 0.0
            )
        )
        val response = BasePagedResponse(
            code = BaseResponse.SUCCESS_CODE,
            message = "ok",
            rows = rows
        )
        coEvery { dataSource.list(query) } returns NetworkResult.Success(response)

        val result = repository.list(query)

        assertEquals(1, result.size)
        assertEquals(1L, result[0].receiptId)
        assertEquals("Shop", result[0].merchant)
    }

    @Test(expected = com.skybound.space.core.network.ApiException::class)
    fun list_failure_throwsApiException() = runBlocking {
        coEvery { dataSource.list(any()) } returns NetworkResult.Failure(
            NetworkError.Http(500, "Server error")
        )
        repository.list(ReceiptListQueryEntity())
    }

    @Test
    fun list_success_emptyRows_returnsEmptyList() = runBlocking {
        coEvery { dataSource.list(any()) } returns NetworkResult.Success(
            BasePagedResponse(
                code = BaseResponse.SUCCESS_CODE,
                message = "ok",
                rows = emptyList()
            )
        )
        val result = repository.list(ReceiptListQueryEntity())
        assertEquals(emptyList<ReceiptEntity>(), result)
    }
}
