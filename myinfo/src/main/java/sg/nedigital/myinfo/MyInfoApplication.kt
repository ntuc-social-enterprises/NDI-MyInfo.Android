package sg.nedigital.myinfo

import android.app.Application
import sg.nedigital.myinfo.di.DaggerMyInfoComponent
import sg.nedigital.myinfo.di.MyInfoComponent

class MyInfoApplication : Application() {
    internal lateinit var component: MyInfoComponent

    override fun onCreate() {
        super.onCreate()

        component = DaggerMyInfoComponent.factory().create(
            this
        )
        MyInfo.buildMyInfoProvider(this)
    }
}
