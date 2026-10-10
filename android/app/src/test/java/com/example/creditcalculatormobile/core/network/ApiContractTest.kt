package com.example.creditcalculatormobile.core.network

import com.example.creditcalculatormobile.data.repository.ApplicationsRepository
import com.example.creditcalculatormobile.data.remote.ApplicationsApi
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.Response

class ApiContractTest {
    @Test fun `signing consumes the backend JSON contract`() = runTest {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setHeader("Content-Type", "application/json").setBody(
                """{"applicationId":"app-1","contractNumber":"CC-1","contractStatus":"SIGNED","applicationStatus":"CONTRACT_SIGNED","signed":true,"signedAt":"2026-10-10T12:00:00Z","signatureId":"sig-1"}"""))
            val json = Json { ignoreUnknownKeys = true }
            val api = Retrofit.Builder().baseUrl(server.url("/"))
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType())).build()
                .create(ApplicationsApi::class.java)
            val result = ApplicationsRepository(api, json).signContract("app-1")
            assertTrue(result is ApiResult.Success)
            val contract = (result as ApiResult.Success).value
            assertTrue(contract.signed)
            assertEquals("CONTRACT_SIGNED", contract.applicationStatus)
            val request = server.takeRequest()
            assertEquals("POST", request.method)
            assertEquals("/api/applications/app-1/contract/sign", request.path)
        }
    }

    @Test fun `HTTP conflicts retain the server message`() = runTest {
        val result = safeApiCall<String>(Json) {
            Response.error(409, okhttp3.ResponseBody.create("application/json".toMediaType(), """{"message":"Documents not ready"}"""))
        }
        assertEquals("Documents not ready", (result as ApiResult.Failure).error.message)
    }

    @Test fun `cancellation is propagated`() = runTest {
        try {
            safeApiCall<String>(Json) { throw CancellationException("screen closed") }
            fail("Cancellation must not become an API error")
        } catch (_: CancellationException) { }
    }
}
