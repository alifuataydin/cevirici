package tr.afa.cevirici

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.telecom.TelecomManager
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_NUMBER = "number"
    }

    private lateinit var numberView: TextView
    private lateinit var defaultBanner: View
    private lateinit var returnCall: Button
    private val number = StringBuilder()
    private var tone: ToneGenerator? = null

    private val roleLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { updateBanners() }

    private val permLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    private val callListener: () -> Unit = { updateBanners() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        numberView = findViewById(R.id.number)
        defaultBanner = findViewById(R.id.defaultBanner)
        returnCall = findViewById(R.id.btnReturnCall)

        tone = try { ToneGenerator(AudioManager.STREAM_DTMF, 70) } catch (_: RuntimeException) { null }

        findViewById<Button>(R.id.btnMakeDefault).setOnClickListener { requestDefaultDialer() }
        findViewById<Button>(R.id.btnRecents).setOnClickListener {
            startActivity(Intent(this, RecentsActivity::class.java))
        }
        findViewById<Button>(R.id.btnContacts).setOnClickListener {
            startActivity(Intent(this, ContactsActivity::class.java))
        }
        returnCall.setOnClickListener { startActivity(InCallActivity.intent(this)) }

        Keypad.build(
            findViewById<GridLayout>(R.id.keypad),
            onKey = { c -> append(c); playTone(c) },
            onLongKey = { c -> if (c == '0') { append('+'); true } else false }
        )

        val delete = findViewById<ImageButton>(R.id.btnDelete)
        delete.setOnClickListener {
            if (number.isNotEmpty()) { number.deleteCharAt(number.length - 1); render() }
        }
        delete.setOnLongClickListener { number.clear(); render(); true }

        findViewById<View>(R.id.btnCall).setOnClickListener { CallUtil.placeCall(this, number.toString()) }

        handleIntent(intent)
        requestPermissions()
        CallManager.addListener(callListener)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        updateBanners()
    }

    override fun onDestroy() {
        CallManager.removeListener(callListener)
        tone?.release()
        super.onDestroy()
    }

    private fun handleIntent(i: Intent?) {
        val fromUri = i?.data?.takeIf { it.scheme == "tel" }?.schemeSpecificPart
        val fromExtra = i?.getStringExtra(EXTRA_NUMBER)
        val n = fromUri ?: fromExtra ?: return
        number.clear()
        number.append(n.filter { it.isDigit() || it in "+*#,;" })
        render()
    }

    private fun append(c: Char) { number.append(c); render() }

    private fun render() {
        numberView.text = CallUtil.format(number.toString()).ifEmpty { number.toString() }
    }

    private fun playTone(c: Char) {
        val t = when (c) {
            in '0'..'9' -> ToneGenerator.TONE_DTMF_0 + (c - '0')
            '*' -> ToneGenerator.TONE_DTMF_S
            '#' -> ToneGenerator.TONE_DTMF_P
            else -> return
        }
        tone?.startTone(t, 120)
    }

    private fun isDefaultDialer(): Boolean =
        getSystemService(TelecomManager::class.java)?.defaultDialerPackage == packageName

    private fun updateBanners() {
        defaultBanner.visibility = if (isDefaultDialer()) View.GONE else View.VISIBLE
        returnCall.visibility = if (CallManager.primaryCall != null) View.VISIBLE else View.GONE
    }

    private fun requestDefaultDialer() {
        if (Build.VERSION.SDK_INT >= 29) {
            val rm = getSystemService(RoleManager::class.java) ?: return
            if (rm.isRoleAvailable(RoleManager.ROLE_DIALER) && !rm.isRoleHeld(RoleManager.ROLE_DIALER)) {
                roleLauncher.launch(rm.createRequestRoleIntent(RoleManager.ROLE_DIALER))
            }
        } else {
            roleLauncher.launch(
                Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER)
                    .putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, packageName)
            )
        }
    }

    private fun requestPermissions() {
        val perms = mutableListOf(
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_PHONE_STATE
        )
        if (Build.VERSION.SDK_INT >= 33) perms.add(Manifest.permission.POST_NOTIFICATIONS)
        val missing = perms.filter { !CallUtil.has(this, it) }
        if (missing.isNotEmpty()) permLauncher.launch(missing.toTypedArray())
    }
}
