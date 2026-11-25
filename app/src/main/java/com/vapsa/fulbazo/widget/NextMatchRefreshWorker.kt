package com.vapsa.fulbazo.widget

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit

/** Actualiza el widget con la app cerrada (15 minutos es lo mínimo que permite Android). */
class NextMatchRefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val updater = EntryPointAccessors.fromApplication(applicationContext, WidgetEntryPoint::class.java)
            .nextMatchUpdater()
        return if (updater.refresh()) Result.success() else Result.retry()
    }

    companion object {
        private const val PERIODIC_WORK = "widget_proximo_partido"
        private const val REFRESH_NOW_WORK = "widget_proximo_partido_ahora"
        private const val PERIOD_MINUTES = 15L

        /** Además de esperar a que haya red, hace que Android se la dé a la app en segundo plano. */
        private val withNetwork = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        fun schedule(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<NextMatchRefreshWorker>(PERIOD_MINUTES, TimeUnit.MINUTES)
                    .setConstraints(withNetwork)
                    .build(),
            )
        }

        /** Si ya hay una actualización en marcha, no empieza otra. */
        fun refreshNow(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                REFRESH_NOW_WORK,
                ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<NextMatchRefreshWorker>().setConstraints(withNetwork).build(),
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_WORK)
        }
    }
}

/** El worker y el widget los crea Android, no Hilt: por aquí piden lo que necesitan. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun nextMatchUpdater(): NextMatchUpdater
}
