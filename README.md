# NDI-MyInfo.Android

This project is to help developers to setup MyInfo login and fetch the person API
For the detail of the API please refer [here](https://public.cloud.myinfo.gov.sg/myinfo/api/myinfo-kyc-v3.1.1.html)

Setup your project by putting the `myinfo_config.json` and `myinfo_certificate.p12` under your asset folder
Create your `myinfo_config.json` and don't forget to put it to your .gitignore
```json
{
  "client_id": "your project client ID",
  "client_secret": "your project client secret",
  "redirect_uri": "your registered callback URL in dashboard",
  "authorization_scope": "openid email profile",
  "authorization_endpoint_uri": "https://test.api.myinfo.gov.sg/com/v3/authorise",
  "token_endpoint_uri": "https://test.api.myinfo.gov.sg/com/v3/token",
  "environment": "sandbox|test|production",
  "myinfo_attributes": "name,dob,sex,nationality",
  "private_key_secret": ""
}
```
We are using [AppAuth] (https://github.com/openid/AppAuth-Android) under the hood, we need to define the app scheme that we use as the redirect URI
```groovy
android {
    ...
    manifestPlaceholders = [
                'appAuthRedirectScheme': 'your scheme'
    ]
}
```
If you are using https redirect instead of app scheme, you need to define it at the `AndroidManifest.xml`
```xml
        ...
        <activity android:name="net.openid.appauth.RedirectUriReceiverActivity">
            <intent-filter>
                <action android:name="android.intent.action.VIEW" />

                <category android:name="android.intent.category.DEFAULT" />
                <category android:name="android.intent.category.BROWSABLE" />
                <data
                    android:host="your domain"
                    android:path="your path"
                    android:scheme="https" />
            </intent-filter>
        </activity>
```
To support API 30 and above, don't forget to add this as well
```xml
    <queries>
        <intent>
            <action android:name="android.intent.action.VIEW" />
            <category android:name="android.intent.category.BROWSABLE" />
            <data android:scheme="https" />
        </intent>
        <intent>
            <action android:name="android.intent.action.VIEW" />
            <category android:name="android.intent.category.APP_BROWSER" />
            <data android:scheme="https" />
        </intent>
    </queries>
```


To start the login process
```kotlin
    private fun doAuth() {
        startActivityForResult(MyInfo.getInstance().getAuthIntent(), RC_AUTH)
    }

     override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_CANCELED) {
            //authorization flow is cancelled by user
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
                }
            })
        }
    }
```

After the login process is done, to fetch the person API, simply do as code below
We have provided some extensions function to fetch DOB, name, sex, and nationality fields as shown in the example


```kotlin
    MyInfo.getInstance()
                   .getPerson(
                       object : MyInfoCallback<JSONObject> {
                           override fun onSuccess(payload: JSONObject?) {
                              //use the json object response
                            tv_person.text = "Name: ${data.getName().value}" +
                                    "\nDob: ${data.getDob().value}" +
                                    "\nSex: ${data.getSex().desc}" +
                                    "\nNationaility: ${data.getNationality().desc}"
                           }
   
                           override fun onError(throwable: MyInfoException) {
                               //show some error message
                           }
                       })
                                            
```
