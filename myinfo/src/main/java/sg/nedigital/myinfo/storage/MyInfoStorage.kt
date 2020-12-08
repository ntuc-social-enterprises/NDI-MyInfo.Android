package sg.nedigital.myinfo.storage

import android.app.Application
import android.content.Context
import com.google.gson.Gson
import net.openid.appauth.AuthState
import org.json.JSONException
import java.util.concurrent.locks.ReentrantLock
import javax.inject.Inject

interface MyInfoStorage {

    fun getLastKnownConfigHash(): String?
    fun acceptConfiguration(configHash: String)
    fun readState(): AuthState
    fun writeState(state: AuthState?)
}
class MyInfoStorageImpl @Inject constructor(
    private val gson: Gson,
    application: Application
) : MyInfoStorage {

    companion object {
        private const val SP_NAME = "MyInfoStorage"
        private const val KEY_LAST_HASH = "KEY_LAST_HASH"
        private const val KEY_STATE = "KEY_STATE"
    }

    private val sharedPreferences = application.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
    private val prefsLock = ReentrantLock()

    override fun getLastKnownConfigHash(): String? {
        return sharedPreferences.getString(KEY_LAST_HASH, null)
    }

    override fun acceptConfiguration(configHash: String) {
        sharedPreferences.edit().putString(KEY_LAST_HASH, configHash)
            .apply()
    }

    override fun readState(): AuthState {
        prefsLock.lock()
        return try {
            val currentState = sharedPreferences.getString(KEY_STATE, null)
                ?: return AuthState()
            try {
                AuthState.jsonDeserialize(currentState)
            } catch (e: JSONException) {
                AuthState()
            }
        } finally {
            prefsLock.unlock()
        }
    }

    override fun writeState(state: AuthState?) {
        prefsLock.lock()
        try {
            val editor = sharedPreferences.edit()
            if (state == null) {
                editor.remove(KEY_STATE)
            } else {
                editor.putString(KEY_STATE, state.jsonSerializeString())
            }
            check(editor.commit()) { "Failed to write state to shared prefs" }
        } finally {
            prefsLock.unlock()
        }
    }
}