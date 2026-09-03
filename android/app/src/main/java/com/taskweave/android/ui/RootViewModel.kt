package com.taskweave.android.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskweave.android.auth.AuthManager
import com.taskweave.android.auth.CalendarAuthorizer
import com.taskweave.android.auth.Session
import com.taskweave.android.fcm.FcmRegistrar
import com.taskweave.android.sync.SyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface RootUiState {
    data object Loading : RootUiState
    data object SignedOut : RootUiState
    data class SignedIn(val session: Session) : RootUiState
}

@HiltViewModel
class RootViewModel @Inject constructor(
    private val authManager: AuthManager,
    private val fcmRegistrar: FcmRegistrar,
    private val calendarAuthorizer: CalendarAuthorizer,
    private val syncScheduler: SyncScheduler,
) : ViewModel() {

    private val signingIn = MutableStateFlow(false)
    val errorMessage = MutableStateFlow<String?>(null)

    val state: StateFlow<RootUiState> = combine(authManager.session, signingIn) { session, busy ->
        when {
            session != null -> RootUiState.SignedIn(session)
            busy -> RootUiState.Loading
            else -> RootUiState.SignedOut
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RootUiState.Loading)

    /** Consumed by the UI to launch the Calendar-consent IntentSender when needed. */
    val calendarConsent = MutableStateFlow<android.content.IntentSender?>(null)

    fun signIn(activityContext: Context) {
        if (signingIn.value) return
        signingIn.value = true
        viewModelScope.launch {
            authManager.signIn(activityContext)
                .onSuccess { onSignedIn() }
                .onFailure { errorMessage.value = it.message ?: "Sign-in failed" }
            signingIn.value = false
        }
    }

    private suspend fun onSignedIn() {
        syncScheduler.syncNow()
        fcmRegistrar.registerCurrentToken()
        runCatching { calendarAuthorizer.ensureAuthorized() }
            .getOrNull()
            ?.let { calendarConsent.value = it }
    }

    fun onCalendarConsentHandled() {
        calendarConsent.value = null
    }

    fun signOut() {
        viewModelScope.launch {
            fcmRegistrar.unregisterAndDelete()
            authManager.signOut()
        }
    }

    fun consumeError() {
        errorMessage.value = null
    }
}
