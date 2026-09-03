package com.taskweave.android.ui.signin

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.taskweave.android.R
import com.taskweave.android.ui.components.BrutalButton
import com.taskweave.android.ui.components.MetaLabel
import com.taskweave.android.ui.theme.Brand

@Composable
fun SignInScreen(onSignIn: (Context) -> Unit) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brand.Cream)
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MetaLabel("The last-minute life saver")
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.sign_in_title),
            style = MaterialTheme.typography.displayLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.sign_in_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(40.dp))
        BrutalButton(
            text = stringResource(R.string.sign_in_button),
            onClick = { onSignIn(context) },
            filled = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
