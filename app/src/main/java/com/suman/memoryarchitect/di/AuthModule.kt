package com.suman.memoryarchitect.di

import com.suman.memoryarchitect.core.auth.PlayerIdentityManager
import com.suman.memoryarchitect.core.auth.PlayerIdentityManagerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {

    @Binds
    @Singleton
    abstract fun bindPlayerIdentityManager(
        impl: PlayerIdentityManagerImpl,
    ): PlayerIdentityManager
}
