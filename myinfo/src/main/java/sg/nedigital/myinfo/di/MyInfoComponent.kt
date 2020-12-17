package sg.nedigital.myinfo.di

import android.app.Application
import dagger.BindsInstance
import dagger.Component
import sg.nedigital.myinfo.MyInfoConfiguration
import sg.nedigital.myinfo.MyInfoProvider
import sg.nedigital.myinfo.repositories.MyInfoRepository
import sg.nedigital.myinfo.services.MyInfoService
import sg.nedigital.myinfo.util.AuthStateManager

@MyInfoScope
@Component(
    modules = [
        NetworkModule::class,
        AppModule::class
    ]
)
interface MyInfoComponent {

    fun application(): Application

    fun apiService(): MyInfoService

    fun configuration(): MyInfoConfiguration

    fun myInfoRepository(): MyInfoRepository

    fun myInfoProvider(): MyInfoProvider

    fun authStateManager(): AuthStateManager

    @Component.Factory
    interface Factory {
        fun create(
            @BindsInstance application: Application
        ): MyInfoComponent
    }
}
