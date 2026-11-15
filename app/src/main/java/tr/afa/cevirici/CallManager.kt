package tr.afa.cevirici

import android.os.Build
import android.telecom.Call

/** Telecom'dan gelen aramaları tutan ve arayüze değişiklikleri bildiren tekil nesne. */
object CallManager {
    var service: CallService? = null

    private val _calls = mutableListOf<Call>()
    val calls: List<Call> get() = _calls.toList()

    private val listeners = mutableSetOf<() -> Unit>()

    private val callback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) = notifyChanged()
        override fun onDetailsChanged(call: Call, details: Call.Details) = notifyChanged()
    }

    /** Ekranda gösterilecek arama: önce çalan, sonra aranan, sonra görüşmedeki, sonra bekleyen. */
    val primaryCall: Call?
        get() {
            val order = listOf(
                Call.STATE_RINGING, Call.STATE_DIALING, Call.STATE_CONNECTING,
                Call.STATE_SELECT_PHONE_ACCOUNT, Call.STATE_ACTIVE, Call.STATE_HOLDING
            )
            for (s in order) _calls.firstOrNull { it.stateCompat() == s }?.let { return it }
            return _calls.lastOrNull()
        }

    fun add(call: Call) {
        _calls.add(call)
        call.registerCallback(callback)
        notifyChanged()
    }

    fun remove(call: Call) {
        call.unregisterCallback(callback)
        _calls.remove(call)
        notifyChanged()
    }

    fun addListener(l: () -> Unit) { listeners.add(l) }
    fun removeListener(l: () -> Unit) { listeners.remove(l) }

    fun notifyChanged() {
        listeners.toList().forEach { it() }
    }
}

@Suppress("DEPRECATION")
fun Call.stateCompat(): Int =
    if (Build.VERSION.SDK_INT >= 31) details.state else state

fun Call.number(): String = details?.handle?.schemeSpecificPart ?: ""
