package com.ywh.dm.dm_html

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.text.Editable
import com.ywh.dm.dm_html.Html.ImageGetter
import android.text.Html.TagHandler
import android.text.Layout
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextUtils
import android.text.style.AlignmentSpan
import android.text.style.BackgroundColorSpan
import android.text.style.BulletSpan
import android.text.style.ForegroundColorSpan
import android.text.style.ImageSpan
import android.text.style.LeadingMarginSpan
import android.text.style.ParagraphStyle
import android.text.style.QuoteSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.SubscriptSpan
import android.text.style.SuperscriptSpan
import android.text.style.TypefaceSpan
import android.text.style.URLSpan
import android.text.style.UnderlineSpan
import org.ccil.cowan.tagsoup.Parser
import org.xml.sax.Attributes
import org.xml.sax.ContentHandler
import org.xml.sax.InputSource
import org.xml.sax.Locator
import org.xml.sax.SAXException
import org.xml.sax.XMLReader
import java.io.IOException
import java.io.StringReader
import java.util.regex.Pattern

/**
 * Create by yangwenhao on 2022/9/16
 */
internal class HtmlToSpannedConverter(
    private val mSource: String, imageGetter: ImageGetter?,
    tagHandler: TagHandler?, parser: Parser, flags: Int
) : ContentHandler {
    private val mReader: XMLReader
    private var mSpannableStringBuilder: SpannableStringBuilder = SpannableStringBuilder()
    private val mImageGetter: ImageGetter?
    private val mTagHandler: TagHandler?
    private val mFlags: Int

    companion object {
        private val HEADING_SIZES = floatArrayOf(
            1.5f, 1.4f, 1.3f, 1.2f, 1.1f, 1f
        )
        private var sTextAlignPattern: Pattern? = null
        private var sForegroundColorPattern: Pattern? = null
        private var sBackgroundColorPattern: Pattern? = null
        private var sTextDecorationPattern: Pattern? = null
        private var sTextIndentPattern: Pattern? = null

        /**
         * Name-value mapping of HTML/CSS colors which have different values in [Color].
         */
        private var sColorMap: HashMap<String, Int>? = null
        private val textAlignPattern: Pattern?
            get() {
                if (sTextAlignPattern == null) {
                    sTextAlignPattern = Pattern.compile("(?:\\s+|\\A)text-align\\s*:\\s*(\\S*)\\b")
                }
                return sTextAlignPattern
            }
        private val foregroundColorPattern: Pattern?
            get() {
                if (sForegroundColorPattern == null) {
                    sForegroundColorPattern = Pattern.compile(
                        "(?:\\s+|\\A)color\\s*:\\s*(\\S*)\\b"
                    )
                }
                return sForegroundColorPattern
            }
        private val backgroundColorPattern: Pattern?
            get() {
                if (sBackgroundColorPattern == null) {
                    sBackgroundColorPattern = Pattern.compile(
                        "(?:\\s+|\\A)background(?:-color)?\\s*:\\s*(\\S*)\\b"
                    )
                }
                return sBackgroundColorPattern
            }
        private val textDecorationPattern: Pattern?
            get() {
                if (sTextDecorationPattern == null) {
                    sTextDecorationPattern = Pattern.compile(
                        "(?:\\s+|\\A)text-decoration\\s*:\\s*(\\S*)\\b"
                    )
                }
                return sTextDecorationPattern
            }
        private val textIndentPattern: Pattern?
            get() {
                if (sTextIndentPattern == null) {
                    sTextIndentPattern = Pattern.compile(
                        "(?:\\s+|\\A)text-indent\\s*:\\s*(\\S*)\\b"
                    )
                }
                return sTextIndentPattern
            }

        private fun appendNewlines(text: Editable, minNewline: Int) {
            val len = text.length
            if (len == 0) {
                return
            }
            var existingNewlines = 0
            var i = len - 1
            while (i >= 0 && text[i] == '\n') {
                existingNewlines++
                i--
            }
            for (j in existingNewlines until minNewline) {
                text.append("\n")
            }
        }

        private fun startBlockElement(text: Editable, attributes: Attributes, margin: Int) {
            if (margin > 0) {
                appendNewlines(text, margin)
                start(text, Newline(margin))
            }
            val style = attributes.getValue("", "style")
            if (style != null) {
                val m = textAlignPattern!!.matcher(style)
                if (m.find()) {
                    val alignment = m.group(1)
                    if (alignment.equals("left", ignoreCase = true)) {
                        start(text, Alignment(Layout.Alignment.ALIGN_NORMAL))
                    } else if (alignment.equals("center", ignoreCase = true)) {
                        start(text, Alignment(Layout.Alignment.ALIGN_CENTER))
                    } else if (alignment.equals("right", ignoreCase = true)) {
                        start(text, Alignment(Layout.Alignment.ALIGN_OPPOSITE))
                    }
                }
            }
        }

        private fun endBlockElement(text: Editable) {
            val newLine = getLast(text, Newline::class.java)
            newLine?.let {
                appendNewlines(text, it.mNumNewlines)
                text.removeSpan(newLine)
            }
            val alignment = getLast(text, Alignment::class.java)
            if (alignment != null) {
                setSpanFromMark(text, alignment, AlignmentSpan.Standard(alignment.mAlignment))
            }
        }

        private fun handleBr(text: Editable) {
            text.append('\n')
        }

        private fun endLi(text: Editable) {
            endLineCssStyle(text)
            endBlockElement(text)
            end(text, Bullet::class.java, BulletSpan())
        }

        private fun endBlockquote(text: Editable) {
            endBlockElement(text)
            end(text, Blockquote::class.java, QuoteSpan())
        }

        private fun endHeading(text: Editable) {
            // RelativeSizeSpan and StyleSpan are CharacterStyles
            // Their ranges should not include the newlines at the end
            val heading = getLast(text, Heading::class.java)
            heading?.let {
                setSpanFromMark(
                    text, it, RelativeSizeSpan(HEADING_SIZES[it.mLevel]),
                    StyleSpan(Typeface.BOLD)
                )
            }

            endBlockElement(text)
        }

        private fun <T> getLast(text: Spanned, kind: Class<T>): T? {
            /*
         * This knows that the last returned object from getSpans()
         * will be the most recently added.
         */
            val objs = text.getSpans(0, text.length, kind)
            return if (objs.isEmpty()) {
                null
            } else {
                objs[objs.size - 1]
            }
        }

        private fun setSpanFromMark(text: Spannable, mark: Any, vararg spans: Any) {
            val where = text.getSpanStart(mark)
            text.removeSpan(mark)
            val len = text.length
            if (where != len) {
                for (span in spans) {
                    text.setSpan(span, where, len, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }
        }

        private fun start(text: Editable, mark: Any) {
            val len = text.length
            text.setSpan(mark, len, len, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
        }

        private fun end(text: Editable, kind: Class<*>, repl: Any) {
            val obj = getLast(text, kind)
            if (obj != null) {
                setSpanFromMark(text, obj, repl)
            }
        }

        private fun endLineCssStyle(text: Editable) {
            val strikethrough = getLast(text, Strikethrough::class.java)
            strikethrough?.let {
                setSpanFromMark(text, it, StrikethroughSpan())
            }
            val background = getLast(text, Background::class.java)
            background?.let {
                setSpanFromMark(text, it, BackgroundColorSpan(it.mBackgroundColor))
            }
            val foreground = getLast(text, Foreground::class.java)
            foreground?.let {
                setSpanFromMark(text, it, ForegroundColorSpan(it.mForegroundColor))
            }
            val underline = getLast(text, Underline::class.java)
            underline?.let {
                setSpanFromMark(text, it, UnderlineSpan())
            }
            val leadingMargin = getLast(text, LeadingMargin::class.java)
            leadingMargin?.let {
                setSpanFromMark(text, it, LeadingMarginSpan.Standard(it.em * 36, 0))
            }
        }

        private fun startImg(text: Editable, attributes: Attributes, img: ImageGetter?) {
            val src = attributes.getValue("", "src")
            var d: Drawable? = null
            if (img != null) {
                d = img.getDrawable(src) ?: img.defaultDrawable()
            }
            if (d != null) {
                if (img == null || img.drawableWidth() == -1) {
                    d.setBounds(0, 0, d.intrinsicWidth, d.intrinsicHeight)
                } else {
                    val imageGetterWidth = img.drawableWidth()
                    val width = d.intrinsicWidth
                    val height = d.intrinsicHeight
                    if (height > 0 && width > 0) {
                        d.setBounds(0, 0, imageGetterWidth, (imageGetterWidth * height.toFloat() / width).toInt())
                    } else {
                        d.setBounds(0, 0, d.intrinsicWidth, d.intrinsicHeight)
                    }
                }
                val len = text.length
                text.append("\uFFFC")
                text.setSpan(
                    ImageSpan(d, src), len, text.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }

        private fun endFont(text: Editable) {
            val font = getLast(text, Font::class.java)
            font?.let {
                setSpanFromMark(text, it, TypefaceSpan(it.mFace))
            }
            val foreground = getLast(text, Foreground::class.java)
            foreground?.let {
                setSpanFromMark(
                    text, it,
                    ForegroundColorSpan(it.mForegroundColor)
                )
            }
        }

        private fun startA(text: Editable, attributes: Attributes) {
            val href = attributes.getValue("", "href")
            start(text, Href(href))
        }

        private fun endA(text: Editable) {
            val href = getLast(text, Href::class.java)
            href?.let {
                if (it.mHref != null) {
                    setSpanFromMark(text, it, URLSpan(it.mHref))
                }
            }
        }

        init {
            sColorMap = HashMap<String, Int>().apply {
                this["darkgray"] = -0x565657
                this["gray"] = -0x7f7f80
                this["lightgray"] = -0x2c2c2d
                this["darkgrey"] = -0x565657
                this["grey"] = -0x7f7f80
                this["lightgrey"] = -0x2c2c2d
                this["green"] = -0xff8000
            }

        }
    }

    fun convert(): Spanned {
        mReader.contentHandler = this
        try {
            mReader.parse(InputSource(StringReader(mSource)))
        } catch (e: IOException) {
            // We are reading from a string. There should not be IO problems.
            throw RuntimeException(e)
        } catch (e: SAXException) {
            // TagSoup doesn't throw parse exceptions.
            throw RuntimeException(e)
        }
        // Fix flags and range for paragraph-type markup.
        val obj = mSpannableStringBuilder.getSpans(0, mSpannableStringBuilder.length, ParagraphStyle::class.java)
        for (i in obj.indices) {
            val start = mSpannableStringBuilder.getSpanStart(obj[i])
            var end = mSpannableStringBuilder.getSpanEnd(obj[i])
            // If the last line of the range is blank, back off by one.
            if (end - 2 >= 0) {
                if (mSpannableStringBuilder[end - 1] == '\n' &&
                    mSpannableStringBuilder[end - 2] == '\n'
                ) {
                    end--
                }
            }
            if (end == start) {
                mSpannableStringBuilder.removeSpan(obj[i])
            } else {
                mSpannableStringBuilder.setSpan(obj[i], start, end, Spannable.SPAN_PARAGRAPH)
            }
        }
        return mSpannableStringBuilder
    }

    /**
     * <p></p>
    <strong></strong>
    <em></em>
    <span></span>
    <img />
    <br>
     */
    private fun handleStartTag(tag: String, attributes: Attributes) {
        if (tag.equals("br", ignoreCase = true)) {
            // We don't need to handle this. TagSoup will ensure that there's a </br> for each <br>
            // so we can safely emit the linebreaks when we handle the close tag.
        } else if (tag.equals("p", ignoreCase = true)) {
            startBlockElement(mSpannableStringBuilder, attributes, marginParagraph)
            startLineCssStyle(mSpannableStringBuilder, attributes)
        } else if (tag.equals("ul", ignoreCase = true)) {
            startBlockElement(mSpannableStringBuilder, attributes, marginList)
        } else if (tag.equals("li", ignoreCase = true)) {
            startLi(mSpannableStringBuilder, attributes)
        } else if (tag.equals("div", ignoreCase = true)) {
            startBlockElement(mSpannableStringBuilder, attributes, marginDiv)
        } else if (tag.equals("span", ignoreCase = true)) {
            startLineCssStyle(mSpannableStringBuilder, attributes)
        } else if (tag.equals("strong", ignoreCase = true)) {
            start(mSpannableStringBuilder, Bold())
        } else if (tag.equals("b", ignoreCase = true)) {
            start(mSpannableStringBuilder, Bold())
        } else if (tag.equals("em", ignoreCase = true)) {
            start(mSpannableStringBuilder, Italic())
        } else if (tag.equals("cite", ignoreCase = true)) {
            start(mSpannableStringBuilder, Italic())
        } else if (tag.equals("dfn", ignoreCase = true)) {
            start(mSpannableStringBuilder, Italic())
        } else if (tag.equals("i", ignoreCase = true)) {
            start(mSpannableStringBuilder, Italic())
        } else if (tag.equals("big", ignoreCase = true)) {
            start(mSpannableStringBuilder, Big())
        } else if (tag.equals("small", ignoreCase = true)) {
            start(mSpannableStringBuilder, Small())
        } else if (tag.equals("font", ignoreCase = true)) {
            startFont(mSpannableStringBuilder, attributes)
        } else if (tag.equals("blockquote", ignoreCase = true)) {
            startBlockquote(mSpannableStringBuilder, attributes)
        } else if (tag.equals("tt", ignoreCase = true)) {
            start(mSpannableStringBuilder, Monospace())
        } else if (tag.equals("a", ignoreCase = true)) {
            startA(mSpannableStringBuilder, attributes)
        } else if (tag.equals("u", ignoreCase = true)) {
            start(mSpannableStringBuilder, Underline())
        } else if (tag.equals("del", ignoreCase = true)) {
            start(mSpannableStringBuilder, Strikethrough())
        } else if (tag.equals("s", ignoreCase = true)) {
            start(mSpannableStringBuilder, Strikethrough())
        } else if (tag.equals("strike", ignoreCase = true)) {
            start(mSpannableStringBuilder, Strikethrough())
        } else if (tag.equals("sup", ignoreCase = true)) {
            start(mSpannableStringBuilder, Super())
        } else if (tag.equals("sub", ignoreCase = true)) {
            start(mSpannableStringBuilder, Sub())
        } else if (tag.length == 2 && tag[0].lowercaseChar() == 'h' && tag[1] >= '1' && tag[1] <= '6') {
            startHeading(mSpannableStringBuilder, attributes, tag[1] - '1')
        } else if (tag.equals("img", ignoreCase = true)) {
            mSpannableStringBuilder.append("\n")
            startImg(mSpannableStringBuilder, attributes, mImageGetter)
            mSpannableStringBuilder.append("\n")
        } else mTagHandler?.handleTag(true, tag, mSpannableStringBuilder, mReader)
    }

    private fun handleEndTag(tag: String) {
        if (tag.equals("br", ignoreCase = true)) {
            handleBr(mSpannableStringBuilder)
        } else if (tag.equals("p", ignoreCase = true)) {
            endLineCssStyle(mSpannableStringBuilder)
            endBlockElement(mSpannableStringBuilder)
        } else if (tag.equals("ul", ignoreCase = true)) {
            endBlockElement(mSpannableStringBuilder)
        } else if (tag.equals("li", ignoreCase = true)) {
            endLi(mSpannableStringBuilder)
        } else if (tag.equals("div", ignoreCase = true)) {
            endBlockElement(mSpannableStringBuilder)
        } else if (tag.equals("span", ignoreCase = true)) {
            endLineCssStyle(mSpannableStringBuilder)
        } else if (tag.equals("strong", ignoreCase = true)) {
            end(mSpannableStringBuilder, Bold::class.java, StyleSpan(Typeface.BOLD))
        } else if (tag.equals("b", ignoreCase = true)) {
            end(mSpannableStringBuilder, Bold::class.java, StyleSpan(Typeface.BOLD))
        } else if (tag.equals("em", ignoreCase = true)) {
            end(mSpannableStringBuilder, Italic::class.java, StyleSpan(Typeface.ITALIC))
        } else if (tag.equals("cite", ignoreCase = true)) {
            end(mSpannableStringBuilder, Italic::class.java, StyleSpan(Typeface.ITALIC))
        } else if (tag.equals("dfn", ignoreCase = true)) {
            end(mSpannableStringBuilder, Italic::class.java, StyleSpan(Typeface.ITALIC))
        } else if (tag.equals("i", ignoreCase = true)) {
            end(mSpannableStringBuilder, Italic::class.java, StyleSpan(Typeface.ITALIC))
        } else if (tag.equals("big", ignoreCase = true)) {
            end(mSpannableStringBuilder, Big::class.java, RelativeSizeSpan(1.25f))
        } else if (tag.equals("small", ignoreCase = true)) {
            end(mSpannableStringBuilder, Small::class.java, RelativeSizeSpan(0.8f))
        } else if (tag.equals("font", ignoreCase = true)) {
            endFont(mSpannableStringBuilder)
        } else if (tag.equals("blockquote", ignoreCase = true)) {
            endBlockquote(mSpannableStringBuilder)
        } else if (tag.equals("tt", ignoreCase = true)) {
            end(mSpannableStringBuilder, Monospace::class.java, TypefaceSpan("monospace"))
        } else if (tag.equals("a", ignoreCase = true)) {
            endA(mSpannableStringBuilder)
        } else if (tag.equals("u", ignoreCase = true)) {
            end(mSpannableStringBuilder, Underline::class.java, UnderlineSpan())
        } else if (tag.equals("del", ignoreCase = true)) {
            end(mSpannableStringBuilder, Strikethrough::class.java, StrikethroughSpan())
        } else if (tag.equals("s", ignoreCase = true)) {
            end(mSpannableStringBuilder, Strikethrough::class.java, StrikethroughSpan())
        } else if (tag.equals("strike", ignoreCase = true)) {
            end(mSpannableStringBuilder, Strikethrough::class.java, StrikethroughSpan())
        } else if (tag.equals("sup", ignoreCase = true)) {
            end(mSpannableStringBuilder, Super::class.java, SuperscriptSpan())
        } else if (tag.equals("sub", ignoreCase = true)) {
            end(mSpannableStringBuilder, Sub::class.java, SubscriptSpan())
        } else if (tag.length == 2 && tag[0].lowercaseChar() == 'h' && tag[1] >= '1' && tag[1] <= '6') {
            endHeading(mSpannableStringBuilder)
        } else mTagHandler?.handleTag(false, tag, mSpannableStringBuilder, mReader)
    }

    private val marginParagraph: Int
        get() = getMargin(Html.FROM_HTML_SEPARATOR_LINE_BREAK_PARAGRAPH)
    private val marginHeading: Int
        get() = getMargin(Html.FROM_HTML_SEPARATOR_LINE_BREAK_HEADING)
    private val marginListItem: Int
        get() = getMargin(Html.FROM_HTML_SEPARATOR_LINE_BREAK_LIST_ITEM)
    private val marginList: Int
        get() = getMargin(Html.FROM_HTML_SEPARATOR_LINE_BREAK_LIST)
    private val marginDiv: Int
        get() = getMargin(Html.FROM_HTML_SEPARATOR_LINE_BREAK_DIV)
    private val marginBlockquote: Int
        get() = getMargin(Html.FROM_HTML_SEPARATOR_LINE_BREAK_BLOCKQUOTE)

    /**
     * Returns the minimum number of newline characters needed before and after a given block-level
     * element.
     *
     * @param flag the corresponding option flag defined in [Html] of a block-level element
     */
    private fun getMargin(flag: Int): Int {
        return if (flag and mFlags != 0) {
            1
        } else 2
    }

    private fun startLi(text: Editable, attributes: Attributes) {
        startBlockElement(text, attributes, marginListItem)
        start(text, Bullet())
        startLineCssStyle(text, attributes)
    }

    private fun startBlockquote(text: Editable, attributes: Attributes) {
        startBlockElement(text, attributes, marginBlockquote)
        start(text, Blockquote())
    }

    private fun startHeading(text: Editable, attributes: Attributes, level: Int) {
        startBlockElement(text, attributes, marginHeading)
        start(text, Heading(level))
    }

    //行内样式
    private fun startLineCssStyle(text: Editable, attributes: Attributes) {
        val style = attributes.getValue("", "style")
        if (style != null) {
            val styles = style.split(";".toRegex()).toTypedArray()
            for (str in styles) {
                var m = foregroundColorPattern!!.matcher(str)
                if (m.find()) {
                    val c = getHtmlColor(m.group(1))
                    if (c != -1) {
                        start(text, Foreground(c or -0x1000000))
                    }
                    continue
                }
                m = backgroundColorPattern!!.matcher(str)
                if (m.find()) {
                    val c = getHtmlColor(m.group(1))
                    if (c != -1) {
                        start(text, Background(c or -0x1000000))
                    }
                    continue
                }
                m = textDecorationPattern!!.matcher(str)
                if (m.find()) {
                    val textDecoration = m.group(1)
                    if (textDecoration.equals("line-through", ignoreCase = true)) {
                        start(text, Strikethrough())
                    } else if (textDecoration.equals("underline", ignoreCase = true)) {
                        start(text, Underline())
                    }
                    continue
                }

                m = textIndentPattern!!.matcher(str)
                if (m.find()) {
                    val textIndent = m.group(1)
                    if (textIndent != null && textIndent.endsWith("em")) {
                        val textIndentEmValue = textIndent.substring(0, textIndent.indexOf("em"))
                        val textIndentEmValueInt = textIndentEmValue.toIntOrNull() ?: 0
                        start(text, LeadingMargin(textIndentEmValueInt))
                    }
                    continue
                }
            }
        }
    }

    private fun startFont(text: Editable, attributes: Attributes) {
        val color = attributes.getValue("", "color")
        val face = attributes.getValue("", "face")
        if (!TextUtils.isEmpty(color)) {
            val c = getHtmlColor(color)
            if (c != -1) {
                start(text, Foreground(c or -0x1000000))
            }
        }
        if (!TextUtils.isEmpty(face)) {
            start(text, Font(face))
        }
    }

    private fun getHtmlColor(color: String?): Int {
        if (color == null) return -1
        if (mFlags and Html.FROM_HTML_OPTION_USE_CSS_COLORS
            == Html.FROM_HTML_OPTION_USE_CSS_COLORS
        ) {
            val i = sColorMap!![color.lowercase()]
            if (i != null) {
                return i
            }
        }
        // If |color| is the name of a color, pass it to Color to convert it. Otherwise,
        // it may start with "#", "0", "0x", "+", or a digit. All of these cases are
        // handled below by XmlUtils. (Note that parseColor accepts colors starting
        // with "#", but it treats them differently from XmlUtils.)
        return if (Character.isLetter(color[0])) {
            try {
                Color.parseColor(color)
            } catch (e: IllegalArgumentException) {
                -1
            }
        } else try {
            Color.parseColor(color)
            //            return XmlUtils.convertValueToInt(color, -1);
        } catch (nfe: NumberFormatException) {
            -1
        }
    }

    override fun setDocumentLocator(locator: Locator) {}

    @Throws(SAXException::class)
    override fun startDocument() {
    }

    @Throws(SAXException::class)
    override fun endDocument() {
    }

    @Throws(SAXException::class)
    override fun startPrefixMapping(prefix: String, uri: String) {
    }

    @Throws(SAXException::class)
    override fun endPrefixMapping(prefix: String) {
    }

    @Throws(SAXException::class)
    override fun startElement(uri: String, localName: String, qName: String, attributes: Attributes) {
        handleStartTag(localName, attributes)
    }

    @Throws(SAXException::class)
    override fun endElement(uri: String, localName: String, qName: String) {
        handleEndTag(localName)
    }

    @Throws(SAXException::class)
    override fun characters(ch: CharArray, start: Int, length: Int) {
        val sb = StringBuilder()
        /*
         * Ignore whitespace that immediately follows other whitespace;
         * newlines count as spaces.
         */for (i in 0 until length) {
            val c = ch[i + start]
            if (c == ' ' || c == '\n') {
                var pred: Char
                var len = sb.length
                if (len == 0) {
                    len = mSpannableStringBuilder.length
                    pred = if (len == 0) {
                        '\n'
                    } else {
                        mSpannableStringBuilder[len - 1]
                    }
                } else {
                    pred = sb[len - 1]
                }
                if (pred != ' ' && pred != '\n') {
                    sb.append(' ')
                }
            } else {
                sb.append(c)
            }
        }
        mSpannableStringBuilder.append(sb)
    }

    @Throws(SAXException::class)
    override fun ignorableWhitespace(ch: CharArray, start: Int, length: Int) {
    }

    @Throws(SAXException::class)
    override fun processingInstruction(target: String, data: String) {
    }

    @Throws(SAXException::class)
    override fun skippedEntity(name: String) {
    }

    private class Bold
    private class Italic
    private class Underline
    private class Strikethrough
    private class Big
    private class Small
    private class Monospace
    private class Blockquote
    private class Super
    private class Sub
    private class Bullet
    private class Font(var mFace: String)
    private class Href(var mHref: String?)
    private class Foreground(val mForegroundColor: Int)
    private class Background(val mBackgroundColor: Int)
    private class Heading(val mLevel: Int)
    private class Newline(val mNumNewlines: Int)
    private class Alignment(val mAlignment: Layout.Alignment)
    private class LeadingMargin(val em: Int)

    init {
        mImageGetter = imageGetter
        mTagHandler = tagHandler
        mReader = parser
        mFlags = flags
    }
}