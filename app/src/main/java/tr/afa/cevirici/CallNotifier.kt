package tr.afa.cevirici

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.telecom.Call
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

object CallNotifier {
    private const val CH_INCOMING = "incoming_calls"
    private const val CH_ONGOING = "ongoing_calls"
    private const val NOTIF_ID = 42

    fun createChannels(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java) ?: return
        val incoming = NotificationChannel(CH_INCOMING, "Gelen aramalar", NotificationManager.IMPORTANCE_HIGH).apply {
            // Zil sesini Telecom sistemi çalar; bildirim ayrıca ses çıkarmasın
            setSound(null, null)
            enableVibration(false)
        }
        val ongoing = NotificationChannel(CH_ONGOING, "Devam eden aramalar", NotificationManager.IMPORTANCE_LOW)
        nm.createNotificationChannel(incoming)
        nm.createNotificationChannel(ongoing)
    }

    private val piFlags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT

    @SuppressLint("MissingPermission")
    fun update(ctx: Context) {
        val nm = NotificationManagerCompat.from(ctx)
        val call = CallManager.primaryCall
        if (call == null) { nm.cancel(NOTIF_ID); return }
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            // Bildirim izni yoksa gelen aramada ekranı doğrudan açmayı dene
            if (call.stateCompat() == Call.STATE_RINGING) {
                try { ctx.startActivity(InCallActivity.intent(ctx)) } catch (_: Exception) { }
            }
            return
        }

        val name = CallUtil.displayName(ctx, call.number())
        val openPi = PendingIntent.getActivity(ctx, 0, InCallActivity.intent(ctx), piFlags)

        val builder = if (call.stateCompat() == Call.STATE_RINGING) {
            val answerPi = PendingIntent.getActivity(
                ctx, 1, InCallActivity.intent(ctx).putExtra(InCallActivity.EXTRA_ANSWER, true), piFlags
            )
            val rejectPi = PendingIntent.getBroadcast(
                ctx, 2, Intent(ctx, CallActionReceiver::class.java).setAction(CallActionReceiver.ACTION_REJECT), piFlags
            )
            NotificationCompat.Builder(ctx, CH_INCOMING)
                .setContentTitle("Gelen arama")
                .setContentText(name)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setFullScreenIntent(openPi, true)
                .addAction(0, "Reddet", rejectPi)
                .addAction(0, "Yanıtla", answerPi)
        } else {
            val hangupPi = PendingIntent.getBroadcast(
                ctx, 3, Intent(ctx, CallActionReceiver::class.java).setAction(CallActionReceiver.ACTION_HANGUP), piFlags
            )
            val b = NotificationCompat.Builder(ctx, CH_ONGOING)
                .setContentTitle(name)
                .setContentText(CallUtil.stateLabel(call.stateCompat()))
                .addAction(0, "Kapat", hangupPi)
            val connect = call.details?.connectTimeMillis ?: 0L
            if (call.stateCompat() == Call.STATE_ACTIVE && connect > 0) {
                b.setWhen(connect).setUsesChronometer(true).setShowWhen(true)
            }
            b
        }

        builder.setSmallIcon(R.drawable.ic_call)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setOngoing(true)
            .setOnlyAlertOnce(call.stateCompat() != Call.STATE_RINGING)
            .setContentIntent(openPi)

        try { nm.notify(NOTIF_ID, builder.build()) } catch (_: SecurityException) { }
    }

    fun cancel(ctx: Context) = NotificationManagerCompat.from(ctx).cancel(NOTIF_ID)
}
