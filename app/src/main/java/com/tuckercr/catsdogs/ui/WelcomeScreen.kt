package com.tuckercr.catsdogs.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tuckercr.catsdogs.R
import com.tuckercr.catsdogs.ui.pets.PetMood
import com.tuckercr.catsdogs.ui.pets.PetScene
import com.tuckercr.catsdogs.ui.pets.skyColor
import com.tuckercr.catsdogs.ui.theme.CatsDogsTheme

/** The first onboarding screen: introduces Sunny and Misty, who tell you the weather. */
@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PetScene(
            mood = PetMood.SUNNY,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(PetMood.SUNNY.skyColor)
                .padding(horizontal = 12.dp, vertical = 16.dp),
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.welcome_title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(modifier = Modifier.height(20.dp))
        Character(name = stringResource(R.string.welcome_sunny_name), body = stringResource(R.string.welcome_sunny_body))
        Spacer(modifier = Modifier.height(16.dp))
        Character(name = stringResource(R.string.welcome_misty_name), body = stringResource(R.string.welcome_misty_body))
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.welcome_together),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onGetStarted,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(R.string.get_started))
        }
    }
}

@Composable
private fun Character(
    name: String,
    body: String,
) {
    Column(
        modifier = Modifier.semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun WelcomeScreenPreview() {
    CatsDogsTheme {
        WelcomeScreen(onGetStarted = {})
    }
}
