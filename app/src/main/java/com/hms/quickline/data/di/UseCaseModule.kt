package com.hms.quickline.data.di

import com.hms.quickline.data.repository.HomeRepository
import com.hms.quickline.domain.usecase.HomeUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    @Singleton
    fun provideHomeUseCase(homeRepository: HomeRepository) = HomeUseCase(homeRepository)

}