package com.snapreceipt.io.ocr

import com.skybound.space.core.network.NetworkError
import com.skybound.space.core.network.NetworkResult
import com.snapreceipt.io.data.network.datasource.FileRemoteDataSource
import com.snapreceipt.io.data.network.datasource.ReceiptRemoteDataSource
import com.snapreceipt.io.data.network.datasource.UploadRemoteDataSource
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class BackendOCRServiceTest {
    @Test
    fun testMapHttpError() = runBlocking {
        val receiptRemote = mockk<ReceiptRemoteDataSource>()
        coEvery { receiptRemote.scan(any()) } returns
            NetworkResult.Failure(NetworkError.Http(60004, "blurry"))

        val service = BackendOCRService(
            mockk<FileRemoteDataSource>(relaxed = true),
            mockk<UploadRemoteDataSource>(relaxed = true),
            receiptRemote
        )

        val result = service.recognizeImage("https://image.snapreceipt.io/receipts/555.jpg")
        assertEquals(60004, result?.code)
        assertEquals("blurry", result?.msg)
    }
}
