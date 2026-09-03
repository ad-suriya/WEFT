package com.taskweave.android.di

import android.content.Context
import androidx.room.Room
import com.taskweave.android.data.local.TaskWeaveDatabase
import com.taskweave.android.data.local.dao.PendingOpDao
import com.taskweave.android.data.local.dao.TaskDao
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
    fun provideDatabase(@ApplicationContext context: Context): TaskWeaveDatabase =
        Room.databaseBuilder(context, TaskWeaveDatabase::class.java, "taskweave.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideTaskDao(db: TaskWeaveDatabase): TaskDao = db.taskDao()

    @Provides fun providePendingOpDao(db: TaskWeaveDatabase): PendingOpDao = db.pendingOpDao()
}
