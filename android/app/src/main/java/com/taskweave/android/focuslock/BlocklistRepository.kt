package com.taskweave.android.focuslock

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.focusLockStore by preferencesDataStore("focus_lock")

/** Which apps to block, and when the guard should run. */
@Singleton
class BlocklistRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val KEY_PACKAGES = stringSetPreferencesKey("blocked_packages")
    private val KEY_DURING_FOCUS = booleanPreferencesKey("block_during_focus")

    val blockedPackages: Flow<Set<String>> =
        context.focusLockStore.data.map { it[KEY_PACKAGES] ?: emptySet() }

    /** Auto-arm the guard whenever a focus session is running (default on). */
    val blockDuringFocus: Flow<Boolean> =
        context.focusLockStore.data.map { it[KEY_DURING_FOCUS] ?: true }

    suspend fun setBlocked(pkg: String, blocked: Boolean) {
        context.focusLockStore.edit { prefs ->
            val current = prefs[KEY_PACKAGES] ?: emptySet()
            prefs[KEY_PACKAGES] = if (blocked) current + pkg else current - pkg
        }
    }

    suspend fun setBlockDuringFocus(enabled: Boolean) {
        context.focusLockStore.edit { it[KEY_DURING_FOCUS] = enabled }
    }
}
