package com.ywh.dm.html

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import com.ywh.dm.dm_html.Html

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val tvHello = findViewById<TextView>(R.id.tvHello)
//        var str = "<span style=text-decoration:underline;>哈哈哈123333333<br></br>3333333哈哈哈哈哈哈<strong><em>嘿嘿</em></strong>哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈\n</span>"
//        var str = "<div style=text-align:center>哈哈哈哈</div>"
        var str = "<span style=text-decoration:underline;color:#ff00ff;text-indent:2em>哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈<span style=text-decoration:underline;color:#ff0000> 内层</span></span>"

        val span = Html.fromHtml(str, Html.FROM_HTML_MODE_LEGACY, null, null)
        //        val leadingMarginSpan = LeadingMarginSpan.Standard(30, 0)
        //        val spannableStringBuilder = SpannableStringBuilder(span)
        //        spannableStringBuilder.setSpan(leadingMarginSpan, 0, span.length, Spannable.SPAN_PARAGRAPH)
        tvHello.text = span
    }
}