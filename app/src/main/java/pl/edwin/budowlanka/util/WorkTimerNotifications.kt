package pl.edwin.budowlanka.util

import android.Manifest
import android.app.AlarmManager
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
import pl.edwin.budowlanka.data.AppDao
import pl.edwin.budowlanka.data.AppDatabase
import pl.edwin.budowlanka.data.WorkEndReason
import pl.edwin.budowlanka.data.WorkTimeType

object WorkTimerNotifications {
    const val CHANNEL_ID = "work_timer"
    const val ACTION_PAUSE = "pl.edwin.budowlanka.WORK_PAUSE"
    const val ACTION_STOP = "pl.edwin.budowlanka.WORK_STOP"
    const val ACTION_LONG_SESSION = "pl.edwin.budowlanka.WORK_LONG_SESSION"
    const val EXTRA_ESTIMATE_ID = "estimate_id"
    const val EXTRA_START_AT = "start_at"

    private const val REMINDER_CHANNEL_ID = "work_timer_reminder"
    private const val BASE_NOTIFICATION_ID = 3200
    private const val REMINDER_NOTIFICATION_OFFSET = 200_000
    private const val REMINDER_REQUEST_OFFSET = 400_000
    private const val LONG_SESSION_AFTER_MS = 10L * 60L * 60L * 1000L
    private const val REMINDER_PREFS = "work_timer_reminders"

    private fun notificationId(estimateId: Long): Int =
        BASE_NOTIFICATION_ID + (estimateId % 100000).toInt()

    private fun reminderNotificationId(estimateId: Long): Int =
        notificationId(estimateId) + REMINDER_NOTIFICATION_OFFSET

    private fun reminderRequestCode(estimateId: Long): Int =
        notificationId(estimateId) + REMINDER_REQUEST_OFFSET

