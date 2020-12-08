package sg.nedigital.myinfo

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.TextUtils
import android.util.Log
import net.openid.appauth.AuthState
import net.openid.appauth.connectivity.ConnectionBuilder
import net.openid.appauth.connectivity.DefaultConnectionBuilder
import okio.Buffer
import okio.BufferedSource
import okio.Okio
import org.json.JSONException
import org.json.JSONObject
import sg.nedigital.myinfo.di.MyInfoScope
import sg.nedigital.myinfo.exceptions.InvalidConfigurationException
import sg.nedigital.myinfo.storage.MyInfoStorage
import java.io.IOException
import java.nio.charset.Charset

@MyInfoScope
class MyInfoConfiguration constructor(
    val context: Context,
    private val storage: MyInfoStorage
) {
    companion object {
        const val CONFIG_INFO_FILE_NAME = "myinfo_config.json"
    }

    init {
        readConfiguration()
    }

    val host = when (environment) {
        MyInfoEnvironment.SANDBOX -> "https://sandbox.api.myinfo.gov.sg/com/v3/"
        MyInfoEnvironment.TEST -> "https://test.api.myinfo.gov.sg/com/v3/"
        MyInfoEnvironment.PRODUCTION -> "https://api.myinfo.gov.sg/com/v3/"
    }
    val debugLog = environment != MyInfoEnvironment.PRODUCTION

    private lateinit var environment: MyInfoEnvironment
    private var configJson: JSONObject? = null
    private var configHash: String? = null
    private val configError: String? = null

    lateinit var clientId: String
    lateinit var clientSecret: String
    lateinit var attributes: String
    var scope: String? = null
    lateinit var redirectUri: Uri
    lateinit var authEndpointUri: Uri
    lateinit var tokenEndpointUri: Uri

    /**
     * Indicates whether the configuration has changed from the last known valid state.
     */
    fun hasConfigurationChanged(): Boolean {
        val lastHash = getLastKnownConfigHash()
        return configHash != lastHash
    }

    /**
     * Indicates whether the current configuration is valid.
     */
    fun isValid(): Boolean {
        return configError == null
    }

    /**
     * Returns a description of the configuration error, if the configuration is invalid.
     */
    fun getConfigurationError(): String? {
        return configError
    }

    /**
     * Indicates that the current configuration should be accepted as the "last known valid"
     * configuration.
     */
    fun acceptConfiguration() {
        configHash?.let {
            storage.acceptConfiguration(it)
        }
    }

    fun getConnectionBuilder(): ConnectionBuilder {
        return DefaultConnectionBuilder.INSTANCE
    }

    private fun getLastKnownConfigHash(): String? {
        return storage.getLastKnownConfigHash()
    }

    @Throws(InvalidConfigurationException::class)
    private fun readConfiguration() {
        val configSource: BufferedSource =
            Okio.buffer(Okio.source(context.resources.assets.open(CONFIG_INFO_FILE_NAME)))
        val configData = Buffer()
        try {
            configSource.readAll(configData)
            configHash = configData.sha256().base64()
            configJson = JSONObject(configData.readString(Charset.forName("UTF-8")))
        } catch (ex: IOException) {
            throw InvalidConfigurationException(
                "Failed to read configuration: " + ex.message
            )
        } catch (ex: JSONException) {
            throw InvalidConfigurationException(
                "Unable to parse configuration: " + ex.message
            )
        }
        clientId = getConfigString("client_id") ?: throw InvalidConfigurationException(
            "client_id is empty"
        )
        clientSecret = getConfigString("client_secret") ?: throw InvalidConfigurationException(
            "client_secret is empty"
        )
        attributes = getConfigString("myinfo_attributes") ?: throw InvalidConfigurationException(
            "myinfo_attributes is empty"
        )
        val environmentString = getConfigString("environment")
        environment = when(environmentString) {
            MyInfoEnvironment.SANDBOX.slug -> MyInfoEnvironment.SANDBOX
            MyInfoEnvironment.TEST.slug -> MyInfoEnvironment.TEST
            MyInfoEnvironment.PRODUCTION.slug -> MyInfoEnvironment.PRODUCTION
            else -> throw InvalidConfigurationException(
                "environment must be one of sandbox, test, or production"
            )
        }

        scope = getRequiredConfigString("authorization_scope")
        redirectUri = getRequiredConfigUri("redirect_uri")
        if (!isRedirectUriRegistered()) {
            throw InvalidConfigurationException(
                "redirect_uri is not handled by any activity in this app! "
                        + "Ensure that the appAuthRedirectScheme in your build.gradle file "
                        + "is correctly configured, or that an appropriate intent filter "
                        + "exists in your app manifest."
            )
        }
        authEndpointUri = getRequiredConfigWebUri("authorization_endpoint_uri")
        tokenEndpointUri = getRequiredConfigWebUri("token_endpoint_uri")
    }

    fun getConfigString(propName: String?): String? {
        var value = configJson!!.optString(propName) ?: return null
        value = value.trim { it <= ' ' }
        return if (TextUtils.isEmpty(value)) {
            null
        } else value
    }

    @Throws(InvalidConfigurationException::class)
    private fun getRequiredConfigString(propName: String): String {
        return getConfigString(propName)
            ?: throw InvalidConfigurationException(
                "$propName is required but not specified in the configuration"
            )
    }

    @Throws(InvalidConfigurationException::class)
    fun getRequiredConfigUri(propName: String): Uri {
        val uriStr = getRequiredConfigString(propName)
        val uri: Uri
        uri = try {
            Uri.parse(uriStr)
        } catch (ex: Throwable) {
            throw InvalidConfigurationException(
                "$propName could not be parsed",
                ex
            )
        }
        if (!uri.isHierarchical || !uri.isAbsolute) {
            throw InvalidConfigurationException(
                "$propName must be hierarchical and absolute"
            )
        }
        if (!TextUtils.isEmpty(uri.encodedUserInfo)) {
            throw InvalidConfigurationException("$propName must not have user info")
        }
        if (!TextUtils.isEmpty(uri.encodedQuery)) {
            throw InvalidConfigurationException("$propName must not have query parameters")
        }
        if (!TextUtils.isEmpty(uri.encodedFragment)) {
            throw InvalidConfigurationException("$propName must not have a fragment")
        }
        return uri
    }

    @Throws(InvalidConfigurationException::class)
    fun getRequiredConfigWebUri(propName: String): Uri {
        val uri = getRequiredConfigUri(propName)
        val scheme = uri.scheme
        if (TextUtils.isEmpty(scheme) || !("http" == scheme || "https" == scheme)) {
            throw InvalidConfigurationException(
                "$propName must have an http or https scheme"
            )
        }
        return uri
    }

    private fun isRedirectUriRegistered(): Boolean {
        // ensure that the redirect URI declared in the configuration is handled by some activity
        // in the app, by querying the package manager speculatively
        val redirectIntent = Intent()
        redirectIntent.setPackage(context.packageName)
        redirectIntent.action = Intent.ACTION_VIEW
        redirectIntent.addCategory(Intent.CATEGORY_BROWSABLE)
        redirectIntent.data = redirectUri
        return !context.packageManager.queryIntentActivities(redirectIntent, 0).isEmpty()
    }
}

enum class MyInfoEnvironment(val slug: String) {
    SANDBOX("sandbox"),
    TEST("test"),
    PRODUCTION("production")
}