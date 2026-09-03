package com.taskweave.android.ui.focuslock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskweave.android.focuslock.BlocklistRepository
import com.taskweave.android.focuslock.FocusLockController
import com.taskweave.android.focuslock.InstalledApps
import com.taskweave.android.focuslock.LaunchableApp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BlocklistViewModel @Inject constructor(
    private val installedApps: InstalledApps,
    private val blocklistRepository: BlocklistRepository,
    private val focusLockController: FocusLockController,
) : ViewModel() {

    val apps = MutableStateFlow<List<LaunchableApp>>(emptyList())
    val loading = MutableStateFlow(true)

    val blocked: StateFlow<Set<String>> = blocklistRepository.blockedPackages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val blockDuringFocus: StateFlow<Boolean> = blocklistRepository.blockDuringFocus
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val manualOn: StateFlow<Boolean> = focusLockController.manualOn

    init {
        viewModelScope.launch {
            apps.value = installedApps.launchableApps()
            loading.value = false
        }
    }

    fun toggle(pkg: String, on: Boolean) {
        viewModelScope.launch { blocklistRepository.setBlocked(pkg, on) }
    }

    fun setBlockDuringFocus(on: Boolean) {
        viewModelScope.launch { blocklistRepository.setBlockDuringFocus(on) }
    }

    fun setManual(on: Boolean) = focusLockController.setManual(on)
}
