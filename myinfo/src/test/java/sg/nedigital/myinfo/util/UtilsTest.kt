package sg.nedigital.myinfo.util

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import net.openid.appauth.GrantTypeValues
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import java.io.File
import java.util.TreeMap

@RunWith(RobolectricTestRunner::class)
class UtilsTest {

    @Test
    fun authHeaderTest() {
        val context = ApplicationProvider.getApplicationContext<Application>()

        val configPath = this.javaClass.classLoader!!.getResource("myinfo_certificate.p12")
        Shadows.shadowOf(context.assets).addAssetPath(File(configPath.path).parent)

        val params = TreeMap<String, String>()
        params["grant_type"] = GrantTypeValues.AUTHORIZATION_CODE
        params["code"] = "12345"
        params["redirect_uri"] = "https://redirect.com"
        params["client_id"] = "client1"
        params["client_secret"] = "12345678"

        val header = Utils.getAuthHeader(
            context,
            "GET",
            "https://test.api.myinfo.gov.sg/com/v3/token",
            "client1",
            params,
            "12345678"
        )
        println(header)
        assert(header.startsWith("PKI_SIGN"))
        assert(header.contains("app_id=\"client1\""))
        assert(header.contains("signature="))
    }

}