package tr.afa.cevirici

import android.graphics.Color
import android.text.SpannableString
import android.text.style.RelativeSizeSpan
import android.util.TypedValue
import android.view.Gravity
import android.widget.GridLayout
import android.widget.TextView

/** Tuş takımını (1-9, *, 0, #) verilen GridLayout'a dizer. */
object Keypad {
    private val keys = listOf(
        '1' to "", '2' to "ABC", '3' to "DEF",
        '4' to "GHI", '5' to "JKL", '6' to "MNO",
        '7' to "PQRS", '8' to "TUV", '9' to "WXYZ",
        '*' to "", '0' to "+", '#' to ""
    )

    fun build(
        grid: GridLayout,
        light: Boolean = false,
        onKey: (Char) -> Unit,
        onLongKey: (Char) -> Boolean = { false }
    ) {
        val ctx = grid.context
        val keyHeight = (68 * ctx.resources.displayMetrics.density).toInt()
        val bg = TypedValue().also {
            ctx.theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, it, true)
        }
        grid.removeAllViews()
        for ((digit, letters) in keys) {
            val label = SpannableString(if (letters.isEmpty()) "$digit" else "$digit\n$letters")
            if (letters.isNotEmpty()) label.setSpan(RelativeSizeSpan(0.38f), 2, label.length, 0)
            val key = TextView(ctx).apply {
                text = label
                gravity = Gravity.CENTER
                textSize = 30f
                if (light) setTextColor(Color.WHITE)
                setBackgroundResource(bg.resourceId)
                isClickable = true
                isFocusable = true
                contentDescription = digit.toString()
                setOnClickListener { onKey(digit) }
                setOnLongClickListener { onLongKey(digit) }
            }
            val lp = GridLayout.LayoutParams(
                GridLayout.spec(GridLayout.UNDEFINED, 1f),
                GridLayout.spec(GridLayout.UNDEFINED, 1f)
            ).apply { width = 0; height = keyHeight }
            grid.addView(key, lp)
        }
    }
}
