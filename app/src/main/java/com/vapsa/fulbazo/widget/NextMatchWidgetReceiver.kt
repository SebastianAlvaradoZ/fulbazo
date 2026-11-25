package com.vapsa.fulbazo.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback

/** Recibe los avisos del sistema sobre el widget "Mi próximo partido". */
class NextMatchWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = NextMatchWidget()

    /** Al poner el widget, al reiniciar el teléfono y al actualizar la app. */
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        NextMatchRefreshWorker.schedule(context)
        NextMatchRefreshWorker.refreshNow(context)
    }

    /** Se quitó el último widget. */
    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        NextMatchRefreshWorker.cancel(context)
    }
}

/** Botón de actualizar del widget. */
class RefreshNextMatchAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        NextMatchRefreshWorker.refreshNow(context)
    }
}
