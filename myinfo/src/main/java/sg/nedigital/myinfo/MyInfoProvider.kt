package sg.nedigital.myinfo

import android.content.Context
import android.content.Intent
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.ClientSecretPost
import net.openid.appauth.GrantTypeValues
import net.openid.appauth.TokenRequest
import org.json.JSONObject
import sg.nedigital.myinfo.di.MyInfoScope
import sg.nedigital.myinfo.exceptions.MyInfoException
import sg.nedigital.myinfo.repositories.MyInfoRepository
import sg.nedigital.myinfo.util.AuthStateManager
import sg.nedigital.myinfo.util.MyInfoCallback
import javax.inject.Inject

interface MyInfoProvider {
    fun getConfiguration(): MyInfoConfiguration
    fun getAuthIntent(): Intent?
    fun isAuthorized(): Boolean
    fun onPostLogin(context: Context, data: Intent?, callback: MyInfoCallback<String>)
    fun getLatestAccessToken(): String?
    fun getPerson(callback: MyInfoCallback<JSONObject>)
    fun logout()
}

@MyInfoScope
class MyInfoProviderImpl @Inject constructor(
    private val repository: MyInfoRepository,
    private val authStateManager: AuthStateManager,
    private val configuration: MyInfoConfiguration
) : MyInfoProvider {
    override fun getConfiguration() = configuration

    override fun getAuthIntent() = repository.getAuthIntent()

    override fun isAuthorized() =
        authStateManager.current.isAuthorized && !configuration.hasConfigurationChanged()

    override fun onPostLogin(context: Context, data: Intent?, callback: MyInfoCallback<String>) {
        if (data != null) {
            val authService = configuration.createAuthorizationService()

            val response = AuthorizationResponse.fromIntent(data)
            val ex = AuthorizationException.fromIntent(data)

            if (response != null || ex != null) {
                authStateManager.updateAfterAuthorization(response, ex)
            }

            if (response?.authorizationCode != null) {
                // authorization code exchange is required
                authStateManager.updateAfterAuthorization(response, ex)
                val request: AuthorizationRequest = response.request

                authService.performTokenRequest(
                    TokenRequest.Builder(request.configuration, request.clientId)
                        .setGrantType(GrantTypeValues.AUTHORIZATION_CODE)
                        .setRedirectUri(request.redirectUri)
                        .setAuthorizationCode(response.authorizationCode)
                        .build(),
                    ClientSecretPost(configuration.clientSecret)
                ) { response, ex ->
                    authStateManager.updateAfterTokenResponse(response, ex)
                    if (!authStateManager.current.isAuthorized) {
                        val message = ("Authorization Code exchange failed" + ex?.error)
                        callback.onError(MyInfoException(message))
                    } else {
                        callback.onSuccess(authStateManager.current.accessToken)
                    }
                }
            } else if (ex != null) {
                callback.onError(MyInfoException("Authorization flow failed: " + ex.message))
            } else {
                callback.onError(MyInfoException("No authorization state retained - reauthorization required"))
            }
        } else {
            callback.onError(MyInfoException("Intent data passed is empty"))
        }
    }

    override fun getLatestAccessToken(): String? {
        return authStateManager.current.accessToken
    }

    override fun getPerson(callback: MyInfoCallback<JSONObject>) {
        repository.getPerson(configuration.attributes, callback)
    }

    override fun logout() {
        authStateManager.logout()
    }
}
