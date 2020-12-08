package sg.nedigital.myinfo

import android.app.Application

class MyInfo {
    companion object {
        @Volatile
        private lateinit var instance: MyInfoProvider

        fun getInstance(): MyInfoProvider {
            return instance
        }

        fun buildMyInfoProvider(application: Application): MyInfoProvider {
            instance = (application as MyInfoApplication).component.myInfoProvider()
            return instance
        }
    }
}
