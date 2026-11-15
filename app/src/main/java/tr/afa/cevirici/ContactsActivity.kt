package tr.afa.cevirici

import android.Manifest
import android.os.Bundle
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import java.util.Locale

class ContactsActivity : AppCompatActivity() {

    data class Entry(val name: String, val number: String, val label: String)

    private var all: List<Entry> = emptyList()
    private lateinit var adapter: ArrayAdapter<Entry>
    private lateinit var empty: TextView
    private lateinit var search: EditText
    private val tr = Locale("tr", "TR")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)
        title = "Rehber"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val list = findViewById<ListView>(R.id.list)
        empty = findViewById(R.id.empty)
        search = findViewById(R.id.search)
        search.visibility = View.VISIBLE
        list.emptyView = empty

        adapter = object : ArrayAdapter<Entry>(this, android.R.layout.simple_list_item_2, android.R.id.text1, mutableListOf()) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val v = super.getView(position, convertView, parent)
                val e = getItem(position)!!
                v.findViewById<TextView>(android.R.id.text1).text = e.name
                v.findViewById<TextView>(android.R.id.text2).text =
                    listOf(e.label, CallUtil.format(e.number)).filter { it.isNotBlank() }.joinToString(" · ")
                return v
            }
        }
        list.adapter = adapter
        list.setOnItemClickListener { _, _, pos, _ ->
            adapter.getItem(pos)?.let { CallUtil.placeCall(this, it.number) }
        }
        search.doAfterTextChanged { applyFilter() }
    }

    override fun onResume() {
        super.onResume()
        if (!CallUtil.has(this, Manifest.permission.READ_CONTACTS)) {
            empty.text = "Rehberi görmek için izin gerekli."
            return
        }
        empty.text = "Kişi bulunamadı"
        all = load()
        applyFilter()
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }

    private fun applyFilter() {
        val q = search.text.toString().trim().lowercase(tr)
        val digits = q.filter { it.isDigit() }
        val filtered = if (q.isEmpty()) all else all.filter {
            it.name.lowercase(tr).contains(q) ||
                (digits.isNotEmpty() && it.number.filter { ch -> ch.isDigit() }.contains(digits))
        }
        adapter.clear()
        adapter.addAll(filtered)
    }

    private fun load(): List<Entry> {
        val out = mutableListOf<Entry>()
        val seen = HashSet<String>()
        try {
            contentResolver.query(
                Phone.CONTENT_URI,
                arrayOf(Phone.DISPLAY_NAME, Phone.NUMBER, Phone.TYPE, Phone.LABEL),
                null, null, "${Phone.DISPLAY_NAME} COLLATE LOCALIZED ASC"
            )?.use { c ->
                while (c.moveToNext()) {
                    val name = c.getString(0) ?: continue
                    val number = c.getString(1) ?: continue
                    val key = name + "|" + number.filter { it.isDigit() || it == '+' }
                    if (!seen.add(key)) continue
                    val label = Phone.getTypeLabel(resources, c.getInt(2), c.getString(3)).toString()
                    out.add(Entry(name, number, label))
                }
            }
        } catch (_: SecurityException) { }
        return out
    }
}
