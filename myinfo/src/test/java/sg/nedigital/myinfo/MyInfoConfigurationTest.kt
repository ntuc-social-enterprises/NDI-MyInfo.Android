package sg.nedigital.myinfo

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.content.res.Resources
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.test.core.app.ApplicationProvider
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.stub
import com.nhaarman.mockitokotlin2.verify
import java.io.File
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertNotNull
import junit.framework.Assert.assertNull
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationService
import net.openid.appauth.connectivity.DefaultConnectionBuilder
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import sg.nedigital.myinfo.storage.MyInfoStorage

@RunWith(RobolectricTestRunner::class)
class MyInfoConfigurationTest {
    private val context = ApplicationProvider.getApplicationContext<Application>()
    private lateinit var storage: MyInfoStorage
    private lateinit var configuration: MyInfoConfiguration
    private lateinit var mockContext: Context

    @Before
    fun setUp() {
        val certPath = this.javaClass.classLoader!!.getResource("myinfo_certificate.p12")
        val configPath = this.javaClass.classLoader!!.getResource("myinfo_config.json")
        Shadows.shadowOf(context.assets).addAssetPath(File(certPath.path).parent)
        Shadows.shadowOf(context.assets).addAssetPath(File(configPath.path).parent)
        storage = mock()
        mockContext = mock()
        val assetMock: AssetManager = mock()
        assetMock.stub {
            on { open(any()) }.thenReturn(
                context.resources.assets.open(
                    MyInfoConfiguration.CONFIG_INFO_FILE_NAME
                )
            )
        }
        val mockAssets: Resources = mock()
        mockAssets.stub {
            on { assets }.thenReturn(assetMock)
            on { getColor(any()) }.thenReturn(context.resources.getColor(android.R.color.black))
        }
        val packageManagerMock: PackageManager = mock()
        packageManagerMock.stub {
            on { queryIntentActivities(any(), any()) }.thenReturn(listOf(mock()))
        }
        mockContext.stub {
            on { resources }.thenReturn(mockAssets)
            on { packageManager }.thenReturn(packageManagerMock)
            on { packageName }.thenReturn(context.packageName)
        }
        configuration = MyInfoConfiguration(mockContext, storage)
    }

    @Test
    fun hasConfigurationChanged() {
        assert(configuration.hasConfigurationChanged())
    }

    @Test
    fun isValid() {
        assert(configuration.isValid())
    }

    @Test
    fun getConfigurationError() {
        assertNull(configuration.getConfigurationError())
    }

    @Test
    fun acceptConfiguration() {
        configuration.acceptConfiguration()
        verify(storage).acceptConfiguration(any())
    }

    @Test
    fun getConnectionBuilder() {
        assertEquals(DefaultConnectionBuilder.INSTANCE, configuration.getConnectionBuilder())
    }

    @Test
    fun warmUpBrowser() {
        val customTabMock: CustomTabsIntent.Builder = CustomTabsIntent.Builder()
        val authMock: AuthorizationService = mock()
        authMock.stub {
            on { createCustomTabsIntentBuilder(any()) }.thenReturn(customTabMock)
        }
        val authRequest: AuthorizationRequest = mock()
        authRequest.stub {
            on { toUri() }.thenReturn(Uri.parse("https://redirect.com"))
        }
        assertNotNull(configuration.warmUpBrowser(authMock, authRequest))
    }

    @Test
    fun attributeTest() {
        assertEquals("https://redirect.com/callback", configuration.redirectUri.toString())
        assertEquals("client1", configuration.clientId)
        assertEquals("client_secret", configuration.clientSecret)
        assertEquals("openid email profile", configuration.scope)
        assertEquals("name,dob,sex,nationality", configuration.attributes)
        assertEquals("12345678", configuration.privateKeyPassword)
        assertEquals("https://test.api.myinfo.gov.sg/com/v3/", configuration.host)
        assertEquals(MyInfoEnvironment.TEST, configuration.environment)
        assert(configuration.debugLog)
        assertEquals(
            "https://test.api.myinfo.gov.sg/com/v3/token",
            configuration.tokenEndpointUri.toString()
        )
        assertEquals(
            "https://test.api.myinfo.gov.sg/com/v3/authorise",
            configuration.authEndpointUri.toString()
        )
    }
}
