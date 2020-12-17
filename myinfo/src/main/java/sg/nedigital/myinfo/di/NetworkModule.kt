package sg.nedigital.myinfo.di

import android.app.Application
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import java.util.concurrent.TimeUnit
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import sg.nedigital.myinfo.MyInfoConfiguration
import sg.nedigital.myinfo.services.MyInfoService
import sg.nedigital.myinfo.storage.MyInfoStorage
import sg.nedigital.myinfo.util.AuthStateManager
import sg.nedigital.myinfo.util.JWTDecoder

@Module
class NetworkModule {

    private companion object {
        const val CONNECT_TIMEOUT = 10L
        const val WRITE_TIMEOUT = 10L
        const val READ_TIMEOUT = 10L
        const val CACHE_SIZE = 10 * 1024 * 1024 // 10MB
    }

    @Provides
    @MyInfoScope
    fun providesOkHttpClient(
        cache: Cache,
        configuration: MyInfoConfiguration
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .cache(cache)
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
            .addInterceptor(
                HttpLoggingInterceptor().setLevel(
                    if (configuration.debugLog) {
                        HttpLoggingInterceptor.Level.BODY
                    } else {
                        HttpLoggingInterceptor.Level.NONE
                    }
                )
            )
            .build()
    }

    @Provides
    @MyInfoScope
    fun providesOkhttpCache(context: Application): Cache =
        Cache(context.cacheDir, CACHE_SIZE.toLong())

    @Provides
    @MyInfoScope
    fun providesGson(): Gson = GsonBuilder()
        .create()

    @Provides
    @MyInfoScope
    fun provideAccountRetrofit(
        okHttpClient: OkHttpClient,
        configuration: MyInfoConfiguration,
        gson: Gson
    ): Retrofit = Retrofit.Builder()
        .baseUrl(configuration.host)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    @Provides
    @MyInfoScope
    fun provideService(retrofit: Retrofit): MyInfoService =
        retrofit.create(MyInfoService::class.java)

    @Provides
    @MyInfoScope
    fun provideJWTDecoder(): JWTDecoder = JWTDecoder()

    @Provides
    @MyInfoScope
    fun provideAuthStateManager(context: Application, storage: MyInfoStorage): AuthStateManager {
        return AuthStateManager(context.applicationContext, storage)
    }

    @Provides
    @MyInfoScope
    fun provideMyInfoConfiguration(
        context: Application,
        storage: MyInfoStorage
    ): MyInfoConfiguration {
        return MyInfoConfiguration(context.applicationContext, storage)
    }
}
