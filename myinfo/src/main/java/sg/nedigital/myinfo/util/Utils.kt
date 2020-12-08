package sg.nedigital.myinfo.util

import android.content.Context
import android.net.Uri
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.UserNotAuthenticatedException
import android.util.Base64
import android.util.Log
import android.widget.Toast
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Signature
import java.util.*

class Utils {
    companion object {
        fun getAuthHeader(
            context: Context,
            method: String,
            url: String,
            params: TreeMap<String, String>
        ): String {
            val appId = "NTUC-FAIRPRICE"
            val timestamp = Date().time
            val nonce = UUID.randomUUID().toString().replace("-", "")
            val map = mapOf(
                Pair("app_id", appId),
                Pair("nonce", nonce),
                Pair("signature_method", "RS256"),
                Pair("timestamp", timestamp.toString())
            )

//            val signature = signData(context, map, url, method, params) //todo wait for private cert to be ready
            val signature = ""
            val res = "PKI_SIGN app_id=\"" + appId +
                    "\",timestamp=\"" + timestamp +
                    "\",nonce=\"" + nonce +
                    "\",signature_method=\"RS256\"" +
                    ",signature=\"" + signature + "\""

            Log.d("test", res)
            return res
        }

//        fun signData(context: Context, map: Map<String, String>, url: String, method: String, params: TreeMap<String, String>): String? {
//            params.putAll(map)
//            var finalParams = ""
//
//            params.forEach {
//                finalParams += "&${it.key}=${it.value}"
//            }
//            val baseStr = "$method&${Uri.parse(url).buildUpon().build()}&${finalParams.removePrefix("&")}"
//            Log.d("test", "baseStr: $baseStr")
//
//            if(!checkKeyExists()){
//                generateKey(context)
//            }
//
//            try {
//                //We get the Keystore instance
//                val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
//                    load(null)
//                }
//
//                //Retrieves the private key from the keystore
//                val privateKey: PrivateKey = keyStore.getKey(KEY_ALIAS, null) as PrivateKey
//
//                //We sign the data with the private key. We use RSA algorithm along SHA-256 digest algorithm
//                val signature: ByteArray? = Signature.getInstance("SHA256withRSA").run {
//                    initSign(privateKey)
//                    update(baseStr.toByteArray())
//                    sign()
//                }
//
//                if (signature != null) {
//                    Log.d("test", "Signed successfully")
//                    return Base64.encodeToString(signature, Base64.NO_WRAP)
//                }
//
//            } catch (e: UserNotAuthenticatedException) {
//                e.printStackTrace()
//            } catch (e: KeyPermanentlyInvalidatedException) {
//                //Exception thrown when the key has been invalidated for example when lock screen has been disabled.
//                Toast.makeText(context, "Keys are invalidated.\n" + e.message, Toast.LENGTH_LONG).show()
//            } catch (e: Exception) {
//                e.printStackTrace()
//                throw RuntimeException(e)
//            }
//            return null
//        }

    }
}