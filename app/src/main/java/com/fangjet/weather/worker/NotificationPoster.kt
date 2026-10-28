package com.fangjet.weather.worker

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.fangjet.weather.MainActivity
import com.fangjet.weather.R
import com.fangjet.weather.WeatherApplication
import com.fangjet.weather.ui.pets.petSceneIcon

/** Whether the app may post notifications (always below Android 13, which has no permission for it). */
internal fun Context.canPostNotifications(): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

/**
 * Posts a [WeatherNotification] in the brand look: the accent blue, the pets in the notification's
 * mood, and the full text when expanded. Each kind has its own channel and id, so a walk nudge
 * never replaces the morning briefing.
 */
internal fun Context.postWeatherNotification(notification: WeatherNotification) {
    if (!canPostNotifications()) return
    val tapIntent = Intent(this, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val tapPendingIntent = PendingIntent.getActivity(
        this,
        0,
        tapIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    val (channel, id) = when (notification.kind) {
        NotificationKind.MORNING_BRIEFING -> WeatherApplication.CHANNEL_MORNING_BRIEFING to 1000
        NotificationKind.WALK_TIME -> WeatherApplication.CHANNEL_WALK_TIME to 1001
        NotificationKind.RAIN -> WeatherApplication.CHANNEL_RAIN to 1002
    }
    val iconSize = resources.getDimensionPixelSize(android.R.dimen.notification_large_icon_width)
    val built = NotificationCompat
        .Builder(this, channel)
        .setSmallIcon(R.drawable.ic_notification_weather)
        .setColor(ContextCompat.getColor(this, R.color.notification_accent))
        .setLargeIcon(petSceneIcon(notification.mood, iconSize, notification.southernHemisphere))
        .setContentTitle(notification.title)
        .setContentText(notification.body)
        .setStyle(NotificationCompat.BigTextStyle().bigText(notification.body))
        .setContentIntent(tapPendingIntent)
        .setAutoCancel(true)
        .build()
    @Suppress("MissingPermission") // checked by canPostNotifications above
    NotificationManagerCompat.from(this).notify(id, built)
}
