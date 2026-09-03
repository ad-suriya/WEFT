package com.taskweave.android.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.taskweave.android.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.StateFlow
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wraps Credential Manager + Sign in with Google. Always requests an **ID token**
 * with the existing **Web** OAuth client id as the server client id
 * ([BuildConfig.SERVER_CLIENT_ID]) — that is the audience the backend verifies.
 *
 * The Calendar scope is a separate authorization ([CalendarAuthorizer]); it is
 * requested right after the first successful sign-in.
 */
@Singleton
class AuthManager @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val tokenStore: TokenStore,
) {
    private val credentialManager = CredentialManager.create(appContext)

    val session: StateFlow<Session?> get() = tokenStore.session
    val isSignedIn: Boolean get() = tokenStore.idToken != null

    /** Interactive sign-in — pass an Activity context so the account picker can show. */
    suspend fun signIn(activityContext: Context): Result<Session> = runCatching {
        val option = GetSignInWithGoogleOption.Builder(BuildConfig.SERVER_CLIENT_ID)
            .setNonce(newNonce())
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val response = credentialManager.getCredential(activityContext, request)
        val session = response.toSession()
        tokenStore.update(session)
        session
    }

    /**
     * Silent re-auth used by [TokenAuthenticator] on a 401. Only succeeds if the
     * user previously authorized an account on this device.
     */
    suspend fun silentRefresh(context: Context = appContext): Result<String> = runCatching {
        val option = GetGoogleIdOption.Builder()
            .setServerClientId(BuildConfig.SERVER_CLIENT_ID)
            .setFilterByAuthorizedAccounts(true)
            .setAutoSelectEnabled(true)
            .setNonce(newNonce())
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val response = credentialManager.getCredential(context, request)
        val session = response.toSession()
        tokenStore.update(session)
        session.idToken
    }.recoverCatching { e ->
        if (e is NoCredentialException) tokenStore.clear()
        throw e
    }

    suspend fun signOut() {
        runCatching {
            credentialManager.clearCredentialState(
                androidx.credentials.ClearCredentialStateRequest(),
            )
        }
        tokenStore.clear()
    }

    private fun GetCredentialResponse.toSession(): Session {
        val cred = credential
        check(
            cred is CustomCredential &&
                cred.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL,
        ) { "Unexpected credential type: ${cred.type}" }
        val google = try {
            GoogleIdTokenCredential.createFrom(cred.data)
        } catch (e: GoogleIdTokenParsingException) {
            throw IllegalStateException("Malformed Google ID token", e)
        }
        return Session(
            idToken = google.idToken,
            email = google.id,
            displayName = google.displayName,
            photoUrl = google.profilePictureUri?.toString(),
        )
    }

    private fun newNonce(): String {
        val bytes = ByteArray(16).also { SecureRandom().nextBytes(it) }
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
