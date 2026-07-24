package com.suman.memoryarchitect.di

import com.suman.memoryarchitect.core.common.DefaultDispatcherProvider
import com.suman.memoryarchitect.core.common.DispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object DispatcherModule {

    @Provides
    fun provideDispatcherProvider(): DispatcherProvider = DefaultDispatcherProvider()
}