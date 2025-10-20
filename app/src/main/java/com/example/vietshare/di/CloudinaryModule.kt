package com.example.vietshare.di

import android.content.Context
import com.cloudinary.android.MediaManager
import com.example.vietshare.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CloudinaryModule {

    @Provides
    @Singleton
    fun provideMediaManager(@ApplicationContext context: Context): MediaManager {
        val config = mapOf(
            "cloud_name" to BuildConfig.CLOUDINARY_CLOUD_NAME,
            "api_key" to BuildConfig.CLOUDINARY_API_KEY,
            "api_secret" to BuildConfig.CLOUDINARY_API_SECRET
        )
        // First, initialize the singleton instance
        MediaManager.init(context, config)
        // Then, retrieve and return the initialized instance
        return MediaManager.get()
    }
}
