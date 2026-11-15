package tr.afa.cevirici

import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService

/**
 * Uygulama varsayılan telefon uygulaması olduğunda sistem bu servise bağlanır
 * ve her gelen/giden aramayı buraya teslim eder.
 */
class CallService : InCallService() {

    private val listener: () -> Unit = { CallNotifier.update(this) }

    override fun onCreate() {
        super.onCreate()
        CallManager.service = this
        CallNotifier.createChannels(this)
        CallManager.addListener(listener)
    }

    override fun onDestroy() {
        CallManager.removeListener(listener)
        CallManager.service = null
        CallNotifier.cancel(this)
        super.onDestroy()
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        CallManager.add(call)
        // Gelen aramada tam ekran bildirim (kilit ekranında da) arama ekranını açar.
        // Giden aramada kullanıcı zaten uygulamada olduğu için ekranı doğrudan açıyoruz.
        if (call.stateCompat() != Call.STATE_RINGING) {
            try {
                startActivity(InCallActivity.intent(this))
            } catch (_: Exception) { }
        }
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        CallManager.remove(call)
    }

    @Deprecated("API 34'te kullanımdan kalktı ama tüm sürümlerde çalışır")
    override fun onCallAudioStateChanged(audioState: CallAudioState?) {
        @Suppress("DEPRECATION")
        super.onCallAudioStateChanged(audioState)
        CallManager.notifyChanged()
    }
}
