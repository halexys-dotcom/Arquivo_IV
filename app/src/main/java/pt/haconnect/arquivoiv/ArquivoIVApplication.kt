package pt.haconnect.arquivoiv

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import pt.haconnect.arquivoiv.worker.RetencaoWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class ArquivoIVApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        try {
            scheduleRetencaoAlerts()
        } catch (_: Exception) {
            // WorkManager might not be ready yet
        }
    }

    private fun scheduleRetencaoAlerts() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(false)
            .build()

        // 1. Verificação imediata ao iniciar a app
        val immediateWork = OneTimeWorkRequestBuilder<RetencaoWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniqueWork(
            "retencao_immediate_check",
            ExistingWorkPolicy.REPLACE,
            immediateWork
        )

        // 2. Verificação periódica em segundo plano (24 em 24h)
        val alertWork = PeriodicWorkRequestBuilder<RetencaoWorker>(
            24, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "retencao_alert_work",
            ExistingPeriodicWorkPolicy.KEEP,
            alertWork
        )
    }
}
