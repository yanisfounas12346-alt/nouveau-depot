package com.yanis.objectif30.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.yanis.objectif30.MainActivity
import com.yanis.objectif30.data.PlanRepository
import java.time.LocalDate

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "prep_demain"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    channelId,
                    "Préparation du lendemain",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Affaires, séance et repas à préparer pour demain"
                }
            )
        }

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val tomorrow = LocalDate.now().plusDays(1)
        val plan = PlanRepository.day(tomorrow.dayOfWeek.value - 1)
        val prep = plan.prep.take(3).joinToString(" • ")

        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("🥊 Demain : ${plan.focus}")
            .setContentText(prep)
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Demain — ${plan.name}\n${plan.focus}\n\nÀ préparer : $prep\n\nRepas chaud : ${plan.meals[1].description}"
                )
            )
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()

        manager.notify(3019, notification)
    }
}
