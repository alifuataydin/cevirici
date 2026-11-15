package tr.afa.cevirici

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Bildirimdeki "Reddet" ve "Kapat" düğmeleri. */
class CallActionReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_REJECT = "tr.afa.cevirici.REJECT"
        const val ACTION_HANGUP = "tr.afa.cevirici.HANGUP"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val call = CallManager.primaryCall ?: return
        when (intent.action) {
            ACTION_REJECT -> call.reject(false, null)
            ACTION_HANGUP -> call.disconnect()
        }
    }
}
