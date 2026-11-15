package tr.afa.cevirici

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.VideoProfile
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.Chronometer
import android.widget.GridLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/** Gelen, giden ve devam eden arama ekranı. */
class InCallActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ANSWER = "answer"
        fun intent(ctx: Context): Intent =
            Intent(ctx, InCallActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }

    private lateinit var nameView: TextView
    private lateinit var numberView: TextView
    private lateinit var stateView: TextView
    private lateinit var timer: Chronometer
    private lateinit var otherCall: TextView
    private lateinit var btnSwap: Button
    private lateinit var ongoingGroup: View
    private lateinit var incomingGroup: View
    private lateinit var btnMute: Button
    private lateinit var btnSpeaker: Button
    private lateinit var btnHold: Button
    private lateinit var dtmfPad: GridLayout
    private lateinit var dtmfText: TextView

    private val handler = Handler(Looper.getMainLooper())
    private var finishing = false
    private var proximityLock: PowerManager.WakeLock? = null
    private val listener: () -> Unit = { render() }
    private val finishTask = Runnable { finishAndRemoveTask() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_incall)

        nameView = findViewById(R.id.name)
        numberView = findViewById(R.id.numberText)
        stateView = findViewById(R.id.state)
        timer = findViewById(R.id.timer)
        otherCall = findViewById(R.id.otherCall)
        btnSwap = findViewById(R.id.btnSwap)
        ongoingGroup = findViewById(R.id.ongoingGroup)
        incomingGroup = findViewById(R.id.incomingGroup)
        btnMute = findViewById(R.id.btnMute)
        btnSpeaker = findViewById(R.id.btnSpeaker)
        btnHold = findViewById(R.id.btnHold)
        dtmfPad = findViewById(R.id.dtmfPad)
        dtmfText = findViewById(R.id.dtmfText)

        val pm = getSystemService(PowerManager::class.java)
        if (pm?.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK) == true) {
            proximityLock = pm.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "cevirici:proximity")
        }

        findViewById<View>(R.id.btnAnswer).setOnClickListener {
            CallManager.primaryCall?.answer(VideoProfile.STATE_AUDIO_ONLY)
        }
        findViewById<View>(R.id.btnReject).setOnClickListener {
            CallManager.primaryCall?.reject(false, null)
        }
        findViewById<View>(R.id.btnHangup).setOnClickListener {
            CallManager.primaryCall?.disconnect()
        }
        btnMute.setOnClickListener {
            val s = CallManager.service ?: return@setOnClickListener
            s.setMuted(!isMuted())
        }
        btnSpeaker.setOnClickListener {
            val s = CallManager.service ?: return@setOnClickListener
            @Suppress("DEPRECATION")
            s.setAudioRoute(
                if (isSpeaker()) CallAudioState.ROUTE_WIRED_OR_EARPIECE else CallAudioState.ROUTE_SPEAKER
            )
        }
        btnHold.setOnClickListener {
            val c = CallManager.primaryCall ?: return@setOnClickListener
            if (c.stateCompat() == Call.STATE_HOLDING) c.unhold() else c.hold()
        }
        btnSwap.setOnClickListener {
            val primary = CallManager.primaryCall
            CallManager.calls.firstOrNull { it != primary && it.stateCompat() == Call.STATE_HOLDING }?.unhold()
        }
        findViewById<View>(R.id.btnKeypad).setOnClickListener {
            val show = dtmfPad.visibility != View.VISIBLE
            dtmfPad.visibility = if (show) View.VISIBLE else View.GONE
            dtmfText.visibility = if (show) View.VISIBLE else View.GONE
        }
        Keypad.build(dtmfPad, light = true, onKey = { c ->
            val call = CallManager.primaryCall ?: return@build
            call.playDtmfTone(c)
            handler.postDelayed({ call.stopDtmfTone() }, 180)
            dtmfText.append(c.toString())
        })

        CallManager.addListener(listener)
        handleAnswer(intent)
        render()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleAnswer(intent)
        render()
    }

    override fun onDestroy() {
        CallManager.removeListener(listener)
        handler.removeCallbacksAndMessages(null)
        setProximity(false)
        super.onDestroy()
    }

    private fun handleAnswer(i: Intent?) {
        if (i?.getBooleanExtra(EXTRA_ANSWER, false) == true) {
            i.removeExtra(EXTRA_ANSWER)
            CallManager.primaryCall?.takeIf { it.stateCompat() == Call.STATE_RINGING }
                ?.answer(VideoProfile.STATE_AUDIO_ONLY)
        }
    }

    @Suppress("DEPRECATION")
    private fun audioState(): CallAudioState? = CallManager.service?.callAudioState
    private fun isMuted() = audioState()?.isMuted == true
    private fun isSpeaker() = audioState()?.route == CallAudioState.ROUTE_SPEAKER

    private fun render() {
        val call = CallManager.primaryCall
        if (call == null) {
            stateView.text = CallUtil.stateLabel(Call.STATE_DISCONNECTED)
            timer.stop()
            setProximity(false)
            if (!finishing) {
                finishing = true
                handler.postDelayed(finishTask, 900)
            }
            return
        }
        finishing = false
        handler.removeCallbacks(finishTask)

        val state = call.stateCompat()
        val number = call.number()
        val contact = CallUtil.lookupName(this, number)
        nameView.text = contact ?: CallUtil.displayName(this, number)
        numberView.text = if (contact != null) CallUtil.format(number) else ""
        stateView.text = CallUtil.stateLabel(state)

        val ringing = state == Call.STATE_RINGING
        incomingGroup.visibility = if (ringing) View.VISIBLE else View.GONE
        ongoingGroup.visibility = if (ringing) View.GONE else View.VISIBLE
        if (ringing) { dtmfPad.visibility = View.GONE; dtmfText.visibility = View.GONE }

        val connect = call.details?.connectTimeMillis ?: 0L
        if (state == Call.STATE_ACTIVE && connect > 0) {
            timer.base = SystemClock.elapsedRealtime() - (System.currentTimeMillis() - connect)
            timer.visibility = View.VISIBLE
            timer.start()
        } else {
            timer.stop()
            timer.visibility = View.GONE
        }

        btnMute.text = if (isMuted()) "Sesi aç" else "Sessiz"
        btnSpeaker.text = if (isSpeaker()) "Ahize" else "Hoparlör"
        btnHold.text = if (state == Call.STATE_HOLDING) "Devam" else "Beklet"
        btnHold.isEnabled = state == Call.STATE_HOLDING ||
            (state == Call.STATE_ACTIVE && call.details?.can(Call.Details.CAPABILITY_HOLD) == true)

        val other = CallManager.calls.firstOrNull { it != call }
        if (other != null) {
            otherCall.visibility = View.VISIBLE
            otherCall.text = "${CallUtil.stateLabel(other.stateCompat())}: ${CallUtil.displayName(this, other.number())}"
            btnSwap.visibility =
                if (other.stateCompat() == Call.STATE_HOLDING && !ringing) View.VISIBLE else View.GONE
        } else {
            otherCall.visibility = View.GONE
            btnSwap.visibility = View.GONE
        }

        // Kulağa yaklaştırınca ekranı kapat (hoparlör açıkken değil)
        val talking = state == Call.STATE_ACTIVE || state == Call.STATE_DIALING || state == Call.STATE_CONNECTING
        setProximity(talking && !isSpeaker())
    }

    private fun setProximity(on: Boolean) {
        val lock = proximityLock ?: return
        if (on && !lock.isHeld) lock.acquire(3 * 60 * 60 * 1000L)
        else if (!on && lock.isHeld) lock.release()
    }
}
