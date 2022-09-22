package com.ywh.dm.dm_html

import android.graphics.drawable.Drawable
import android.text.Html.TagHandler
import android.text.Spanned
import org.ccil.cowan.tagsoup.HTMLSchema
import org.ccil.cowan.tagsoup.Parser
import org.xml.sax.SAXNotRecognizedException
import org.xml.sax.SAXNotSupportedException

/**
 * Created by ywh on 2022/09/16 0020.
 * Html parser based on native Html parser.
 */
object Html {
    interface ImageGetter {
        /**
         * This method is called when the HTML parser encounters an
         * &lt;img&gt; tag.  The `source` argument is the
         * string from the "src" attribute; the return value should be
         * a Drawable representation of the image or `null`
         * for a generic replacement image.  Make sure you call
         * setBounds() on your Drawable if it doesn't already have
         * its bounds set.
         */
        fun getDrawable(source: String?): Drawable?

        //默认图片
        fun defaultDrawable(): Drawable

        //图片宽 -1 原图尺寸
        fun drawableWidth(): Int = -1
    }

    /**
     * Flag indicating that texts inside &lt;p&gt; elements will be separated from other texts with
     * one newline character by default.
     */
    const val FROM_HTML_SEPARATOR_LINE_BREAK_PARAGRAPH = 0x00000001

    /**
     * Flag indicating that texts inside &lt;h1&gt;~&lt;h6&gt; elements will be separated from
     * other texts with one newline character by default.
     */
    const val FROM_HTML_SEPARATOR_LINE_BREAK_HEADING = 0x00000002

    /**
     * Flag indicating that texts inside &lt;li&gt; elements will be separated from other texts
     * with one newline character by default.
     */
    const val FROM_HTML_SEPARATOR_LINE_BREAK_LIST_ITEM = 0x00000004

    /**
     * Flag indicating that texts inside &lt;ul&gt; elements will be separated from other texts
     * with one newline character by default.
     */
    const val FROM_HTML_SEPARATOR_LINE_BREAK_LIST = 0x00000008

    /**
     * Flag indicating that texts inside &lt;div&gt; elements will be separated from other texts
     * with one newline character by default.
     */
    const val FROM_HTML_SEPARATOR_LINE_BREAK_DIV = 0x00000010

    /**
     * Flag indicating that texts inside &lt;blockquote&gt; elements will be separated from other
     * texts with one newline character by default.
     */
    const val FROM_HTML_SEPARATOR_LINE_BREAK_BLOCKQUOTE = 0x00000020

    /**
     * Flag indicating that CSS color values should be used instead of those defined in
     * [Color].
     */
    const val FROM_HTML_OPTION_USE_CSS_COLORS = 0x00000100
    private const val supportBlackSpace = true

    /**
     * Flags for [.fromHtml]: Separate block-level
     * elements with blank lines (two newline characters) in between. This is the legacy behavior
     * prior to N.
     */
    const val FROM_HTML_MODE_LEGACY = 0x00000000

    /**
     * Flags for [.fromHtml]: Separate block-level
     * elements with line breaks (single newline character) in between. This inverts the
     * [Spanned] to HTML string conversion done with the option
     */
    val FROM_HTML_MODE_COMPACT: Int = (FROM_HTML_SEPARATOR_LINE_BREAK_PARAGRAPH
        or FROM_HTML_SEPARATOR_LINE_BREAK_HEADING
        or FROM_HTML_SEPARATOR_LINE_BREAK_LIST_ITEM
        or FROM_HTML_SEPARATOR_LINE_BREAK_LIST
        or FROM_HTML_SEPARATOR_LINE_BREAK_DIV
        or FROM_HTML_SEPARATOR_LINE_BREAK_BLOCKQUOTE)

    /**
     * Returns displayable styled text from the provided HTML string. Any &lt;img&gt; tags in the
     * HTML will display as a generic replacement image which your program can then go through and
     * replace with real images.
     *
     *
     *
     * This uses TagSoup to handle real HTML, including all of the brokenness found in the wild.
     */
    var mOneChineseCharWidth = 0F

    @JvmOverloads
    fun fromHtml(
        source: String?, flags: Int, imageGetter: ImageGetter? = null,
        tagHandler: TagHandler? = null,
        oneChineseCharWidth: Float = 0F
    ): Spanned {
        mOneChineseCharWidth = oneChineseCharWidth
        val parser = Parser()
        try {
            parser.setProperty(Parser.schemaProperty, HtmlParser.schema)
        } catch (e: SAXNotRecognizedException) {
            // Should not happen.
            throw RuntimeException(e)
        } catch (e: SAXNotSupportedException) {
            // Should not happen.
            throw RuntimeException(e)
        }
        val handledBlackSpace = handlerBlackSpace(source)
        val converter = HtmlToSpannedConverter(handledBlackSpace, imageGetter, tagHandler, parser, flags)
        return converter.convert()
    }

    //空格字符处理
    private fun handlerBlackSpace(source: String?): String {
        if (source == null) return ""
        if (!supportBlackSpace) return source
        return source.replace("&nbsp;", " ")
    }

    /**
     * Lazy initialization holder for HTML parser. This class will
     * a) be preloaded by the zygote, or b) not loaded until absolutely
     * necessary.
     */
    private object HtmlParser {
        val schema: HTMLSchema = HTMLSchema()
    }
}