package tr.afa.cevirici

import android.Manifest
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.provider.CallLog
import android.text.format.DateUtils
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class RecentsActivity : AppCompatActivity() {

    data class Entry(val number: String, val name: String?, val type: Int, val date: Long, val duration: Long)

    private lateinit var adapter: ArrayAdapter<Entry>
    private lateinit var empty: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)
        title = "Son aramalar"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val list = findViewById<ListView>(R.id.list)
        empty = findViewById(R.id.empty)
        list.emptyView = empty

        adapter = object : ArrayAdapter<Entry>(this, android.R.layout.simple_list_item_2, android.R.id.text1, mutableListOf()) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val v = super.getView(position, convertView, parent)
                val e = getItem(position)!!
                val t1 = v.findViewById<TextView>(android.R.id.text1)
                val t2 = v.findViewById<TextView>(android.R.id.text2)
                if (t2.tag == null) t2.tag = t2.textColors
                t1.text = e.name?.takeIf { it.isNotBlank() }
                    ?: if (e.number.isBlank()) "Gizli numara" else CallUtil.format(e.number)
                val dur = if (e.duration > 0) " · " + DateUtils.formatElapsedTime(e.duration) else ""
                t2.text = "${typeLabel(e.type)} · ${DateUtils.getRelativeTimeSpanString(e.date)}$dur"
                if (e.type == CallLog.Calls.MISSED_TYPE) t2.setTextColor(Color.rgb(0xD9, 0x30, 0x25))
                else t2.setTextColor(t2.tag as ColorStateList)
                return v
            }
        }
        list.adapter = adapter
        list.setOnItemClickListener { _, _, pos, _ ->
            adapter.getItem(pos)?.let { CallUtil.placeCall(this, it.number) }
        }
        list.setOnItemLongClickListener { _, _, pos, _ ->
            adapter.getItem(pos)?.let {
                startActivity(
                    Intent(this, MainActivity::class.java).putExtra(MainActivity.EXTRA_NUMBER, it.number)
                )
            }
            true
        }
    }

    override fun onResume() {
        super.onResume()
        if (!CallUtil.has(this, Manifest.permission.READ_CALL_LOG)) {
            empty.text = "Arama kaydını görmek için izin gerekli."
            return
        }
        empty.text = "Kayıt yok"
        adapter.clear()
        adapter.addAll(load())
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }

    private fun load(): List<Entry> {
        val out = mutableListOf<Entry>()
        try {
            contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(
                    CallLog.Calls.NUMBER, CallLog.Calls.CACHED_NAME, CallLog.Calls.TYPE,
                    CallLog.Calls.DATE, CallLog.Calls.DURATION
                ),
                null, null, "${CallLog.Calls.DATE} DESC"
            )?.use { c ->
                while (c.moveToNext() && out.size < 300) {
                    out.add(Entry(c.getString(0) ?: "", c.getString(1), c.getInt(2), c.getLong(3), c.getLong(4)))
                }
            }
        } catch (_: SecurityException) { }
        return out
    }

    private fun typeLabel(type: Int) = when (type) {
        CallLog.Calls.INCOMING_TYPE -> "↙ Gelen"
        CallLog.Calls.OUTGOING_TYPE -> "↗ Giden"
        CallLog.Calls.MISSED_TYPE -> "✕ Cevapsız"
        CallLog.Calls.REJECTED_TYPE -> "⊘ Reddedilen"
        CallLog.Calls.BLOCKED_TYPE -> "⊘ Engellenen"
        else -> "Arama"
    }
}
