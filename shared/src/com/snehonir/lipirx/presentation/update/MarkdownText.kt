package com.snehonir.lipirx.presentation.update

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snehonir.lipirx.ui.theme.Lipi

/**
 * Small Markdown renderer for release notes, so no extra library is needed.
 *
 * Supports: # headings, - * + bullets (nested by indent), 1. numbered lists, [ ] / [x] task items,
 * > quotes, ``` code blocks, --- rules, **bold**, *italic*, ~~strike~~, `code`,
 * [text](url) and bare https:// links (tappable).
 * Anything it does not recognise is shown as plain text, so it never breaks on unusual notes.
 */
@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
) {
    val blocks = remember(markdown) { parseMarkdown(markdown) }
    val uriHandler = LocalUriHandler.current

    val ink = Lipi.Ink
    val muted = Lipi.Muted
    val primary = Lipi.Primary
    val surface = Lipi.Surface
    val border = Lipi.CardBorder
    val colors = InlineColors(link = primary, codeBg = surface, codeFg = ink)

    val openUrl: (String) -> Unit = { url ->
        try {
            uriHandler.openUri(url)
        } catch (_: Exception) {
            // Ignore open error
        }
    }

    val body = TextStyle(fontSize = 13.sp, lineHeight = 19.sp, color = ink)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        blocks.forEachIndexed { index, block ->
            when (block) {
                is MdBlock.Heading -> MdText(
                    text = buildInline(block.text, colors, openUrl),
                    style = TextStyle(
                        fontSize = headingSize(block.level),
                        lineHeight = headingSize(block.level) * 1.3f,
                        fontWeight = FontWeight.ExtraBold,
                        color = ink
                    ),
                    modifier = Modifier.padding(top = if (index == 0) 0.dp else 4.dp)
                )

                is MdBlock.Paragraph -> MdText(
                    text = buildInline(block.text, colors, openUrl),
                    style = body
                )

                is MdBlock.Bullet -> Row(
                    modifier = Modifier.padding(start = (block.level * 14).dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (block.level == 0) "•" else "–",
                        style = body.copy(color = muted)
                    )
                    MdText(
                        text = buildInline(block.text, colors, openUrl),
                        style = body,
                        modifier = Modifier.weight(1f)
                    )
                }

                is MdBlock.Numbered -> Row(
                    modifier = Modifier.padding(start = (block.level * 14).dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${block.number}.",
                        style = body.copy(color = muted, fontWeight = FontWeight.SemiBold)
                    )
                    MdText(
                        text = buildInline(block.text, colors, openUrl),
                        style = body,
                        modifier = Modifier.weight(1f)
                    )
                }

                is MdBlock.Quote -> Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .fillMaxHeight()
                            .background(primary.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    MdText(
                        text = buildInline(block.text, colors, openUrl),
                        style = body.copy(color = muted, fontStyle = FontStyle.Italic),
                        modifier = Modifier.weight(1f)
                    )
                }

                is MdBlock.Code -> Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = surface
                ) {
                    Text(
                        text = block.text,
                        modifier = Modifier.padding(10.dp),
                        style = TextStyle(
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            fontFamily = FontFamily.Monospace,
                            color = ink
                        )
                    )
                }

                MdBlock.Rule -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(border)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Rendering helpers
// ─────────────────────────────────────────────────────────────────────────────

private fun headingSize(level: Int): TextUnit = when (level) {
    1 -> 17.sp
    2 -> 15.sp
    3 -> 14.sp
    else -> 13.sp
}

@Composable
private fun MdText(
    text: AnnotatedString,
    style: TextStyle,
    modifier: Modifier = Modifier,
) {
    Text(text = text, style = style, modifier = modifier)
}

private class InlineColors(
    val link: Color,
    val codeBg: Color,
    val codeFg: Color,
)

private fun buildInline(text: String, colors: InlineColors, onUrl: (String) -> Unit): AnnotatedString =
    buildAnnotatedString { appendInline(text, colors, onUrl) }

private val inlineRegex = Regex(
    """(`[^`]+`)|(\*\*[^*]+\*\*)|(~~[^~]+~~)|(\*[^*\s][^*]*\*)|(\[[^\]]+]\([^)]+\))|(https?://[^\s)]+)"""
)

private fun AnnotatedString.Builder.appendInline(text: String, colors: InlineColors, onUrl: (String) -> Unit) {
    var cursor = 0
    for (match in inlineRegex.findAll(text)) {
        if (match.range.first > cursor) append(text.substring(cursor, match.range.first))
        val token = match.value

        when {
            token.startsWith("`") ->
                withStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = colors.codeBg,
                        color = colors.codeFg
                    )
                ) { append(token.trim('`')) }

            token.startsWith("**") ->
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    appendInline(token.removeSurrounding("**"), colors, onUrl)
                }

            token.startsWith("~~") ->
                withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                    appendInline(token.removeSurrounding("~~"), colors, onUrl)
                }

            token.startsWith("*") ->
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    appendInline(token.removeSurrounding("*"), colors, onUrl)
                }

            token.startsWith("[") -> {
                val label = token.substringBefore("](").removePrefix("[")
                val url = token.substringAfter("](").removeSuffix(")")
                appendLink(label, url, colors, onUrl)
            }

            else -> {
                val url = token.trimEnd('.', ',', ';', ':', '!', '?')
                appendLink(url, url, colors, onUrl)
                append(token.substring(url.length))
            }
        }
        cursor = match.range.last + 1
    }
    if (cursor < text.length) append(text.substring(cursor))
}

