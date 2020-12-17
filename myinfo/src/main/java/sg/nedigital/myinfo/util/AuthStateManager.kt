package sg.nedigital.myinfo.util

import android.content.Context
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.TokenResponse
import sg.nedigital.myinfo.storage.MyInfoStorage

class AuthStateManager @Inject internal constructor(
    context: Context,
    private val storage: MyInfoStorage
) {
    private val currentAuthState: AtomicReference<AuthState> = AtomicReference()

    val current: AuthState
        get() {
            if (currentAuthState.get() != null) {
                return currentAuthState.get()
            }
            val state = readState()
            return if (currentAuthState.compareAndSet(null, state)) {
                state
            } else {
                currentAuthState.get()
            }
        }

    fun replace(state: AuthState): AuthState {
        writeState(state)
        currentAuthState.set(state)
        return state
    }

    fun updateAfterAuthorization(
        response: AuthorizationResponse?,
        ex: AuthorizationException?
    ): AuthState {
        val current = current
        current.update(response, ex)
        return replace(current)
    }

    fun updateAfterTokenResponse(
        response: TokenResponse?,
        ex: AuthorizationException?
    ): AuthState {
        val current = current
        current.update(response, ex)
        return replace(current)
    }

    private fun readState(): AuthState {
        return storage.readState()
    }

    private fun writeState(state: AuthState?) {
        storage.writeState(state)
    }

    fun logout() {
        // discard the authorization and token state, but retain the configuration and
        // dynamic client registration (if applicable), to save from retrieving them again.
        val currentState: AuthState = current
        val clearedState = AuthState(currentState.authorizationServiceConfiguration!!)
        if (currentState.lastRegistrationResponse != null) {
            clearedState.update(currentState.lastRegistrationResponse)
        }
        replace(clearedState)
    }
}
