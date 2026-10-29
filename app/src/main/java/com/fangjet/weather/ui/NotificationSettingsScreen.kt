package com.fangjet.weather.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fangjet.weather.R
import com.fangjet.weather.model.SettingsViewModel
import com.fangjet.weather.worker.NotificationKind
import com.fangjet.weather.worker.canPostNotifications

/**
 * A switch for each notification, under Settings. Walk time only shows with a dog ("I have a dog"
 * in Settings), any kind is hidden while Remote Config has it off, and hidden switches keep their
 * values.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsRoute(onNavigateBack: () -> Unit) {
    val viewModel = hiltViewModel<SettingsViewModel>()
    val hasDog by viewModel.showWalkCard.collectAsStateWithLifecycle()
    val notifyMorningBriefing by viewModel.notifyMorningBriefing.collectAsStateWithLifecycle()
    val notifyWalkTime by viewModel.notifyWalkTime.collectAsStateWithLifecycle()
    val notifyRain by viewModel.notifyRain.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val granted = rememberGrantedOnResume { context.canPostNotifications() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_notifications)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_navigate_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            if (!granted) {
                SettingsLinkRow(
                    label = stringResource(R.string.settings_notifications_disabled),
                    onClick = context::openAppDetailsSettings,
                )
                return@Column
            }
            // Each shows unless Remote Config has switched it off for everyone; Walk time also needs a dog.
            if (viewModel.remotelyEnabled(NotificationKind.MORNING_BRIEFING)) {
                SettingsSwitchRow(
                    label = stringResource(R.string.settings_notify_briefing),
                    summary = stringResource(
                        if (hasDog) R.string.settings_notify_briefing_summary_dog else R.string.settings_notify_briefing_summary,
                    ),
                    checked = notifyMorningBriefing,
                    onCheckedChange = viewModel::setNotifyMorningBriefing,
                )
            }
            if (hasDog && viewModel.remotelyEnabled(NotificationKind.WALK_TIME)) {
                SettingsSwitchRow(
                    label = stringResource(R.string.settings_notify_walk),
                    summary = stringResource(R.string.settings_notify_walk_summary),
                    checked = notifyWalkTime,
                    onCheckedChange = viewModel::setNotifyWalkTime,
                )
            }
            if (viewModel.remotelyEnabled(NotificationKind.RAIN)) {
                SettingsSwitchRow(
                    label = stringResource(R.string.settings_notify_rain),
                    summary = stringResource(R.string.settings_notify_rain_summary),
                    checked = notifyRain,
                    onCheckedChange = viewModel::setNotifyRain,
                )
            }
        }
    }
}
