package com.ywh.dm.html

import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val tvHello = findViewById<TextView>(R.id.tvHello)
        var str =
//                    "<span style=text-decoration:underline;>哈哈哈123333333<br></br>3333333哈哈哈哈哈哈<strong><em>嘿嘿</em></strong>哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈1 \n</span>"
            //            .plus("<p style=text-align:left>哇哇哇哇无</p>")
            "<p style=text-align:end;>13</p>"
        val span = Html.fromHtml(str, Html.FROM_HTML_MODE_LEGACY, null, null)
        //        val leadingMarginSpan = LeadingMarginSpan.Standard(30, 0)
        //        val spannableStringBuilder = SpannableStringBuilder(span)
        //        spannableStringBuilder.setSpan(leadingMarginSpan, 0, span.length, Spannable.SPAN_PARAGRAPH)
        tvHello.text = span
    }
}