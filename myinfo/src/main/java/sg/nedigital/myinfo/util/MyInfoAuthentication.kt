package sg.nedigital.myinfo.util

import java.util.HashMap
import net.openid.appauth.ClientAuthentication

class MyInfoAuthentication(private val secret: String, private val authorization: String) :
    ClientAuthentication {
    val PARAM_CLIENT_ID = "client_id"
    val PARAM_CLIENT_SECRET = "client_secret"
    val PARAM_AUTHORIZATION = "Authorization"

    override fun getRequestHeaders(clientId: String): MutableMap<String, String> {
        val map = LinkedHashMap<String, String>()
        map[PARAM_AUTHORIZATION] = authorization
        return map
    }

    override fun getRequestParameters(clientId: String): MutableMap<String, String> {
        val additionalParameters: MutableMap<String, String> = HashMap()
        additionalParameters[PARAM_CLIENT_ID] = clientId
        additionalParameters[PARAM_CLIENT_SECRET] = secret
        return additionalParameters
    }
}
