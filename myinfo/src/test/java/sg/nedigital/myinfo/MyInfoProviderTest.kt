package sg.nedigital.myinfo

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.stub
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import junit.framework.Assert.assertEquals
import junit.framework.Assert.fail
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationResponse.EXTRA_RESPONSE
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.GrantTypeValues
import net.openid.appauth.TokenRequest
import net.openid.appauth.TokenResponse
import net.openid.appauth.connectivity.DefaultConnectionBuilder
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import sg.nedigital.myinfo.exceptions.MyInfoException
import sg.nedigital.myinfo.repositories.MyInfoRepository
import sg.nedigital.myinfo.util.AuthStateManager
import sg.nedigital.myinfo.util.MyInfoCallback


@RunWith(RobolectricTestRunner::class)
class MyInfoProviderTest {
    private lateinit var provider: MyInfoProvider
    private lateinit var repository: MyInfoRepository
    private lateinit var authStateManager: AuthStateManager
    private lateinit var configuration: MyInfoConfiguration

    @Before
    fun setUp() {
        repository = mock()
        authStateManager = mock()
        configuration = mock()
        configuration.stub {
            on { getConnectionBuilder() }.thenReturn(DefaultConnectionBuilder.INSTANCE)
            on { clientSecret }.thenReturn("secret")
        }
        provider = MyInfoProviderImpl(repository, authStateManager, configuration)
    }

    @Test
    fun getConfiguration() {
        assertEquals(configuration, provider.getConfiguration())
    }

    @Test
    fun getAuthIntent() {
        provider.getAuthIntent()
        verify(repository).getAuthIntent()
    }

    @Test
    fun isAuthorized() {
        val currentMock: AuthState = mock()
        currentMock.stub {
            on { isAuthorized }.thenReturn(true)
        }
        authStateManager.stub {
            on { current }.thenReturn(currentMock)
        }
        configuration.stub {
            on { hasConfigurationChanged() }.thenReturn(false)
        }
        assert(provider.isAuthorized())
    }

    @Test
    fun `isAuthorized with config changes`() {
        val currentMock: AuthState = mock()
        currentMock.stub {
            on { isAuthorized }.thenReturn(true)
        }
        authStateManager.stub {
            on { current }.thenReturn(currentMock)
        }
        configuration.stub {
            on { hasConfigurationChanged() }.thenReturn(true)
        }
        assert(!provider.isAuthorized())
    }

    @Test
    fun `onPostLogin success`() {
        val currentMock: AuthState = mock()
        currentMock.stub {
            on { authorizationServiceConfiguration }.thenReturn(mock())
            on { isAuthorized }.thenReturn(true)
            on { accessToken }.thenReturn("123")
        }
        authStateManager.stub {
            on { current }.thenReturn(currentMock)
        }

        val authServiceMock : AuthorizationService = mock()
        configuration.stub {
            on { createAuthorizationService() }.thenReturn(authServiceMock)
        }
        authServiceMock.stub {
            on { performTokenRequest(any(), any(), any()) }.thenAnswer {
                val callback = it.getArgument(2) as AuthorizationService.TokenResponseCallback
                callback.onTokenRequestCompleted(
                    TokenResponse.Builder(
                        getMinimalTokenRequestBuilder()
                            .setRedirectUri(Uri.parse("https://redirect.com"))
                            .setAuthorizationCode("code")
                            .build()
                    ).setAccessToken("123").build(), null)
            }
        }
        val application = ApplicationProvider.getApplicationContext<Application>()

        val response = AuthorizationResponse.Builder(
            getMinimalAuthRequestBuilder().setRedirectUri(Uri.parse("https://redirect.com")).build())
            .setAuthorizationCode("code")
            .setAccessToken("234")
            .build()

        val intent = Intent()
        intent.putExtra(EXTRA_RESPONSE, response.jsonSerializeString())
        var isSuccess = false
        provider.onPostLogin(application, intent, object: MyInfoCallback<String> {
            override fun onSuccess(payload: String?) {
                isSuccess = true
            }

            override fun onError(throwable: MyInfoException) {
                fail(throwable.message)
            }
        })
        verify(authStateManager, times(2)).updateAfterAuthorization(any(), anyOrNull())
        verify(authStateManager).updateAfterTokenResponse(any(), anyOrNull())
        assert(isSuccess)
    }

    @Test
    fun `onPostLogin no data`() {
        val application = ApplicationProvider.getApplicationContext<Application>()

        var isFail = false
        provider.onPostLogin(application, null, object: MyInfoCallback<String> {
            override fun onSuccess(payload: String?) {
                fail("it should fail on empty intent")
            }

            override fun onError(throwable: MyInfoException) {
                isFail = true
            }
        })
        assert(isFail)
    }

    @Test
    fun `onPostLogin fail`() {
        val currentMock: AuthState = mock()
        currentMock.stub {
            on { authorizationServiceConfiguration }.thenReturn(mock())
        }
        authStateManager.stub {
            on { current }.thenReturn(currentMock)
        }

        val application = ApplicationProvider.getApplicationContext<Application>()

        val intent = Intent()
        intent.putExtra(
            AuthorizationException.EXTRA_EXCEPTION,
            AuthorizationException(0, 0, "error message", "error desc", null, null).toJsonString()
        )
        var isFail = false
        provider.onPostLogin(application, intent, object : MyInfoCallback<String> {
            override fun onSuccess(payload: String?) {
                fail("it should fail on exception not null")
            }

            override fun onError(throwable: MyInfoException) {
                isFail = true
            }
        })
        verify(authStateManager).updateAfterAuthorization(anyOrNull(), any())
        assert(isFail)
    }

    @Test
    fun getLatestAccessToken() {
        val currentMock: AuthState = mock()
        currentMock.stub {
            on { accessToken }.thenReturn("123")
        }
        authStateManager.stub {
            on { current }.thenReturn(currentMock)
        }
        assertEquals("123", provider.getLatestAccessToken())
    }

    @Test
    fun getPerson() {
        configuration.stub {
            on { attributes }.thenReturn("attributes")
        }
        provider.getPerson(object: MyInfoCallback<JSONObject>{
            override fun onSuccess(payload: JSONObject?) {

            }

            override fun onError(throwable: MyInfoException) {
            }
        })
        verify(repository).getPerson(any(), any())
    }


    private fun getMinimalAuthRequestBuilder(): AuthorizationRequest.Builder {
        return AuthorizationRequest.Builder(
            AuthorizationServiceConfiguration(
                Uri.parse("https://authEndpoint.com"),
                Uri.parse("https://tokenEndpoint.com")
            ),
            "client_id",
            "code",
            Uri.parse("https://redirect.com")
        )
    }

    private fun getMinimalTokenRequestBuilder(): TokenRequest.Builder {
        return TokenRequest.Builder(
            AuthorizationServiceConfiguration(
                Uri.parse("https://authEndpoint.com"),
                Uri.parse("https://tokenEndpoint.com")
            ),
            "client_id"
        ).setGrantType(GrantTypeValues.AUTHORIZATION_CODE)
    }

    @Test
    fun logout() {
        provider.logout()
        verify(authStateManager).logout()
    }
}