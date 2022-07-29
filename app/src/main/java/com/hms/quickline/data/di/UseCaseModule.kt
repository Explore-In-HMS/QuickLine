package com.hms.quickline.data.di

import android.content.Context
import com.hms.quickline.domain.repository.LoginRepository
import com.hms.quickline.domain.usecase.LoginUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    @Singleton
    fun provideLoginUseCase(@ApplicationContext applicationContext: Context,
                            loginRepository: LoginRepository
    ) = LoginUseCase(applicationContext, loginRepository)
}