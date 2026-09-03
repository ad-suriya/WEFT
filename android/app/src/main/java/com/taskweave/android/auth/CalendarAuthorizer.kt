package com.taskweave.android.auth

import android.content.Context
import android.content.IntentSender
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.taskweave.android.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Google's Credential Manager ID-token flow does not itself grant API scopes, so
 * the Calendar scope is requested separately with the Authorization API. We ask
 * for offline access (a server auth code) so the backend can exchange it for a
 * refresh token and keep two-way Calendar sync running without the app present.
 *
 * MVP: the consent is triggered right after first sign-in. Wiring the returned
 * [AuthorizationResult.getServerAuthCode] to a backend endpoint is a one-liner
 * once that endpoint exists — see README "Calendar scope".
 */
@Singleton
class CalendarAuthorizer @Inject constructor(
    @ApplicationContext private val appContext: Context,
) {
    private val client = Identity.getAuthorizationClient(appContext)

    private val request: AuthorizationRequest by lazy {
        AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(BuildConfig.CALENDAR_SCOPE)))
            .requestOfflineAccess(BuildConfig.SERVER_CLIENT_ID, /* forceCodeForRefreshToken = */ true)
            .build()
    }

    /**
     * @return null if the scope is already granted (no UI needed), otherwise an
     *   [IntentSender] the caller must launch; feed the result back to [onConsentResult].
     */
    suspend fun ensureAuthorized(): IntentSender? {
        val result = client.authorize(request).await()
        return if (result.hasResolution()) result.pendingIntent?.intentSender else null
    }

    fun parseResult(data: android.content.Intent?): AuthorizationResult? =
        runCatching { client.getAuthorizationResultFromIntent(data) }.getOrNull()
}
