package sg.nedigital.myinfo.util

import android.app.Application
import android.util.Base64
import androidx.test.core.app.ApplicationProvider
import com.google.gson.Gson
import com.nimbusds.jose.EncryptionMethod
import com.nimbusds.jose.JWEAlgorithm
import com.nimbusds.jose.JWEHeader
import com.nimbusds.jose.JWEObject
import com.nimbusds.jose.Payload
import com.nimbusds.jose.crypto.RSAEncrypter
import com.nimbusds.jose.util.Base64URL
import com.nimbusds.jwt.SignedJWT
import net.openid.appauth.GrantTypeValues
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import sg.nedigital.myinfo.entities.Person
import java.io.File
import java.nio.charset.Charset
import java.security.KeyFactory
import java.security.interfaces.RSAPublicKey
import java.security.spec.X509EncodedKeySpec
import java.util.TreeMap


@RunWith(RobolectricTestRunner::class)
class UtilsTest {
    val context = ApplicationProvider.getApplicationContext<Application>()

    @Before
    fun setUp() {
        val configPath = this.javaClass.classLoader!!.getResource("myinfo_certificate.p12")
        val publicKeyPath = this.javaClass.classLoader!!.getResource("public-key.pem")
        Shadows.shadowOf(context.assets).addAssetPath(File(configPath.path).parent)
        Shadows.shadowOf(context.assets).addAssetPath(File(publicKeyPath.path).parent)
    }

    @Test
    fun authHeaderTest() {
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

        assert(header.startsWith("PKI_SIGN"))
        assert(header.contains("app_id=\"client1\""))
        assert(header.contains("signature="))
    }

    @Test
    fun decryptTest() {
        val header = JWEHeader(JWEAlgorithm.RSA_OAEP, EncryptionMethod.A256CBC_HS512)

        val json =
            """{"name": {"value": "TAN XIAO HUI","classification": "C","source": "1","lastupdated": "2019-03-26"}}""" // ktlint-disable

        val signedJWT = SignedJWT(
            Base64URL.encode("""{"alg":"RS256","kid":"key_id"}""".toByteArray()),
            Base64URL.encode(json),
            Base64URL.encode("third".toByteArray())
        )
        val jwe = JWEObject(
            header,
            Payload(signedJWT)
        )
        jwe.encrypt(RSAEncrypter(getPublicKey()))
        val jweString: String = jwe.serialize()

        val result = Utils.decrypt(context, jweString, "12345678")
        assertNotNull(result?.get("name"))
        val attribute = Gson().fromJson(Gson().toJson(result?.get("name")), Person::class.java)
        assertEquals("TAN XIAO HUI", attribute.value)
        assertEquals("C", attribute.classification)
        assertEquals("1", attribute.source)
        assertEquals("2019-03-26", attribute.lastupdated)
    }

    private fun getPublicKey(): RSAPublicKey {
        val bytes = String(
            context.resources.assets.open("public-key.pem").readBytes(),
            Charset.defaultCharset()
        )
        val publicKeyPEM: String = bytes
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace(System.lineSeparator(), "")
            .replace("-----END PUBLIC KEY-----", "")

        val encoded: ByteArray = Base64.decode(publicKeyPEM.toByteArray(), Base64.DEFAULT)

        val keyFactory = KeyFactory.getInstance("RSA")
        val keySpec = X509EncodedKeySpec(encoded)
        return (keyFactory.generatePublic(keySpec) as RSAPublicKey)
    }
}
