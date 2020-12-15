package sg.nedigital.myinfo.repositories

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.browser.customtabs.CustomTabsIntent
import com.google.gson.Gson
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import sg.nedigital.myinfo.MyInfoConfiguration
import sg.nedigital.myinfo.MyInfoEnvironment
import sg.nedigital.myinfo.exceptions.MyInfoException
import sg.nedigital.myinfo.services.MyInfoService
import sg.nedigital.myinfo.util.AuthStateManager
import sg.nedigital.myinfo.util.JWTDecoder
import sg.nedigital.myinfo.util.MyInfoCallback
import sg.nedigital.myinfo.util.Utils
import java.util.HashMap
import java.util.TreeMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject

interface MyInfoRepository {
    fun getAuthIntent(): Intent?
    fun getPerson(attributes: String, callback: MyInfoCallback<JSONObject>)
}

class MyInfoRepositoryImpl @Inject constructor(
    private val service: MyInfoService,
    private val authStateManager: AuthStateManager,
    private val configuration: MyInfoConfiguration,
    private val context: Application,
    private val gson: Gson,
    private val jwtDecoder: JWTDecoder
) : MyInfoRepository {
    private var authService: AuthorizationService? = null

    private var executor = Executors.newSingleThreadExecutor()

    private val authRequest: AtomicReference<AuthorizationRequest?> =
        AtomicReference<AuthorizationRequest?>()

    private val authIntent = AtomicReference<CustomTabsIntent>()

    init {
        if (configuration.hasConfigurationChanged()) {
            Log.d("test", "config change")
            // discard any existing authorization state due to the change of configuration
            Log.d("test", "Configuration change detected, discarding old state")
            authStateManager.replace(AuthState())
            configuration.acceptConfiguration()
        } else {
            Log.d("test", "no config change")
        }
        initializeAppAuth()
    }

    private fun initializeClient() {
        // use a statically configured client ID
        initializeAuthRequest()
    }

    private fun initializeAuthRequest() {
        createAuthRequest()
        warmUpBrowser()
    }

    private fun warmUpBrowser() {
        executor.execute {
            authIntent.set(configuration.warmUpBrowser(authService, authRequest.get())!!)
        }
    }

    private fun initializeAppAuth() {
        recreateAuthorizationService()
        if (authStateManager.current.authorizationServiceConfiguration != null) {
            // configuration is already created, skip to client initialization
            initializeClient()
            return
        }

        val config = AuthorizationServiceConfiguration(
            configuration.authEndpointUri,
            configuration.tokenEndpointUri
        )
        authStateManager.replace(AuthState(config))
        initializeClient()
    }

    private fun recreateAuthorizationService() {
        Log.d("test", "Discarding existing AuthService instance")
        authService?.dispose()

        authService = configuration.createAuthorizationService()
        authRequest.set(null)
        authIntent.set(null)
    }

    private fun createAuthRequest() {
        val map: MutableMap<String, String> = HashMap()
        map["authmode"] = "SINGPASS"
        map["attributes"] = configuration.attributes
        map["purpose"] = "demo of myinfo"
        val authRequestBuilder: AuthorizationRequest.Builder =
            AuthorizationRequest.Builder(
                authStateManager.current.authorizationServiceConfiguration!!,
                configuration.clientId,
                ResponseTypeValues.CODE,
                configuration.redirectUri
            ).setScope(configuration.scope)
                .setAdditionalParameters(map)
        authRequest.set(authRequestBuilder.build())
    }

    override fun getAuthIntent(): Intent? {
        return authRequest.get()?.let {
            authService?.getAuthorizationRequestIntent(
                it,
                authIntent.get()
            )
        }
    }

    override fun getPerson(attributes: String, callback: MyInfoCallback<JSONObject>) {
        val at = authStateManager.current.accessToken
        if (at.isNullOrEmpty()) {
            callback.onError(MyInfoException("Access token not found"))
            return
        }
        val jwt = jwtDecoder.decode(at)
        val sub = jwtDecoder.getClaim(jwt)
        if (sub == null) {
            callback.onError(MyInfoException("Invalid access token, no claim found"))
            return
        }

        val authHeader = if (configuration.environment == MyInfoEnvironment.SANDBOX) {
            "Bearer $at"
        } else {
            val params = TreeMap<String, String>()
            params["attributes"] = configuration.attributes
            params["client_id"] = configuration.clientId
            val baseUrl = configuration.host + "person/$sub/"

            Utils.getAuthHeader(
                context,
                "GET",
                baseUrl,
                configuration.clientId,
                params
            ) + ",Bearer $at"
        }
        service.getPerson(
            sub,
            authHeader,
            configuration.clientId,
            attributes
        ).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    val body = response.body()?.string()
                    if(body.isNullOrEmpty()){
                        callback.onError(MyInfoException("Empty response"))
                        return
                    }
                    val jsonElement = if (configuration.environment != MyInfoEnvironment.SANDBOX) {
                        decodeResponse(body)
                    } else {
                        JSONObject(body)
                    }
                    callback.onSuccess(jsonElement)
                } else {
                    callback.onError(MyInfoException(response.errorBody()?.string()))
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                t.printStackTrace()
                callback.onError(MyInfoException("Fail on fetching person API", t))
            }
        })

//        countDownLatch.await()
//            val params = TreeMap<String, String>()
//            params["attributes"] = "name,dob,sex,nationality"
//            params["client_id"] = "STG-T18CS0001E-NTUC-FAIRPRICE"
//            val request: Request = Builder()
//                .url(url)
//                .addHeader(
//                    "Authorization",
//                    Util.Companion.getAuthHeader(this, "GET", baseUrl, params)
//                        .toString() + ",Bearer " + accessToken
//                )
//                .build()
    }

    private fun decodeResponse(response: String): JSONObject {
        return JSONObject(Utils.decrypt(context, response))
    }
}
