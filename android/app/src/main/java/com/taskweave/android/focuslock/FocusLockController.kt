package com.taskweave.android.focuslock

import android.content.Context
import com.taskweave.android.data.repository.SessionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Decides when [FocusGuardService] should be running:
 *   blocklist non-empty  AND  (a focus session is active  OR  the user tapped
 *   "Block now")  AND  both special permissions are granted.
 * [start] is called once from the Application.
 */
@Singleton
class FocusLockController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionRepository: SessionRepository,
    private val blocklistRepository: BlocklistRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _manual = MutableStateFlow(false)
    val manualOn: StateFlow<Boolean> = _manual.asStateFlow()

    private var running = false

    fun start() {
        combine(
            sessionRepository.active,
            blocklistRepository.blockedPackages,
            blocklistRepository.blockDuringFocus,
            _manual,
        ) { session, packages, duringFocus, manual ->
            packages.isNotEmpty() &&
                (manual || (duringFocus && session != null)) &&
                FocusLockPermissions.allGranted(context)
        }
            .distinctUntilChanged()
            .onEach { shouldRun ->
                if (shouldRun && !running) {
                    running = true
                    FocusGuardService.start(context)
                } else if (!shouldRun && running) {
                    running = false
                    FocusGuardService.stop(context)
                }
            }
            .launchIn(scope)
    }

    fun setManual(on: Boolean) {
        _manual.value = on
    }
}
