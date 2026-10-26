package com.tuckercr.catsdogs.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

/** Who lives with the user. Only a dog makes the "Best walk time" card worth showing. */
enum class Household(
    val labelRes: Int,
    val hasDog: Boolean,
) {
    DOG(R.string.household_dog, hasDog = true),
    CAT(R.string.household_cat, hasDog = false),
    BOTH(R.string.household_both, hasDog = true),
    NEITHER(R.string.household_neither, hasDog = false),
}

/**
 * Onboarding step after the welcome screen. The answer sets the "Best walk time" switch in
 * Settings, so a home without a dog never sees the walk card.
 */
@Composable
fun OnboardingHouseholdScreen(
    onChosen: (Household) -> Unit,
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
            mood = PetMood.CLOUDY,
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .background(PetMood.CLOUDY.skyColor, RoundedCornerShape(24.dp))
                .padding(horizontal = 12.dp, vertical = 16.dp),
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.household_title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.household_body),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(28.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Household.entries.forEach { household ->
                OutlinedButton(
                    onClick = { onChosen(household) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp),
                ) {
                    Text(
                        text = stringResource(household.labelRes),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OnboardingHouseholdScreenPreview() {
    CatsDogsTheme {
        OnboardingHouseholdScreen(onChosen = {})
    }
}
