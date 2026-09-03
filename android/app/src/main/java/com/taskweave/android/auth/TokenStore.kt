package com.taskweave.android.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists the current Google ID token (and a little profile metadata for the UI)
 * in [EncryptedSharedPreferences]. The token is short-lived; [AuthManager] refreshes
 * it via silent sign-in when the API returns 401.
 */
@Singleton
class TokenStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "taskweave_secure_prefs",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    private val _session = MutableStateFlow(read())
    val session: StateFlow<Session?> = _session

    val idToken: String? get() = _session.value?.idToken

    fun update(session: Session) {
        prefs.edit()
            .putString(KEY_TOKEN, session.idToken)
            .putString(KEY_EMAIL, session.email)
            .putString(KEY_NAME, session.displayName)
            .putString(KEY_PHOTO, session.photoUrl)
            .apply()
        _session.value = session
    }

    fun updateToken(idToken: String) {
        val current = _session.value ?: Session(idToken, null, null, null)
        update(current.copy(idToken = idToken))
    }

    fun clear() {
        prefs.edit().clear().apply()
        _session.value = null
    }

    private fun read(): Session? {
        val token = prefs.getString(KEY_TOKEN, null) ?: return null
        return Session(
            idToken = token,
            email = prefs.getString(KEY_EMAIL, null),
            displayName = prefs.getString(KEY_NAME, null),
            photoUrl = prefs.getString(KEY_PHOTO, null),
        )
    }

    private companion object {
        const val KEY_TOKEN = "id_token"
        const val KEY_EMAIL = "email"
        const val KEY_NAME = "name"
        const val KEY_PHOTO = "photo"
    }
}

data class Session(
    val idToken: String,
    val email: String?,
    val displayName: String?,
    val photoUrl: String?,
)
