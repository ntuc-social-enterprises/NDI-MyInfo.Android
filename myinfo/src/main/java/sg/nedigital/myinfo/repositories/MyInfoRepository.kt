package sg.nedigital.myinfo.repositories

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.browser.customtabs.CustomTabsIntent
import com.auth0.android.jwt.JWT
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import net.openid.appauth.*
import net.openid.appauth.browser.AnyBrowserMatcher
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import sg.nedigital.myinfo.MyInfoConfiguration
import sg.nedigital.myinfo.entities.Person
import sg.nedigital.myinfo.exceptions.MyInfoException
import sg.nedigital.myinfo.services.MyInfoService
import sg.nedigital.myinfo.util.AuthStateManager
import sg.nedigital.myinfo.util.MyInfoCallback
import java.util.*
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
    private val gson: Gson
) : MyInfoRepository {
    private var mAuthService: AuthorizationService? = null
    private var mExecutor = Executors.newSingleThreadExecutor()

    private val mAuthRequest: AtomicReference<AuthorizationRequest?> =
        AtomicReference<AuthorizationRequest?>()

    private val mAuthIntent = AtomicReference<CustomTabsIntent>()

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
        Log.i("test", "Using static client ID: " + configuration.clientId)
        // use a statically configured client ID
        initializeAuthRequest()
    }

    private fun initializeAuthRequest() {
        createAuthRequest()
        warmUpBrowser()
//        displayAuthOptions()
    }

    private fun warmUpBrowser() {
        mExecutor.execute {
            Log.i("test", "Warming up browser instance for auth request")
            val intentBuilder: CustomTabsIntent.Builder =
                mAuthService?.createCustomTabsIntentBuilder(mAuthRequest.get()?.toUri())!!
            intentBuilder.setToolbarColor(context.resources.getColor(sg.nedigital.myinfo.R.color.myinfo_primary_color))
            mAuthIntent.set(intentBuilder.build())
        }
    }

    private fun initializeAppAuth() {
        Log.d("test", "Initializing AppAuth")
        recreateAuthorizationService()
        if (authStateManager.current.authorizationServiceConfiguration != null) {
            // configuration is already created, skip to client initialization
            Log.i("test", "auth config already established")
            initializeClient()
            return
        }

        Log.i("test", "Creating auth config from res/raw/auth_config.json")
        val config = AuthorizationServiceConfiguration(
            configuration.authEndpointUri,
            configuration.tokenEndpointUri
        )
        authStateManager.replace(AuthState(config))
        initializeClient()
    }

    private fun recreateAuthorizationService() {
        Log.d("test", "Discarding existing AuthService instance")
        mAuthService?.dispose()

        mAuthService = createAuthorizationService()
        mAuthRequest.set(null)
        mAuthIntent.set(null)
    }

    private fun createAuthorizationService(): AuthorizationService {
        Log.i("test", "Creating authorization service")
        val builder = AppAuthConfiguration.Builder()
        builder.setBrowserMatcher(AnyBrowserMatcher.INSTANCE)
        builder.setConnectionBuilder(configuration.getConnectionBuilder())

        return AuthorizationService(context, builder.build())
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
        mAuthRequest.set(authRequestBuilder.build())
    }

    override fun getAuthIntent(): Intent? {
        return mAuthRequest.get()?.let {
            mAuthService?.getAuthorizationRequestIntent(
                it,
                mAuthIntent.get()
            )
        }
    }

    override fun getPerson(attributes: String, callback: MyInfoCallback<JSONObject>) {
        val at = authStateManager.current.accessToken
        if (at.isNullOrEmpty()) {
            callback.onError(MyInfoException("Access token not found"))
            return
        }
        val jwt = JWT(at)
        val sub = jwt.getClaim("sub").asString()!!

        service.getPerson(
            sub,
            "Bearer $at",
            configuration.clientId,
            attributes
        ).enqueue(object : Callback<JsonElement> {
            override fun onResponse(call: Call<JsonElement>, response: Response<JsonElement>) {
                if (response.isSuccessful) {
                    val json = gson.toJson(response.body()?.asJsonObject)
                    callback.onSuccess(JSONObject(json))
                } else {
                    callback.onError(MyInfoException(response.errorBody()?.string()))
                }
            }

            override fun onFailure(call: Call<JsonElement>, t: Throwable) {
                callback.onError(MyInfoException("Fail on fetching person API", t))
            }
        })

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
}