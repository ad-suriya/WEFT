package com.taskweave.android.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.taskweave.android.data.model.Mode
import com.taskweave.android.ui.theme.Brand
import com.taskweave.android.ui.theme.color

@Composable
fun ModeBadge(mode: Mode, modifier: Modifier = Modifier) {
    BrutalBadge(
        text = "${mode.label} mode",
        color = mode.color(),
        contentColor = Brand.White,
        modifier = modifier,
    )
}
