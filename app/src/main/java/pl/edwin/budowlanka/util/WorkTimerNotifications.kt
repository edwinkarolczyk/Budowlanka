package pl.edwin.budowlanka.util

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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import pl.edwin.budowlanka.MainActivity
import pl.edwin.budowlanka.R
import pl.edwin.budowlanka.data.AppDatabase
import pl.edwin.budowlanka.data.WorkEndReason
import pl.edwin.budowlanka.data.WorkSessionEventEntity
import pl.edwin.budowlanka.data.WorkSessionEventType
import pl.edwin.budowlanka.data.WorkTimeType

object WorkTimerNotifications {
    const val CHANNEL_ID = "work_timer"
    const val ACTION_PAUSE = "pl.edwin.budowlanka.WORK_PAUSE"
    const val ACTION_STOP = "pl.edwin.budowlanka.WORK_STOP"
    const val EXTRA_ESTIMATE_ID = "estimate_id"
    private const val BASE_NOTIFICATION_ID = 3200

    private fun notificationId(estimateId: Long): Int =
        BASE_NOTIFICATION_ID + (estimateId % 100000).toInt()

    private fun channel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Czas pracy",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Aktywny licznik czasu pracy na zleceniu"
                    setShowBadge(false)
                }
            )
        }
    }

    fun show(
        context: Context,
        estimateId: Long,
        estimateTitle: String,
        startAt: Long,
        crewCount: Int,
        workType: String
    ) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        channel(context)
        val manager = context.getSystemService(NotificationManager::class.java)

        val openIntent = Intent(context, MainActivity::class.java)
        val openPending = PendingIntent.getActivity(
            context,
            notificationId(estimateId),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        fun actionIntent(action: String, requestOffset: Int): PendingIntent {
            val intent = Intent(context, WorkTimerActionReceiver::class.java).apply {
                this.action = action
                putExtra(EXTRA_ESTIMATE_ID, estimateId)
            }
            return PendingIntent.getBroadcast(
                context,
                notificationId(estimateId) + requestOffset,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_budowlanka)
            .setContentTitle("BUDOWLANKA • $estimateTitle")
            .setContentText("$crewCount osoba/osób • ${WorkTimeType.label(workType)}")
            .setContentIntent(openPending)
            .setWhen(startAt)
            .setUsesChronometer(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(0, "PAUZA", actionIntent(ACTION_PAUSE, 1))
            .addAction(0, "STOP", actionIntent(ACTION_STOP, 2))
            .build()

        manager.notify(notificationId(estimateId), notification)
    }

    fun cancel(context: Context, estimateId: Long) {
        context.getSystemService(NotificationManager::class.java)
            .cancel(notificationId(estimateId))
    }
}

class WorkTimerActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val estimateId = intent.getLongExtra(WorkTimerNotifications.EXTRA_ESTIMATE_ID, -1L)
        if (estimateId <= 0L) return

        val reason = when (intent.action) {
            WorkTimerNotifications.ACTION_PAUSE -> WorkEndReason.PAUSE
            WorkTimerNotifications.ACTION_STOP -> WorkEndReason.STOP
            else -> return
        }
        val eventType = if (reason == WorkEndReason.PAUSE) {
            WorkSessionEventType.PAUSE
        } else {
            WorkSessionEventType.STOP
        }

        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val dao = AppDatabase.get(context.applicationContext).dao()
                val active = dao.activeWorkSessions(estimateId)
                if (active.isNotEmpty()) {
                    val now = System.currentTimeMillis()
                    dao.stopActiveWorkSessions(estimateId, now, reason)
                    active.forEach { session ->
                        dao.upsertWorkSessionEvent(
                            WorkSessionEventEntity(
                                estimateId = estimateId,
                                sessionId = session.id,
                                crewMemberId = session.crewMemberId,
                                eventType = eventType,
                                at = now,
                                note = WorkTimeType.label(session.type)
                            )
                        )
                    }
                }
                WorkTimerNotifications.cancel(context, estimateId)
            } finally {
                pending.finish()
            }
        }
    }
}

class WorkTimerBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val dao = AppDatabase.get(context.applicationContext).dao()
                dao.allActiveWorkSessions()
                    .groupBy { it.estimateId }
                    .forEach { (estimateId, sessions) ->
                        val estimate = dao.getEstimate(estimateId) ?: return@forEach
                        WorkTimerNotifications.show(
                            context = context,
                            estimateId = estimateId,
                            estimateTitle = estimate.title,
                            startAt = sessions.minOf { it.startAt },
                            crewCount = sessions.size,
                            workType = sessions.first().type
                        )
                    }
            } finally {
                pending.finish()
            }
        }
    }
}
