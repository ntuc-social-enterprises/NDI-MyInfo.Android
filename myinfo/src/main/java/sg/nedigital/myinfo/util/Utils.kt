package sg.nedigital.myinfo.util

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.nimbusds.jose.JWEObject
import com.nimbusds.jose.crypto.RSADecrypter
import sg.nedigital.myinfo.MyInfoConfiguration
import java.io.InputStream
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Signature
import java.util.Date
import java.util.TreeMap
import java.util.UUID


internal class Utils {
    companion object {
        internal fun getAuthHeader(
            context: Context,
            method: String,
            url: String,
            appId: String,
            params: TreeMap<String, String>,
            privateKeyPassword: String
        ): String {
            val timestamp = Date().time
            val nonce = UUID.randomUUID().toString().replace("-", "")
            val map = mapOf(
                Pair("app_id", appId),
                Pair("nonce", nonce),
                Pair("signature_method", "RS256"),
                Pair("timestamp", timestamp.toString())
            )

            val signature = signData(context, map, url, method, params, privateKeyPassword)
            val res = "PKI_SIGN app_id=\"" + appId +
                    "\",timestamp=\"" + timestamp +
                    "\",nonce=\"" + nonce +
                    "\",signature_method=\"RS256\"" +
                    ",signature=\"" + signature + "\""

            Log.d("test", res)
            return res
        }

        private fun signData(
            context: Context,
            map: Map<String, String>,
            url: String,
            method: String,
            params: TreeMap<String, String>,
            privateKeyPassword: String
        ): String? {
            params.putAll(map)
            var finalParams = ""

            params.forEach {
                finalParams += "&${it.key}=${it.value}"
            }
            val baseStr = "$method&${Uri.parse(url).buildUpon().build()}&${finalParams.removePrefix(
                "&"
            )}"
            Log.d("test", "baseStr: $baseStr")

            val privateKey: PrivateKey = getPrivateKey(context, privateKeyPassword)

            try {
                //We sign the data with the private key. We use RSA algorithm along SHA-256 digest algorithm
                val signature: ByteArray? = Signature.getInstance("SHA256withRSA").run {
                    initSign(privateKey)
                    update(baseStr.toByteArray())
                    sign()
                }

                if (signature != null) {
                    Log.d("test", "Signed successfully")
                    return Base64.encodeToString(signature, Base64.NO_WRAP)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                throw RuntimeException(e)
            }
            return null
        }

        internal fun decrypt(context: Context, body: String, privateKeyPassword: String): MutableMap<String, Any>? {
            val privateKey: PrivateKey = getPrivateKey(context, privateKeyPassword)

            val jweObject = JWEObject.parse(body)
            jweObject.decrypt(RSADecrypter(privateKey))
            val signedJWT = jweObject.payload.toSignedJWT()
            val decryptedText = signedJWT.payload.toJSONObject()
            Log.d("test", "decrypted : " + decryptedText.toString())
            return decryptedText
        }

        private fun getPrivateKey(context: Context, privateKeyPassword: String): PrivateKey {
            val keyStore: KeyStore = KeyStore.getInstance("pkcs12")

            val inputStream: InputStream =
                context.resources.assets.open(MyInfoConfiguration.PRIVATE_KEY_FILE_NAME)
            keyStore.load(inputStream, privateKeyPassword.toCharArray())

            val alias: String = keyStore.aliases().nextElement()
            return keyStore.getKey(alias, privateKeyPassword.toCharArray()) as PrivateKey
        }
    }
}
