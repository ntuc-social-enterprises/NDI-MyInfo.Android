package sg.nedigital.myinfo.di

import dagger.Binds
import dagger.Module
import sg.nedigital.myinfo.MyInfoProvider
import sg.nedigital.myinfo.MyInfoProviderImpl
import sg.nedigital.myinfo.repositories.MyInfoRepository
import sg.nedigital.myinfo.repositories.MyInfoRepositoryImpl
import sg.nedigital.myinfo.storage.MyInfoStorage
import sg.nedigital.myinfo.storage.MyInfoStorageImpl

@Module
internal abstract class AppModule {

    @Binds
    @MyInfoScope
    abstract fun bindMyInfoStorage(myInfoStorage: MyInfoStorageImpl): MyInfoStorage

    @Binds
    @MyInfoScope
    abstract fun bindMyInfoProvider(authenticationService: MyInfoProviderImpl): MyInfoProvider

    @Binds
    @MyInfoScope
    abstract fun bindMyInfoRepository(repository: MyInfoRepositoryImpl): MyInfoRepository
}
