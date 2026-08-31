package pt.haconnect.arquivoiv.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import pt.haconnect.arquivoiv.MainActivity
import pt.haconnect.arquivoiv.R
import pt.haconnect.arquivoiv.data.repository.FaturaRepository
import pt.haconnect.arquivoiv.domain.model.Fatura
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Fase 5: notificações de retenção legal de 5 anos.
 * O prazo conta a partir de `dataInsercao` (data em que a fatura foi arquivada).
 * A app NUNCA elimina automaticamente — apenas informa que a fatura pode ser
 * arquivada a frio/eliminada. Anti-spam por marco (1 notificação por fatura).
 */
@HiltWorker
class RetencaoWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val faturaRepository: FaturaRepository
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val CHANNEL_ID = "retencao_alerts"
        const val GRUPO = "retencao_5_anos"
        const val ANOS_RETENCAO = 5L
        /** Aviso prévio quando faltam ≤ 30 dias para completar os 5 anos. */
        const val DIAS_AVISO = 30L
    }

    override suspend fun doWork(): Result {
        criarCanal()
        val hoje = LocalDate.now()
        var elegiveis = 0

        faturaRepository.getAllList().forEach { fatura ->
            val dataLimite = Instant.ofEpochMilli(fatura.dataInsercao)
                .atZone(ZoneId.systemDefault()).toLocalDate().plusYears(ANOS_RETENCAO)
            val diasRestantes = ChronoUnit.DAYS.between(hoje, dataLimite)

            when {
                // Aviso prévio: faltam ≤30 dias e ainda não foi notificada para este marco.
                diasRestantes in 0..DIAS_AVISO && !fatura.notificado30dias -> {
                    enviarNotificacao(fatura, diasRestantes, dataLimite)
                    faturaRepository.marcarNotificado30dias(fatura.id)
                    elegiveis++
                }

                // Marco atingido: completou os 5 anos e ainda não foi notificada.
                diasRestantes < 0 && !fatura.notificado5anos -> {
                    enviarNotificacao(fatura, diasRestantes, dataLimite)
                    faturaRepository.marcarNotificado5anos(fatura.id)
                    elegiveis++
                }
            }
        }

        if (elegiveis > 1) {
            enviarSumario()
        }

        return Result.success()
    }

    private fun criarCanal() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                CHANNEL_ID,
                applicationContext.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = applicationContext.getString(R.string.notification_channel_desc) }
            applicationContext.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(canal)
        }
    }

    /** Notificação individual — ao tocar abre o detalhe da fatura. */
    private fun enviarNotificacao(fatura: Fatura, diasRestantes: Long, dataLimite: LocalDate) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val ctx = applicationContext
        val dataLimiteStr = dataLimite.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        val completou = diasRestantes < 0

        val titulo = ctx.getString(
            if (completou) R.string.notification_retencao_5anos_title
            else R.string.notification_retencao_30dias_title
        )
        val corpo = if (completou) {
            ctx.getString(
                R.string.notification_retencao_5anos_body,
                fatura.numeroFatura,
                fatura.fornecedor,
                dataLimiteStr
            )
        } else {
            ctx.getString(
                R.string.notification_retencao_30dias_body,
                fatura.numeroFatura,
                fatura.fornecedor,
                diasRestantes
            )
        }

        val intent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("fatura_id", fatura.id)
        }
        val pending = PendingIntent.getActivity(
            ctx,
            fatura.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificacao = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(titulo)
            .setContentText(corpo)
            .setStyle(NotificationCompat.BigTextStyle().bigText(corpo))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setGroup(GRUPO)
            .build()

        NotificationManagerCompat.from(ctx).notify(fatura.id.toInt(), notificacao)
    }

    /** Resumo do grupo (só quando há mais de uma fatura elegível) — abre a lista. */
    private fun enviarSumario() {
        val ctx = applicationContext
        val intent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            ctx,
            0x5A7E4,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificacao = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(ctx.getString(R.string.notification_group_title))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setGroup(GRUPO)
            .setGroupSummary(true)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(ctx).notify(0x5A7E4, notificacao)
    }
}
