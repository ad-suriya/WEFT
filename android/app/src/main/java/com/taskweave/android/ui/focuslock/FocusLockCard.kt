package com.taskweave.android.ui.focuslock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taskweave.android.focuslock.FocusLockPermissions

@Composable
fun FocusLockCard(
    onChooseApps: () -> Unit,
    viewModel: BlocklistViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val blocked by viewModel.blocked.collectAsStateWithLifecycle()
    val duringFocus by viewModel.blockDuringFocus.collectAsStateWithLifecycle()
    val manualOn by viewModel.manualOn.collectAsStateWithLifecycle()

    var hasUsage by remember { mutableStateOf(FocusLockPermissions.hasUsageAccess(context)) }
    var hasOverlay by remember { mutableStateOf(FocusLockPermissions.hasOverlay(context)) }
    LifecycleResumeEffect(Unit) {
        hasUsage = FocusLockPermissions.hasUsageAccess(context)
        hasOverlay = FocusLockPermissions.hasOverlay(context)
        onPauseOrDispose { }
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Shield, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text("Focus Lock", style = MaterialTheme.typography.titleMedium)
            }

            if (!hasUsage || !hasOverlay) {
                Text(
                    "Block distracting apps during a session. Needs two one-time permissions:",
                    style = MaterialTheme.typography.bodySmall,
                )
                if (!hasUsage) {
                    OutlinedButton(
                        onClick = { context.startActivity(FocusLockPermissions.usageAccessIntent()) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Grant usage access") }
                }
                if (!hasOverlay) {
                    OutlinedButton(
                        onClick = { context.startActivity(FocusLockPermissions.overlayIntent(context)) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Grant \"display over other apps\"") }
                }
            } else {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Block during focus sessions", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = duringFocus, onCheckedChange = viewModel::setBlockDuringFocus)
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Block now", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = manualOn, onCheckedChange = viewModel::setManual)
                }
            }

            TextButton(onClick = onChooseApps, modifier = Modifier.fillMaxWidth()) {
                Text(
                    if (blocked.isEmpty()) "Choose apps to block" else "Blocked apps: ${blocked.size}",
                    modifier = Modifier.weight(1f),
                )
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
            }
        }
    }
}