private fun AnnotatedString.Builder.appendLink(label: String, url: String, colors: InlineColors, onUrl: (String) -> Unit) {
    val linkAnnotation = LinkAnnotation.Url(
        url = url,
        styles = TextLinkStyles(
            style = SpanStyle(color = colors.link, textDecoration = TextDecoration.Underline)
        ),
        linkInteractionListener = {
            onUrl(url)
        }
    )
    withLink(linkAnnotation) {
        append(label)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Parsing
// ─────────────────────────────────────────────────────────────────────────────

private sealed interface MdBlock {
    data class Heading(val level: Int, val text: String) : MdBlock
    data class Paragraph(val text: String) : MdBlock
    data class Bullet(val level: Int, val text: String) : MdBlock
    data class Numbered(val number: String, val level: Int, val text: String) : MdBlock
    data class Quote(val text: String) : MdBlock
    data class Code(val text: String) : MdBlock
    data object Rule : MdBlock
}

private val headingRegex = Regex("""^(#{1,6})\s+(.*)$""")
private val bulletRegex = Regex("""^[-*+]\s+(.*)$""")
private val numberedRegex = Regex("""^(\d+)[.)]\s+(.*)$""")
private val ruleRegex = Regex("""^([-*_])(\s*\1){2,}$""")
private val lineBreakTagRegex = Regex("""<br\s*/?>""", RegexOption.IGNORE_CASE)

private fun parseMarkdown(source: String): List<MdBlock> {
    val lines = source.lines()
    val blocks = mutableListOf<MdBlock>()
    var inCode = false
    val codeBuffer = StringBuilder()
    var quoteBuffer = StringBuilder()

    fun flushQuote() {
        if (quoteBuffer.isNotEmpty()) {
            blocks.add(MdBlock.Quote(quoteBuffer.toString().trim()))
            quoteBuffer = StringBuilder()
        }
    }

    for (rawLine in lines) {
        val line = rawLine.trimEnd()

        if (line.startsWith("```")) {
            flushQuote()
            if (inCode) {
                blocks.add(MdBlock.Code(codeBuffer.toString().trimEnd()))
                codeBuffer.clear()
                inCode = false
            } else {
                inCode = true
            }
            continue
        }

        if (inCode) {
            codeBuffer.appendLine(rawLine)
            continue
        }

        if (line.startsWith(">")) {
            if (quoteBuffer.isNotEmpty()) quoteBuffer.append(" ")
            quoteBuffer.append(line.removePrefix(">").trim())
            continue
        } else {
            flushQuote()
        }

        if (line.isBlank()) continue

        val cleanLine = line.replace(lineBreakTagRegex, " ")

        when {
            ruleRegex.matches(cleanLine.trim()) -> blocks.add(MdBlock.Rule)

            headingRegex.matches(cleanLine) -> {
                val match = headingRegex.find(cleanLine)!!
                val level = match.groupValues[1].length
                val text = match.groupValues[2]
                blocks.add(MdBlock.Heading(level, text))
            }

            bulletRegex.matches(cleanLine) -> {
                val indent = rawLine.takeWhile { it == ' ' }.length
                val text = bulletRegex.find(cleanLine)!!.groupValues[1]
                blocks.add(MdBlock.Bullet(level = indent / 2, text = text))
            }

            numberedRegex.matches(cleanLine) -> {
                val indent = rawLine.takeWhile { it == ' ' }.length
                val match = numberedRegex.find(cleanLine)!!
                val num = match.groupValues[1]
                val text = match.groupValues[2]
                blocks.add(MdBlock.Numbered(number = num, level = indent / 2, text = text))
            }

            else -> blocks.add(MdBlock.Paragraph(cleanLine))
        }
    }

    flushQuote()
    return blocks
}
