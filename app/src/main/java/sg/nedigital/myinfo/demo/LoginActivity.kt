package sg.nedigital.myinfo.demo

/*
* Copyright 2015 The AppAuth for Android Authors. All Rights Reserved.
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
* in compliance with the License. You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software distributed under the
* License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
* express or implied. See the License for the specific language governing permissions and
* limitations under the License.
*/
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlinx.android.synthetic.main.activity_login.*
import org.json.JSONObject
import sg.nedigital.myinfo.MyInfo
import sg.nedigital.myinfo.exceptions.MyInfoException
import sg.nedigital.myinfo.extensions.getDob
import sg.nedigital.myinfo.extensions.getName
import sg.nedigital.myinfo.extensions.getNationality
import sg.nedigital.myinfo.extensions.getSex
import sg.nedigital.myinfo.util.MyInfoCallback

/**
 * Demonstrates the usage of the AppAuth to authorize a user with an OAuth2 / OpenID Connect
 * provider. Based on the configuration provided in `res/raw/auth_config.json`, the code
 * contained here will:
 *
 * - Retrieve an OpenID Connect discovery document for the provider, or use a local static
 * configuration.
 * - Utilize dynamic client registration, if no static client id is specified.
 * - Initiate the authorization request using the built-in heuristics or a user-selected browser.
 *
 * _NOTE_: From a clean checkout of this project, the authorization service is not configured.
 * Edit `res/values/auth_config.xml` to provide the required configuration properties. See the
 * README.md in the app/ directory for configuration instructions, and the adjacent IDP-specific
 * instructions.
 */
class LoginActivity : AppCompatActivity() {
    private lateinit var mExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mExecutor = Executors.newSingleThreadExecutor()

        setContentView(R.layout.activity_login)
        start_auth.setOnClickListener { startAuth() }
        button_person.setOnClickListener {
            MyInfo.getInstance()
                .getPerson(
                    object : MyInfoCallback<JSONObject> {
                        override fun onSuccess(payload: JSONObject?) {
                            val data = payload!!
                            tv_person.text = "Name: ${data.getName().value}\nDob: ${data.getDob().value}\nSex: ${data.getSex().desc}\nNationaility: ${data.getNationality().desc}"
                        }

                        override fun onError(throwable: MyInfoException) {
                            Snackbar.make(
                                coordinator,
                                throwable.message ?: "Unknown error",
                                Snackbar.LENGTH_SHORT
                            ).show()

                            tv_person.text = throwable.message ?: "Unknown error"
                        }
                    })
        }

        if (intent.getBooleanExtra(EXTRA_FAILED, false)) {
            displayAuthCancelled()
        }
        displayLoading("Initializing")
        displayAuthOptions()

        if (!MyInfo.getInstance().getConfiguration().isValid()) {
            displayError(
                MyInfo.getInstance().getConfiguration().getConfigurationError() ?: "Config error",
                false
            )
            return
        }

        if (MyInfo.getInstance().isAuthorized()) {
            Log.i(TAG, "User is already authenticated, proceeding to token activity")
//            startActivity(Intent(this, TokenActivity::class.java))
//            finish()

            tv_access_token.text = "Access token : ${MyInfo.getInstance().getLatestAccessToken()}"
        }
    }

    @Override
    override fun onStart() {
        super.onStart()

        if (mExecutor.isShutdown) {
            mExecutor = Executors.newSingleThreadExecutor()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mExecutor.shutdownNow()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        displayAuthOptions()
        if (resultCode == RESULT_CANCELED) {
            displayAuthCancelled()
        } else {
            MyInfo.getInstance().onPostLogin(this, data, object : MyInfoCallback<String> {
                override fun onSuccess(payload: String?) {
                    tv_access_token.text = "Access token : $payload"
                }

                override fun onError(throwable: MyInfoException) {
                    Snackbar.make(
                        coordinator,
                        throwable.message ?: "Unknown error",
                        Snackbar.LENGTH_SHORT
                    ).show()
                    tv_access_token.text = "Error fetching access token: ${throwable.message}"
                }
            })
//            val intent = Intent(this, TokenActivity::class.java)
//            intent.putExtras(data!!.extras!!)
//            startActivity(intent)
        }
    }

    @MainThread
    fun startAuth() {
        displayLoading("Making authorization request")
        // WrongThread inference is incorrect for lambdas
        // noinspection WrongThread
        mExecutor.submit { doAuth() }
    }

    /**
     * Performs the authorization request, using the browser selected in the spinner,
     * and a user-provided `login_hint` if available.
     */
    @WorkerThread
    private fun doAuth() {
        startActivityForResult(MyInfo.getInstance().getAuthIntent(), RC_AUTH)
    }

    @MainThread
    private fun displayLoading(loadingMessage: String) {
        loading_container.visibility = View.VISIBLE
        auth_container.visibility = View.GONE
        error_container.visibility = View.GONE
        loading_description.text = loadingMessage
    }

    @MainThread
    private fun displayError(error: String, recoverable: Boolean) {
        error_container.visibility = View.VISIBLE
        loading_container.visibility = View.GONE
        auth_container.visibility = View.GONE
        error_description.text = error
        retry.visibility = if (recoverable) View.VISIBLE else View.GONE
    }

    @MainThread
    private fun displayAuthOptions() {
        auth_container.visibility = View.VISIBLE
        loading_container.visibility = View.GONE
        error_container.visibility = View.GONE

        var authEndpointStr: String = "Static auth endpoint: \n"

        authEndpointStr += MyInfo.getInstance().getAuthEndpoint()
        auth_endpoint.text = authEndpointStr

        var clientIdStr: String = "Static client ID: \n"
        clientIdStr += MyInfo.getInstance().getConfiguration().clientId
        client_id.text = clientIdStr
    }

    private fun displayAuthCancelled() {
        Snackbar.make(
            findViewById(R.id.coordinator),
            "Authorization canceled",
            Snackbar.LENGTH_SHORT
        ).show()
    }

    companion object {
        private const val TAG = "LoginActivity"
        private const val EXTRA_FAILED = "failed"
        private const val RC_AUTH = 100
    }
}
