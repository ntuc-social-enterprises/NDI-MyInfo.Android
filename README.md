# NDI-MyInfo.Android

This project is to help developers to setup MyInfo login and fetch the person API
For the detail of the API please refer [here](https://public.cloud.myinfo.gov.sg/myinfo/api/myinfo-kyc-v3.1.1.html)

Setup your project by putting the `myinfo_config.json` and `myinfo_certificate.p12` under your asset folder

To start the login process
```kotlin
    private fun doAuth() {
        startActivityForResult(MyInfo.getInstance().getAuthIntent(), RC_AUTH)
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
                    showLoginState()
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
        }
    }
```

After the login process is done, to fetch the person API, simply do this

```kotlin
    MyInfo.getInstance()
                   .getPerson(
                       object : MyInfoCallback<JSONObject> {
                           override fun onSuccess(payload: JSONObject?) {
                              //use the json object response
                           }
   
                           override fun onError(throwable: MyInfoException) {
                               //show some error message
                           }
                       })
                                            
```
