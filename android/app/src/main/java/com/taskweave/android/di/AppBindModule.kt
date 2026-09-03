package com.taskweave.android.di

import com.taskweave.android.sync.SyncScheduler
import com.taskweave.android.sync.WorkScheduler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppBindModule {

    @Binds
    @Singleton
    abstract fun bindWorkScheduler(impl: SyncScheduler): WorkScheduler
}
