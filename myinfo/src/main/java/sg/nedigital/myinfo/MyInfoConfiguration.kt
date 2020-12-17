package sg.nedigital.myinfo

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.TextUtils
import androidx.browser.customtabs.CustomTabsIntent
import net.openid.appauth.AppAuthConfiguration
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationService
import net.openid.appauth.browser.AnyBrowserMatcher
import net.openid.appauth.connectivity.ConnectionBuilder
import net.openid.appauth.connectivity.DefaultConnectionBuilder
import okio.Buffer
import okio.BufferedSource
import okio.buffer
import okio.source
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
        const val PRIVATE_KEY_FILE_NAME = "myinfo_certificate.p12"
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

    internal lateinit var environment: MyInfoEnvironment
    private var configJson: JSONObject? = null
    private var configHash: String? = null
    private val configError: String? = null

    lateinit var clientId: String
    internal lateinit var clientSecret: String
    internal lateinit var attributes: String
    internal lateinit var privateKeyPassword: String
    internal var scope: String? = null
    internal lateinit var redirectUri: Uri
    internal lateinit var authEndpointUri: Uri
    internal lateinit var tokenEndpointUri: Uri

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
    internal fun acceptConfiguration() {
        configHash?.let {
            storage.acceptConfiguration(it)
        }
    }

    internal fun getConnectionBuilder(): ConnectionBuilder {
        return DefaultConnectionBuilder.INSTANCE
    }

    private fun getLastKnownConfigHash(): String? {
        return storage.getLastKnownConfigHash()
    }

    internal fun createAuthorizationService(): AuthorizationService {
        val builder = AppAuthConfiguration.Builder()
        builder.setBrowserMatcher(AnyBrowserMatcher.INSTANCE)
        builder.setConnectionBuilder(getConnectionBuilder())

        return AuthorizationService(context, builder.build())
    }

    @Throws(InvalidConfigurationException::class)
    private fun readConfiguration() {
        val configSource: BufferedSource =
                context.resources.assets.open(CONFIG_INFO_FILE_NAME).source().buffer()
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
        clientId = getConfigString("client_id")
                ?: throw InvalidConfigurationException(
                        "client_id is empty"
                )
        clientSecret = getConfigString("client_secret")
                ?: throw InvalidConfigurationException(
                        "client_secret is empty"
                )
        attributes = getConfigString("myinfo_attributes")
                ?: throw InvalidConfigurationException(
                        "myinfo_attributes is empty"
                )
        privateKeyPassword = getConfigString("private_key_secret")
                ?: throw InvalidConfigurationException(
                        "private_key_secret is empty"
                )
        val environmentString = getConfigString("environment")
        environment = when (environmentString) {
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
                            "redirect_uri is not handled by any activity in this app! " +
                            "Ensure that the appAuthRedirectScheme in your build.gradle file " +
                            "is correctly configured, or that an appropriate intent filter " +
                            "exists in your app manifest."
            )
        }
        authEndpointUri = getRequiredConfigWebUri("authorization_endpoint_uri")
        tokenEndpointUri = getRequiredConfigWebUri("token_endpoint_uri")
    }

    private fun getConfigString(propName: String?): String? {
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
    internal fun getRequiredConfigUri(propName: String): Uri {
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
    internal fun getRequiredConfigWebUri(propName: String): Uri {
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

    internal fun warmUpBrowser(
        authService: AuthorizationService?,
        authRequest: AuthorizationRequest?
    ): CustomTabsIntent? {
        val intentBuilder: CustomTabsIntent.Builder =
                authService?.createCustomTabsIntentBuilder(authRequest?.toUri())!!
        intentBuilder.setToolbarColor(context.resources.getColor(R.color.myinfo_primary_color))
        return intentBuilder.build()
    }
}

enum class MyInfoEnvironment(val slug: String) {
    SANDBOX("sandbox"),
    TEST("test"),
    PRODUCTION("production")
}
