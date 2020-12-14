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
        }
        config.stub {
            on { clientId }.thenReturn("client_id")
            on { authEndpointUri }.thenReturn(Uri.parse("https://authEndpoint.com"))
            on { tokenEndpointUri }.thenReturn(Uri.parse("https://tokenEndpoint.com"))
            on { redirectUri }.thenReturn(Uri.parse("https://redirect.com"))
            on { attributes }.thenReturn("attributes")
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
        whenever(authStateManager.current.accessToken).thenReturn("at")
        whenever(jwtDecoder.decode(any())).thenReturn(mock())
        whenever(jwtDecoder.getClaim(any())).thenReturn("123")
        mockServer.enqueue(MockResponse().setBody("{}"))
        repository.getPerson("attributes", object : MyInfoCallback<JSONObject> {
            override fun onSuccess(payload: JSONObject?) {
                println("success")
            }

            override fun onError(throwable: MyInfoException) {
                Assert.fail(throwable.message)
            }
        })
    }

    @Test
    fun getPersonTestNoAccessToken() {
        whenever(authStateManager.current.accessToken).thenReturn(null)
        whenever(jwtDecoder.decode(any())).thenReturn(mock())
        whenever(jwtDecoder.getClaim(any())).thenReturn("123")
        repository.getPerson("attributes", object : MyInfoCallback<JSONObject> {
            override fun onSuccess(payload: JSONObject?) {
                Assert.fail("should error on no AT")
            }

            override fun onError(throwable: MyInfoException) {
                assertEquals("Access token not found", throwable.message)
            }
        })
    }

    @Test
    fun getPersonTestNoSubClaim() {
        whenever(authStateManager.current.accessToken).thenReturn("at")
        whenever(jwtDecoder.decode(any())).thenReturn(mock())
        whenever(jwtDecoder.getClaim(any())).thenReturn(null)
        repository.getPerson("attributes", object : MyInfoCallback<JSONObject> {
            override fun onSuccess(payload: JSONObject?) {
                Assert.fail("should error on no claim")
            }

            override fun onError(throwable: MyInfoException) {
                assertEquals("Invalid access token, no claim found", throwable.message)
            }
        })
    }

    @Test
    fun getPersonTestFail() {
        whenever(authStateManager.current.accessToken).thenReturn("at")
        whenever(jwtDecoder.decode(any())).thenReturn(mock())
        whenever(jwtDecoder.getClaim(any())).thenReturn("123")
        mockServer.enqueue(MockResponse().setResponseCode(400))
        repository.getPerson("attributes", object : MyInfoCallback<JSONObject> {
            override fun onSuccess(payload: JSONObject?) {
                Assert.fail("should error on return 400")
            }

            override fun onError(throwable: MyInfoException) {
                throwable.printStackTrace()
                println(throwable.message)
            }
        })
    }

    @Test
    fun getPersonTestException() {
        whenever(authStateManager.current.accessToken).thenReturn("at")
        whenever(jwtDecoder.decode(any())).thenReturn(mock())
        whenever(jwtDecoder.getClaim(any())).thenReturn("123")

        mockServer.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))

        repository.getPerson("attributes", object : MyInfoCallback<JSONObject> {
            override fun onSuccess(payload: JSONObject?) {
                Assert.fail("should error on no connection")
            }

            override fun onError(throwable: MyInfoException) {
                throwable.printStackTrace()
                println(throwable.message)
            }
        })
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