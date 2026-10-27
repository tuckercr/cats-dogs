package com.fangjet.weather.di

import android.content.Context
import coil.ImageLoader
import com.fangjet.weather.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CoilModule {

    @Singleton
    @Provides
    fun provideImageLoader(
        @ApplicationContext context: Context,
    ): ImageLoader {
        val okHttpClient = OkHttpClient
            .Builder()
            .addNetworkInterceptor { chain ->
                val originalRequest = chain.request()
                // OpenStreetMap's tile policy asks for an app name and a way to reach us.
                val userAgent = "SunnyAndMisty/${BuildConfig.VERSION_NAME} (Android; +https://sunnyandmisty.com)"

                val newRequest = originalRequest
                    .newBuilder()
                    .header("User-Agent", userAgent)
                    .build()

                chain.proceed(newRequest)
            }.build()

        return ImageLoader
            .Builder(context)
            .okHttpClient(okHttpClient)
            .build()
    }
}
