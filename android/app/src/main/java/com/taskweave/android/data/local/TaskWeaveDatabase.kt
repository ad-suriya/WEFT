package com.taskweave.android.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.taskweave.android.data.local.dao.PendingOpDao
import com.taskweave.android.data.local.dao.TaskDao
import com.taskweave.android.data.local.entity.PendingOpEntity
import com.taskweave.android.data.local.entity.TaskEntity

@Database(
    entities = [TaskEntity::class, PendingOpEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class TaskWeaveDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun pendingOpDao(): PendingOpDao
}