    private fun timerChannel(context: Context) {
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

    private fun reminderChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(
                    REMINDER_CHANNEL_ID,
                    "Przypomnienia o czasie pracy",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Ostrzeżenie o długo działającym liczniku czasu"
                    setShowBadge(true)
                }
            )
        }
    }

    private fun actionPendingIntent(
        context: Context,
        estimateId: Long,
        action: String,
        requestOffset: Int
    ): PendingIntent {
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

    private fun reminderAlarmIntent(
        context: Context,
        estimateId: Long,
        startAt: Long,
        flags: Int
    ): PendingIntent? {
        val intent = Intent(context, WorkTimerActionReceiver::class.java).apply {
            action = ACTION_LONG_SESSION
            putExtra(EXTRA_ESTIMATE_ID, estimateId)
            putExtra(EXTRA_START_AT, startAt)
        }
        return PendingIntent.getBroadcast(
            context,
            reminderRequestCode(estimateId),
            intent,
            flags or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun warnedStartAt(context: Context, estimateId: Long): Long =
        context.getSharedPreferences(REMINDER_PREFS, Context.MODE_PRIVATE)
            .getLong("warned_$estimateId", -1L)

    private fun markWarned(context: Context, estimateId: Long, startAt: Long) {
        context.getSharedPreferences(REMINDER_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putLong("warned_$estimateId", startAt)
            .apply()
    }

    private fun scheduleLongSessionReminder(
        context: Context,
        estimateId: Long,
        startAt: Long
    ) {
        if (warnedStartAt(context, estimateId) == startAt) return

        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val pending = reminderAlarmIntent(
            context = context,
            estimateId = estimateId,
            startAt = startAt,
            flags = PendingIntent.FLAG_UPDATE_CURRENT
        ) ?: return

        val now = System.currentTimeMillis()
        val triggerAt = (startAt + LONG_SESSION_AFTER_MS).coerceAtLeast(now + 1_000L)
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAt,
            pending
        )
    }

    private fun cancelLongSessionReminder(context: Context, estimateId: Long) {
        val pending = reminderAlarmIntent(
            context = context,
            estimateId = estimateId,
            startAt = 0L,
            flags = PendingIntent.FLAG_NO_CREATE
        )
        if (pending != null) {
            context.getSystemService(AlarmManager::class.java).cancel(pending)
            pending.cancel()
        }
        context.getSystemService(NotificationManager::class.java)
            .cancel(reminderNotificationId(estimateId))
    }

    fun show(
        context: Context,
        estimateId: Long,
        estimateTitle: String,
        startAt: Long,
        crewCount: Int,
        workType: String
    ) {
        // Alarm jest niezależny od życia procesu aplikacji.
        scheduleLongSessionReminder(context, estimateId, startAt)

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        timerChannel(context)
        val manager = context.getSystemService(NotificationManager::class.java)

        val openIntent = Intent(context, MainActivity::class.java)
        val openPending = PendingIntent.getActivity(
            context,
            notificationId(estimateId),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

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
            .addAction(0, "PAUZA", actionPendingIntent(context, estimateId, ACTION_PAUSE, 1))
            .addAction(0, "STOP", actionPendingIntent(context, estimateId, ACTION_STOP, 2))
            .build()

        manager.notify(notificationId(estimateId), notification)
    }

    private fun showLongSessionWarning(
        context: Context,
        estimateId: Long,
        estimateTitle: String,
        startAt: Long
    ) {
        if (warnedStartAt(context, estimateId) == startAt) return

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        reminderChannel(context)
        markWarned(context, estimateId, startAt)

        val openPending = PendingIntent.getActivity(
            context,
            reminderNotificationId(estimateId),
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_budowlanka)
            .setContentTitle("Licznik działa już 10 godzin")
            .setContentText("$estimateTitle • sprawdź, czy nadal powinien działać.")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Zlecenie: $estimateTitle. Licznik czasu działa od ponad 10 godzin. " +
                        "Sprawdź, czy nie został pozostawiony przypadkowo."
                )
            )
            .setContentIntent(openPending)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .addAction(0, "PAUZA", actionPendingIntent(context, estimateId, ACTION_PAUSE, 1))
            .addAction(0, "STOP", actionPendingIntent(context, estimateId, ACTION_STOP, 2))
            .build()

        context.getSystemService(NotificationManager::class.java)
            .notify(reminderNotificationId(estimateId), notification)
    }

    suspend fun handleLongSessionReminder(
        context: Context,
        dao: AppDao,
        estimateId: Long,
        expectedStartAt: Long
    ) {
        val active = dao.activeWorkSessions(estimateId)
        if (active.isEmpty()) {
            cancelLongSessionReminder(context, estimateId)
            return
        }

        val actualStartAt = active.minOf { it.startAt }
        if (expectedStartAt > 0L && actualStartAt != expectedStartAt) {
            // Stary alarm z poprzedniej sesji tego samego zlecenia.
            scheduleLongSessionReminder(context, estimateId, actualStartAt)
            return
        }

        val elapsed = System.currentTimeMillis() - actualStartAt
        if (elapsed < LONG_SESSION_AFTER_MS) {
            // Zmiana zegara systemowego lub wcześniejsze wybudzenie alarmu.
            scheduleLongSessionReminder(context, estimateId, actualStartAt)
            return
        }

        val estimate = dao.getEstimate(estimateId) ?: run {
            cancelLongSessionReminder(context, estimateId)
            return
        }

        showLongSessionWarning(
            context = context,
            estimateId = estimateId,
            estimateTitle = estimate.title,
            startAt = actualStartAt
        )
    }

    fun cancel(context: Context, estimateId: Long) {
        context.getSystemService(NotificationManager::class.java)
            .cancel(notificationId(estimateId))
        cancelLongSessionReminder(context, estimateId)
    }

    suspend fun restoreActive(context: Context, dao: AppDao) {
        dao.allActiveWorkSessions()
            .groupBy { it.estimateId }
            .forEach { (estimateId, sessions) ->
                val estimate = dao.getEstimate(estimateId) ?: run {
                    cancel(context, estimateId)
                    return@forEach
                }

                show(
                    context = context,
                    estimateId = estimateId,
                    estimateTitle = estimate.title,
                    startAt = sessions.minOf { it.startAt },
                    crewCount = sessions.size,
                    workType = sessions.first().type
                )
            }
    }
}

class WorkTimerActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val estimateId = intent.getLongExtra(WorkTimerNotifications.EXTRA_ESTIMATE_ID, -1L)
        if (estimateId <= 0L) return

        if (intent.action == WorkTimerNotifications.ACTION_LONG_SESSION) {
            val expectedStartAt = intent.getLongExtra(WorkTimerNotifications.EXTRA_START_AT, -1L)
            val pending = goAsync()
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                try {
                    val dao = AppDatabase.get(context.applicationContext).dao()
                    WorkTimerNotifications.handleLongSessionReminder(
                        context = context.applicationContext,
                        dao = dao,
                        estimateId = estimateId,
                        expectedStartAt = expectedStartAt
                    )
                } finally {
                    pending.finish()
                }
            }
            return
        }

        val reason = when (intent.action) {
            WorkTimerNotifications.ACTION_PAUSE -> WorkEndReason.PAUSE
            WorkTimerNotifications.ACTION_STOP -> WorkEndReason.STOP
            else -> return
        }

        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val dao = AppDatabase.get(context.applicationContext).dao()
                val now = System.currentTimeMillis()
                dao.stopWorkSessionsSafely(
                    estimateId = estimateId,
                    endAt = now,
                    endReason = reason
                )
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
                WorkTimerNotifications.restoreActive(context.applicationContext, dao)
            } finally {
                pending.finish()
            }
        }
    }
}
