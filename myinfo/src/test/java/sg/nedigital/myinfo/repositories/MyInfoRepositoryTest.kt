package sg.nedigital.myinfo.repositories

import android.app.Application
import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.net.Uri
import android.os.Build
import com.google.gson.GsonBuilder
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.stub
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import java.util.concurrent.CountDownLatch
import junit.framework.Assert.assertEquals
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationService
import net.openid.appauth.connectivity.DefaultConnectionBuilder
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.json.JSONObject
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.util.ReflectionHelpers
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import sg.nedigital.myinfo.MyInfoConfiguration
import sg.nedigital.myinfo.MyInfoEnvironment
import sg.nedigital.myinfo.exceptions.MyInfoException
import sg.nedigital.myinfo.services.MyInfoService
import sg.nedigital.myinfo.util.AuthStateManager
import sg.nedigital.myinfo.util.JWTDecoder
import sg.nedigital.myinfo.util.MyInfoCallback

@RunWith(RobolectricTestRunner::class)
internal class MyInfoRepositoryTest {

    private val mockServer = MockWebServer()

    private lateinit var service: MyInfoService

    private lateinit var repository: MyInfoRepository
    private lateinit var context: Application
    private lateinit var config: MyInfoConfiguration
    private lateinit var authStateManager: AuthStateManager
    private lateinit var jwtDecoder: JWTDecoder
    private lateinit var authServiceMock: AuthorizationService

    val gson = GsonBuilder()
        .create()

    @Before
    fun setUp() {
        ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", 0)

        mockServer.start()
        val retrofit = Retrofit.Builder().baseUrl(mockServer.url("/"))
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        service = retrofit.create(MyInfoService::class.java)
        authStateManager = mock()
        config = mock()
        jwtDecoder = mock()
        context = mock()
        authServiceMock = mock()
        val resourceMock: Resources = mock()

        resourceMock.stub {
            on { getColor(any()) }.thenReturn(Color.parseColor("#000000"))
        }
        context.stub {
            on { resources }.thenReturn(resourceMock)
        }
        val currentMock: AuthState = mock()
        currentMock.stub {
            on { authorizationServiceConfiguration }.thenReturn(mock())
        }
        authStateManager.stub {
            on { current }.thenReturn(currentMock)
        }
        config.stub {
            on { getConnectionBuilder() }.thenReturn(DefaultConnectionBuilder.INSTANCE)
            on { warmUpBrowser(anyOrNull(), anyOrNull()) }.thenReturn(mock())
            on { createAuthorizationService() }.thenReturn(authServiceMock)
            on { clientId }.thenReturn("client_id")
            on { authEndpointUri }.thenReturn(Uri.parse("https://authEndpoint.com"))
            on { tokenEndpointUri }.thenReturn(Uri.parse("https://tokenEndpoint.com"))
            on { redirectUri }.thenReturn(Uri.parse("https://redirect.com"))
            on { attributes }.thenReturn("attributes")
            on { environment }.thenReturn(MyInfoEnvironment.SANDBOX)
        }

        repository = MyInfoRepositoryImpl(
            service,
            authStateManager,
            config,
            context,
            gson,
            jwtDecoder
        )
    }

    @After
    fun tearDown() {
        mockServer.shutdown()
    }

    @Test
    fun getPersonTestSuccess() {
        val latch = CountDownLatch(1)

        whenever(authStateManager.current.accessToken).thenReturn("at")
        whenever(jwtDecoder.decode(any())).thenReturn(mock())
        whenever(jwtDecoder.getClaim(any())).thenReturn("123")
        mockServer.enqueue(MockResponse().setBody("{}"))
        repository.getPerson("attributes", object : MyInfoCallback<JSONObject> {
            override fun onSuccess(payload: JSONObject?) {
                println("success")
                latch.countDown()
            }

            override fun onError(throwable: MyInfoException) {
                Assert.fail(throwable.message)
                latch.countDown()
            }
        })
        latch.await()
    }

