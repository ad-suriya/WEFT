package com.taskweave.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.taskweave.android.ui.theme.Brand

/** Hard offset shadow (no blur) + 1px ink border — the shell's card treatment. */
fun Modifier.hardShadow(
    offset: Dp = 5.dp,
    shadowColor: Color = Brand.Ink,
    background: Color = Brand.Surface,
    borderColor: Color = Brand.Ink,
): Modifier = this
    .drawBehind {
        val o = offset.toPx()
        drawRect(color = shadowColor, topLeft = Offset(o, o), size = size)
    }
    .background(background)
    .border(1.dp, borderColor, RectangleShape)

@Composable
fun BrutalCard(
    modifier: Modifier = Modifier,
    accent: Color? = null,
    shadowColor: Color = Brand.Ink,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .hardShadow(shadowColor = shadowColor)
            .then(if (accent != null) Modifier.drawLeftRule(accent) else Modifier)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
        content = content,
    )
}

private fun Modifier.drawLeftRule(color: Color): Modifier = this.drawBehind {
    drawRect(color = color, size = size.copy(width = 4.dp.toPx()))
}

/** Tiny uppercase label + hairline divider — the "section header" pattern. */
@Composable
fun SectionHeader(label: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = Brand.Ink,
        )
        Spacer(Modifier.width(10.dp))
        Box(
            Modifier
                .weight(1f)
                .height(1.dp)
                .background(Brand.Ink.copy(alpha = 0.2f)),
        )
    }
}

/** Solid-fill badge: uppercase, wide tracking, square. */
@Composable
fun BrutalBadge(
    text: String,
    color: Color = Brand.Ink,
    contentColor: Color = Brand.White,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = contentColor,
        modifier = modifier
            .background(color)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

/** Outlined chip (ink border, transparent) for metadata. */
@Composable
fun BrutalChip(text: String, modifier: Modifier = Modifier, color: Color = Brand.Ink) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = modifier
            .border(1.dp, color, RectangleShape)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

/** Neo-brutalist button: sans, tiny, uppercase, wide tracking, square, ink border. */
@Composable
fun BrutalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    filled: Boolean = false,
    color: Color = Brand.Ink,
) {
    val bg = if (filled) color else Color.Transparent
    val fg = if (filled) Brand.White else color
    Box(
        modifier = modifier
            .border(1.dp, if (enabled) color else Brand.Gray, RectangleShape)
            .background(if (enabled) bg else Color.Transparent)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = if (enabled) fg else Brand.Gray,
            textAlign = TextAlign.Center,
        )
    }
}

/** Tiny uppercase caption in muted gray. */
@Composable
fun MetaLabel(text: String, modifier: Modifier = Modifier, color: Color = Brand.Gray) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = modifier,
    )
}
