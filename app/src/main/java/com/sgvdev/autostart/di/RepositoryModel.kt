package com.sgvdev.autostart.di

import com.sgvdev.autostart.VlifteApi
import com.sgvdev.autostart.data.remote.AdRemoteDataSource
import com.sgvdev.autostart.data.remote.AdRemoteDataSourceImpl
import com.sgvdev.autostart.data.remote.AdRepository
import com.sgvdev.autostart.data.remote.AdRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class RepositoryModel {

    @Singleton
    @Provides
    fun provideAdDataSource(vlifteApi: VlifteApi): AdRemoteDataSource =
        AdRemoteDataSourceImpl(vlifteApi)

    @Singleton
    @Provides
    fun provideAdRepository(adRemoteDataSource: AdRemoteDataSource): AdRepository =
        AdRepositoryImpl(adRemoteDataSource)
}