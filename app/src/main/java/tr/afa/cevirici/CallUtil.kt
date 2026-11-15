package tr.afa.cevirici

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.telecom.Call
import android.telecom.TelecomManager
import android.telephony.PhoneNumberUtils
import android.widget.Toast
import androidx.core.content.ContextCompat
import java.util.Locale

object CallUtil {
    private val nameCache = mutableMapOf<String, String?>()

    fun has(ctx: Context, perm: String) =
        ContextCompat.checkSelfPermission(ctx, perm) == PackageManager.PERMISSION_GRANTED

    fun placeCall(ctx: Context, number: String) {
        val n = number.trim()
        if (n.isEmpty()) {
            Toast.makeText(ctx, "Numara girin", Toast.LENGTH_SHORT).show()
            return
        }
        if (!has(ctx, Manifest.permission.CALL_PHONE)) {
            Toast.makeText(ctx, "Arama izni verilmedi", Toast.LENGTH_SHORT).show()
            return
        }
        val uri = Uri.fromParts("tel", n, null)
        val tm = ctx.getSystemService(TelecomManager::class.java)
        try {
            if (tm != null) tm.placeCall(uri, Bundle())
            else ctx.startActivity(Intent(Intent.ACTION_CALL, uri))
        } catch (e: SecurityException) {
            ctx.startActivity(Intent(Intent.ACTION_CALL, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    fun lookupName(ctx: Context, number: String): String? {
        if (number.isBlank() || !has(ctx, Manifest.permission.READ_CONTACTS)) return null
        if (nameCache.containsKey(number)) return nameCache[number]
        val name = try {
            val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
            ctx.contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME), null, null, null)
                ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
        } catch (_: Exception) { null }
        nameCache[number] = name
        return name
    }

    fun format(number: String): String {
        if (number.isBlank()) return ""
        val country = Locale.getDefault().country.ifBlank { "TR" }
        return PhoneNumberUtils.formatNumber(number, country) ?: number
    }

    fun displayName(ctx: Context, number: String): String =
        lookupName(ctx, number) ?: if (number.isBlank()) "Gizli numara" else format(number)

    fun stateLabel(state: Int): String = when (state) {
        Call.STATE_RINGING -> "Gelen arama"
        Call.STATE_DIALING -> "Aranıyor…"
        Call.STATE_CONNECTING -> "Bağlanıyor…"
        Call.STATE_SELECT_PHONE_ACCOUNT -> "SIM seçiliyor…"
        Call.STATE_ACTIVE -> "Görüşmede"
        Call.STATE_HOLDING -> "Beklemede"
        Call.STATE_DISCONNECTING -> "Sonlandırılıyor…"
        Call.STATE_DISCONNECTED -> "Arama sona erdi"
        else -> ""
    }
}
