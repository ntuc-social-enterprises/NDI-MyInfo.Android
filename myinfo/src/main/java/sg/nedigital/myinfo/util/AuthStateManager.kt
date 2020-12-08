package sg.nedigital.myinfo.util

import android.content.Context
import net.openid.appauth.*
import sg.nedigital.myinfo.storage.MyInfoStorage
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject

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

    fun updateAfterRegistration(
        response: RegistrationResponse?,
        ex: AuthorizationException?
    ): AuthState {
        val current = current
        if (ex != null) {
            return current
        }
        current.update(response)
        return replace(current)
    }

    private fun readState(): AuthState {
        return storage.readState()
    }

    private fun writeState(state: AuthState?) {
        storage.writeState(state)
    }
}