    @Test
    fun `getPersonTest success no body`() {
        val latch = CountDownLatch(1)

        whenever(authStateManager.current.accessToken).thenReturn("at")
        whenever(jwtDecoder.decode(any())).thenReturn(mock())
        whenever(jwtDecoder.getClaim(any())).thenReturn("123")
        mockServer.enqueue(MockResponse().setResponseCode(200))
        repository.getPerson("attributes", object : MyInfoCallback<JSONObject> {
            override fun onSuccess(payload: JSONObject?) {
                Assert.fail("it should fail with empty body")
                latch.countDown()
            }

            override fun onError(throwable: MyInfoException) {
                assertEquals("Empty response", throwable.message)
                latch.countDown()
            }
        })
        latch.await()
    }

    @Test
    fun getPersonTestNoAccessToken() {
        val latch = CountDownLatch(1)
        whenever(authStateManager.current.accessToken).thenReturn(null)
        whenever(jwtDecoder.decode(any())).thenReturn(mock())
        whenever(jwtDecoder.getClaim(any())).thenReturn("123")
        repository.getPerson("attributes", object : MyInfoCallback<JSONObject> {
            override fun onSuccess(payload: JSONObject?) {
                Assert.fail("should error on no AT")
                latch.countDown()
            }

            override fun onError(throwable: MyInfoException) {
                assertEquals("Access token not found", throwable.message)
                latch.countDown()
            }
        })
        latch.await()
    }

    @Test
    fun getPersonTestNoSubClaim() {
        val latch = CountDownLatch(1)
        whenever(authStateManager.current.accessToken).thenReturn("at")
        whenever(jwtDecoder.decode(any())).thenReturn(mock())
        whenever(jwtDecoder.getClaim(any())).thenReturn(null)
        repository.getPerson("attributes", object : MyInfoCallback<JSONObject> {
            override fun onSuccess(payload: JSONObject?) {
                Assert.fail("should error on no claim")
                latch.countDown()
            }

            override fun onError(throwable: MyInfoException) {
                assertEquals("Invalid access token, no claim found", throwable.message)
                latch.countDown()
            }
        })
        latch.await()
    }

    @Test
    fun getPersonTestFail() {
        val latch = CountDownLatch(1)
        whenever(authStateManager.current.accessToken).thenReturn("at")
        whenever(jwtDecoder.decode(any())).thenReturn(mock())
        whenever(jwtDecoder.getClaim(any())).thenReturn("123")
        mockServer.enqueue(MockResponse().setResponseCode(400))
        repository.getPerson("attributes", object : MyInfoCallback<JSONObject> {
            override fun onSuccess(payload: JSONObject?) {
                Assert.fail("should error on return 400")
                latch.countDown()
            }

            override fun onError(throwable: MyInfoException) {
                throwable.printStackTrace()
                println(throwable.message)
                latch.countDown()
            }
        })
        latch.await()
    }

    @Test
    fun getPersonTestException() {
        val latch = CountDownLatch(1)
        whenever(authStateManager.current.accessToken).thenReturn("at")
        whenever(jwtDecoder.decode(any())).thenReturn(mock())
        whenever(jwtDecoder.getClaim(any())).thenReturn("123")

        mockServer.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))

        repository.getPerson("attributes", object : MyInfoCallback<JSONObject> {
            override fun onSuccess(payload: JSONObject?) {
                Assert.fail("should error on no connection")
                latch.countDown()
            }

            override fun onError(throwable: MyInfoException) {
                throwable.printStackTrace()
                println(throwable.message)
                latch.countDown()
            }
        })
        latch.await()
    }

    @Test
    fun getIntentTest() {
        val mock: Intent = mock()
        whenever(authServiceMock.getAuthorizationRequestIntent(any(), any())).thenReturn(mock)
        assertEquals(mock, repository.getAuthIntent())
    }

    @Test
    fun newConfigTest() {
        whenever(config.hasConfigurationChanged()).thenReturn(true)

        val currentMock: AuthState = mock()
        currentMock.stub {
            on { authorizationServiceConfiguration }.thenReturn(null).thenReturn(mock())
        }
        authStateManager.stub {
            on { current }.thenReturn(currentMock)
        }

        repository = MyInfoRepositoryImpl(
            service,
            authStateManager,
            config,
            context,
            gson,
            jwtDecoder
        )
        verify(authStateManager, times(2)).replace(any())
        verify(config).acceptConfiguration()
    }
}
