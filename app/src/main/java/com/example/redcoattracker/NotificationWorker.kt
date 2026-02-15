package com.example.redcoattracker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.redcoattracker.data.AppDatabase
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class NotificationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val disciplineDao = AppDatabase.getDatabase(applicationContext).disciplineDao()
        val disciplines = disciplineDao.getAllDisciplinesList()
        val today = LocalDate.now()

        val expiringDisciplines = disciplines.filter {
            val expiryDate = if (it.name == "Evaluation date") {
                it.completionDate.plusMonths(18)
            } else {
                it.completionDate.plusMonths(6)
            }
            val daysUntilExpiry = ChronoUnit.DAYS.between(today, expiryDate)
            daysUntilExpiry in 0..30
        }

        if (expiringDisciplines.isNotEmpty()) {
            sendNotification(expiringDisciplines.joinToString { it.name })
        }

        return Result.success()
    }

    private fun sendNotification(expiringDisciplines: String) {
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "discipline_expiry",
                "Discipline Expiry",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, "discipline_expiry")
            .setContentTitle("Expiring Disciplines")
            .setContentText("The following disciplines are expiring soon: $expiringDisciplines")
            .setSmallIcon(R.drawable.redcoat_splash)
            .build()

        notificationManager.notify(1, notification)
    }
}
