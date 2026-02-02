package com.snapreceipt.io.domain.usecase.receipt

import com.snapreceipt.io.domain.model.ReceiptEntity
import com.snapreceipt.io.domain.model.query.ReceiptListQueryEntity
import com.snapreceipt.io.domain.repository.ReceiptRemoteRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FetchReceiptsUseCaseTest {

    private val repository: ReceiptRemoteRepository = mockk()
    private val useCase = FetchReceiptsUseCase(repository)

    @Test
    fun invoke_callsRepositoryWithQuery_returnsSuccess() = runBlocking {
        val query = ReceiptListQueryEntity(receiptDateEnd = "2026-01-01")
        val list = listOf(
            ReceiptEntity(receiptId = 1L, merchant = "Test")
        )
        coEvery { repository.list(query) } returns list

        val result = useCase(query)

        assertTrue(result.isSuccess)
        assertEquals(list, result.getOrNull())
    }

    @Test
    fun invoke_defaultQuery_returnsEmptyList() = runBlocking {
        coEvery { repository.list(ReceiptListQueryEntity()) } returns emptyList()

        val result = useCase()

        assertTrue(result.isSuccess)
        assertEquals(emptyList<ReceiptEntity>(), result.getOrNull())
    }

    @Test
    fun invoke_repositoryThrows_returnsFailure() = runBlocking {
        coEvery { repository.list(any()) } throws RuntimeException("Network error")

        val result = useCase(ReceiptListQueryEntity())

        assertTrue(result.isFailure)
        assertEquals("Network error", result.exceptionOrNull()?.message)
    }
}
