package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.diagnostics.AppLogger
import com.example.diagnostics.LogTag

object MediaNotificationHelper {
    private const val CHANNEL_ID = "lyricecho_spotify_channel"
    private const val NOTIFICATION_ID = 2024

    fun showSpotifySyncNotification(context: Context, track: String, artist: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Spotify Live Lyrics Interceptor",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Shows real-time bilingual lyrics when songs play on Spotify"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("OPEN_FLOATING_POPUP", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("🎵 $track")
            .setContentText("$artist - متن و ترجمه دو زبانه آماده شد (کلیک برای باز شدن)")
            .setStyle(NotificationCompat.BigTextStyle().bigText("آهنگ \"$track\" اثر $artist در اسپاتیفای در حال پخش است. متن و ترجمه هم‌زمان فارسی آماده است."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            notificationManager.notify(NOTIFICATION_ID, notification)
            AppLogger.d(
                LogTag.NOTIFICATION_LISTENER,
                "اعلان هدزآپ پاپ‌آپ اسپاتیفای ارسال شد: \"$track\"",
                "Channel: $CHANNEL_ID | NotificationID: $NOTIFICATION_ID"
            )
        } catch (e: Exception) {
            AppLogger.w(
                LogTag.NOTIFICATION_LISTENER,
                "عدم توانایی در ارسال اعلان هدزآپ (نیاز به POST_NOTIFICATIONS): ${e.message}"
            )
        }
    }
}
