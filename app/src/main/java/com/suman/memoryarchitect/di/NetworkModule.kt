package com.suman.memoryarchitect.di

import com.suman.memoryarchitect.BuildConfig
import com.suman.memoryarchitect.core.network.EmulatorBaseUrlResolver
import com.suman.memoryarchitect.core.network.SecurityConfig
import com.suman.memoryarchitect.data.remote.LevelApi
import com.suman.memoryarchitect.data.remote.ProgressionApi
import com.suman.memoryarchitect.data.remote.RemoteConfigApi
import com.suman.memoryarchitect.data.remote.ShopApi
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder().build()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        // Was 15s across the board - on an unreachable host (the exact "results screen stuck on
        // 'Saving your progress...'" complaint) that meant a full 15-second stall before falling
        // back to the pending-sync queue that already exists for this. connectTimeout specifically
        // governs that failure mode (a TCP connect that never completes) and is now short enough
        // that a genuinely unreachable server resolves in a few seconds, not fifteen; read/write
        // stay a little more generous since those only matter once a connection actually exists.
        val builder = OkHttpClient.Builder()
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)

        if (BuildConfig.DEBUG) {
            builder.addInterceptor(
                HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY },
            )
        }

        SecurityConfig.certificatePins.takeIf { it.isNotEmpty() }?.let { pins ->
            val pinnerBuilder = CertificatePinner.Builder()
            pins.forEach { pinnerBuilder.add(it.hostnamePattern, it.pin) }
            builder.certificatePinner(pinnerBuilder.build())
        }

        return builder.build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, moshi: Moshi): Retrofit =
        Retrofit.Builder()
            // Debug only - see EmulatorBaseUrlResolver's doc: the checked-in BASE_URL is tuned for
            // a physical device on the same Wi-Fi as the dev machine, which an emulator's default
            // NAT networking can never reach, regardless of the mock backend's own health.
            .baseUrl(if (BuildConfig.DEBUG) EmulatorBaseUrlResolver.resolve(BuildConfig.BASE_URL) else BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

    @Provides
    @Singleton
    fun provideRemoteConfigApi(retrofit: Retrofit): RemoteConfigApi =
        retrofit.create(RemoteConfigApi::class.java)

    @Provides
    @Singleton
    fun provideLevelApi(retrofit: Retrofit): LevelApi = retrofit.create(LevelApi::class.java)

    @Provides
    @Singleton
    fun provideProgressionApi(retrofit: Retrofit): ProgressionApi = retrofit.create(ProgressionApi::class.java)

    @Provides
    @Singleton
    fun provideShopApi(retrofit: Retrofit): ShopApi = retrofit.create(ShopApi::class.java)
}