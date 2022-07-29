package com.hms.quickline.data.di

import com.hms.quickline.domain.repository.LoginRepository
import com.huawei.agconnect.auth.AGConnectAuth
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideLoginRepository(agConnectAuth: AGConnectAuth) = LoginRepository(agConnectAuth)
}