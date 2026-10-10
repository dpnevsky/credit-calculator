package com.example.creditcalculatormobile.core.network

import com.example.creditcalculatormobile.data.model.StoredAuthTokens
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class TokenAuthenticatorTest {
    private val original = StoredAuthTokens("old-access", "old-refresh", 0L, "Bearer")
    private fun unauthorized(server: MockWebServer): Response = Response.Builder()
        .request(Request.Builder().url(server.url("/api/applications")).header("Authorization", "Bearer old-access").build())
        .protocol(Protocol.HTTP_1_1).code(401).message("Unauthorized").build()
    private fun authenticator(server: MockWebServer, store: InMemoryTokenStore, failure: (String) -> Unit = {}) =
        TokenAuthenticator(ApiConfig(server.url("/").toString(), 2, 2, false), store, Json, failure)
    private fun success() = MockResponse().setHeader("Content-Type", "application/json")
        .setBody("""{"accessToken":"new-access","refreshToken":"new-refresh","expiresIn":300,"tokenType":"Bearer"}""")

    @Test fun `refresh retries once with the rotated token`() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(success())
            val store = InMemoryTokenStore(original)
            val retry = authenticator(server, store).authenticate(null, unauthorized(server))
            assertEquals("Bearer new-access", retry?.header("Authorization"))
            assertEquals("new-refresh", store.readTokens()?.refreshToken)
            assertEquals("/api/auth/refresh", server.takeRequest().path)
        }
    }

    @Test fun `server outage does not erase tokens`() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(503))
            val store = InMemoryTokenStore(original)
            var failed = false
            assertNull(authenticator(server, store) { failed = true }.authenticate(null, unauthorized(server)))
            assertEquals(original, store.readTokens())
            assertFalse(failed)
        }
    }

    @Test fun `rejected refresh clears the old session`() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(401))
            val store = InMemoryTokenStore(original)
            var failed = false
            assertNull(authenticator(server, store) { failed = true }.authenticate(null, unauthorized(server)))
            assertNull(store.readTokens())
            assertTrue(failed)
        }
    }

    @Test fun `logout while refresh is pending cannot restore tokens`() = runBlocking {
        MockWebServer().use { server ->
            val started = CountDownLatch(1)
            val release = CountDownLatch(1)
            server.dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    started.countDown()
                    check(release.await(5, TimeUnit.SECONDS))
                    return success()
                }
            }
            val store = InMemoryTokenStore(original)
            val auth = authenticator(server, store)
            val response = unauthorized(server)
            val executor = Executors.newSingleThreadExecutor()
            try {
                val pending = executor.submit<Request?> { auth.authenticate(null, response) }
                assertTrue(started.await(5, TimeUnit.SECONDS))
                store.clear()
                release.countDown()
                assertNull(pending.get(5, TimeUnit.SECONDS))
                assertNull(store.readTokens())
            } finally { release.countDown(); executor.shutdownNow() }
        }
    }

    @Test fun `old request cannot be retried in a new account session`() = runBlocking {
        MockWebServer().use { server ->
            val store = InMemoryTokenStore(original)
            val taggedRequest = unauthorized(server).request.newBuilder()
                .tag(SessionVersion::class.java, SessionVersion(store.sessionVersion)).build()
            val oldResponse = unauthorized(server).newBuilder().request(taggedRequest).build()
            store.saveTokens(StoredAuthTokens("another-access", "another-refresh", 300L, "Bearer"))
            assertNull(authenticator(server, store).authenticate(null, oldResponse))
            assertEquals(0, server.requestCount)
            assertEquals("another-refresh", store.readTokens()?.refreshToken)
        }
    }

    @Test fun `concurrent unauthorized responses use one refresh`() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(success())
            val store = InMemoryTokenStore(original)
            val auth = authenticator(server, store)
            val response = unauthorized(server)
            val pool = Executors.newFixedThreadPool(2)
            try {
                val first = pool.submit<Request?> { auth.authenticate(null, response) }
                val second = pool.submit<Request?> { auth.authenticate(null, response) }
                assertEquals("Bearer new-access", first.get(5, TimeUnit.SECONDS)?.header("Authorization"))
                assertEquals("Bearer new-access", second.get(5, TimeUnit.SECONDS)?.header("Authorization"))
                assertEquals(1, server.requestCount)
            } finally { pool.shutdownNow() }
        }
    }
}
