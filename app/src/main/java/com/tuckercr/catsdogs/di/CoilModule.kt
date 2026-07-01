package com.tuckercr.catsdogs.di

import android.content.Context
import coil.ImageLoader
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
                val userAgent = "CatsDogsWeatherApp/1.0 (Android; OpenStreetMap tiles)"

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
