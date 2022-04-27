package com.hms.quickline.data.di

import android.content.Context
import com.hms.quickline.presentation.call.webrtc.CloudDbWrapper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideCloudDBWrapper(@ApplicationContext context: Context): CloudDbWrapper = CloudDbWrapper()

}