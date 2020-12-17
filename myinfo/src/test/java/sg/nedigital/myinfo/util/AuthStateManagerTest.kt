package sg.nedigital.myinfo.util

import android.net.Uri
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.stub
import com.nhaarman.mockitokotlin2.verify
import junit.framework.Assert.assertEquals
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.GrantTypeValues
import net.openid.appauth.TokenRequest
import net.openid.appauth.TokenResponse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import sg.nedigital.myinfo.storage.MyInfoStorage

@RunWith(RobolectricTestRunner::class)
class AuthStateManagerTest {

    private lateinit var authStateManager: AuthStateManager
    private lateinit var storageMock: MyInfoStorage

    @Before
    fun setUp() {
        storageMock = mock()
        storageMock.stub {
            on { readState() }.thenReturn(AuthState())
        }
        authStateManager = AuthStateManager(mock(), storageMock)
    }

    @Test
    fun replace() {
        val state = AuthState()
        authStateManager.replace(state)
        assertEquals(state, authStateManager.current)
        verify(storageMock).writeState(state)
    }

    @Test
    fun updateAfterAuthorization() {
        authStateManager.updateAfterAuthorization(
            AuthorizationResponse.Builder(getMinimalAuthRequestBuilder().build())
                .setAccessToken("123")
                .build(), null)
        assertEquals("123", authStateManager.current.accessToken)
    }

    @Test
    fun updateAfterTokenResponse() {
        authStateManager.updateAfterTokenResponse(
            TokenResponse.Builder(getMinimalTokenRequestBuilder()
                .setRedirectUri(Uri.parse("https://redirect.com"))
                .setAuthorizationCode("code")
                .build())
                .setAccessToken("234")
                .build(), null
        )
        assertEquals("234", authStateManager.current.accessToken)
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
}
