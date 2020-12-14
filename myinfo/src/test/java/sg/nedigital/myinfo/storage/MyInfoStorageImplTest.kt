package sg.nedigital.myinfo.storage

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.google.gson.Gson
import junit.framework.Assert.assertEquals
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationServiceConfiguration
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MyInfoStorageImplTest {
    private lateinit var serializer: Gson
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var storage: MyInfoStorage

    @Before
    fun setUp() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        sharedPreferences =
            application.getSharedPreferences("MyInfoStorage", Context.MODE_PRIVATE)
        serializer = Gson()
        storage = MyInfoStorageImpl(serializer, application)
    }

    @After
    fun tearDown() {
        sharedPreferences.edit().clear().commit()
    }

    @Test
    fun getLastKnownConfigHash() {
        storage.acceptConfiguration("123")
        assertEquals("123", storage.getLastKnownConfigHash())
    }

    @Test
    fun readStateBroken() {
        sharedPreferences.edit().putString("KEY_STATE", "broken")
        assertEquals(AuthState().jsonSerializeString(), storage.readState().jsonSerializeString())
    }

    @Test
    fun readState() {
        val state = AuthState(
            AuthorizationServiceConfiguration(
                Uri.parse("https://authEnpoint.com"),
                Uri.parse("https://tokenEndpoint.com")
            )
        )
        storage.writeState(state)
        assertEquals(state.jsonSerializeString(), storage.readState().jsonSerializeString())

        storage.writeState(null)
        assertEquals(AuthState().jsonSerializeString(), storage.readState().jsonSerializeString())
    }
}