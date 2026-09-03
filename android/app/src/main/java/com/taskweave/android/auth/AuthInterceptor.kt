package com.taskweave.android.auth

import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Singleton

/** Adds `Authorization: Bearer <ID token>` to every request that doesn't already have one. */
@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenStore: TokenStore,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.header("Authorization") != null) return chain.proceed(request)
        val token = tokenStore.idToken ?: return chain.proceed(request)
        return chain.proceed(request.withBearer(token))
    }
}

/**
 * On a 401, performs a single silent re-auth and retries the request with the
 * fresh token. If refresh fails, gives up (the session flow flips to signed-out).
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenStore: TokenStore,
    private val authManagerProvider: dagger.Lazy<AuthManager>,
) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= 2) return null
        val failedToken = response.request.header("Authorization")?.removePrefix("Bearer ")

        synchronized(this) {
            val current = tokenStore.idToken
            if (current != null && current != failedToken) {
                // Another thread already refreshed — retry with the newer token.
                return response.request.withBearer(current)
            }
            val refreshed = runBlocking { authManagerProvider.get().silentRefresh() }
                .getOrNull() ?: return null
            return response.request.withBearer(refreshed)
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}

private fun Request.withBearer(token: String): Request =
    newBuilder().header("Authorization", "Bearer $token").build()
