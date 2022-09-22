package com.ywh.dm.html

import android.graphics.drawable.Drawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Layout
import android.util.Log
import android.widget.TextView
import com.ywh.dm.dm_html.Html
import com.ywh.dm.dm_html.Html.ImageGetter
import java.net.URL
import kotlin.concurrent.thread
import kotlin.math.log

class MainActivity : AppCompatActivity() {
//    var str = "<span style=text-decoration:underline;color:#ff00ff;text-indent:2em>哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈\n哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈<span style=text-decoration:underline;color:#ff0000> 内层</span></span>"

    //        var str = "<span style=text-decoration:underline;color:#ff00ff>哈哈哈哈哈哈啊哈哈<br></br><img src=http://res.damieapp.com/drama/FkMtEoABb9SKEn1lvrn4TmclIRxj>\n好好好</span>"
    //    var str = "<span style=text-decoration:underline;color:#ff00ff;><span style=text-decoration:line-through;color:#ff0000;>嵌套span</span></span>"
    //    var str = "<span style=color:#ff00ff>哈哈<br >嘿嘿</span>"
    //    var str = "<p style=\"text-decoration:underline;\"><span style=text-decoration:line-through;>123hah&nbsp;&nbsp;</span></p>"
        var str = "<p>哈哈</p>\n哦哦哦"
    private var drawableMap = HashMap<String, Drawable>()
    private val imageGetter by lazy {
        object : ImageGetter {
            override fun getDrawable(source: String?): Drawable? {
                return source?.let {
                    val drawable = drawableMap[it]
                    if (drawable == null) {
                        getImageDrawable(it)
                        null
                    } else {
                        drawable
                    }
                }
            }

            override fun defaultDrawable(): Drawable {
                return resources.getDrawable(R.drawable.ic_launcher_background)
            }

            override fun drawableWidth(): Int {
                return MetricsUtil.WidthPixels
            }

        }
    }

    private fun getImageDrawable(url: String) {
        thread {
            //图片catch
            try {
                val drawable = Drawable.createFromStream(URL(url).openStream(), "")
                runOnUiThread {
                    if (drawable != null) {
                        drawableMap[url] = drawable
                        setHtmlData()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setHtmlData()
    }

    private fun setHtmlData() {
        val tvHello = findViewById<TextView>(R.id.tvHello)
        val oneChineseCharWidth = Layout.getDesiredWidth("一", 0, 1, tvHello.paint)
        Log.e("TAG-HTMLDATA", "setHtmlData: ${oneChineseCharWidth}")
        //        var str = "<span style=text-decoration:underline;>哈哈哈123333333<br></br>3333333哈哈哈哈哈哈<strong><em>嘿嘿</em></strong>哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈哈\n</span>"
        //        var str = "<div style=text-align:center>哈哈哈哈</div>"
        val span = Html.fromHtml(str, Html.FROM_HTML_MODE_COMPACT, imageGetter, null, oneChineseCharWidth)
        //        val leadingMarginSpan = LeadingMarginSpan.Standard(30, 0)
        //        val spannableStringBuilder = SpannableStringBuilder(span)
        //        spannableStringBuilder.setSpan(leadingMarginSpan, 0, span.length, Spannable.SPAN_PARAGRAPH)
        tvHello.text = span
    }
}