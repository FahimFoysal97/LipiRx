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
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
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

    // Theme colours are read here (composable scope) and handed down as plain values.
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
            // No app can open it; ignore rather than crash.
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
                    text = buildInline(block.text, colors),
                    style = TextStyle(
                        fontSize = headingSize(block.level),
                        lineHeight = headingSize(block.level) * 1.3f,
                        fontWeight = FontWeight.ExtraBold,
                        color = ink
                    ),
                    modifier = Modifier.padding(top = if (index == 0) 0.dp else 4.dp),
                    onUrl = openUrl
                )

                is MdBlock.Paragraph -> MdText(
                    text = buildInline(block.text, colors),
                    style = body,
                    onUrl = openUrl
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
                        text = buildInline(block.text, colors),
                        style = body,
                        modifier = Modifier.weight(1f),
                        onUrl = openUrl
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
                        text = buildInline(block.text, colors),
                        style = body,
                        modifier = Modifier.weight(1f),
                        onUrl = openUrl
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
                        text = buildInline(block.text, colors),
                        style = body.copy(color = muted, fontStyle = FontStyle.Italic),
                        modifier = Modifier.weight(1f),
                        onUrl = openUrl
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

private const val URL_TAG = "URL"

private fun headingSize(level: Int): TextUnit = when (level) {
    1 -> 17.sp
    2 -> 15.sp
    3 -> 14.sp
    else -> 13.sp
}

/** Plain Text when there are no links, tappable text when there are. */
@Composable
private fun MdText(
    text: AnnotatedString,
    style: TextStyle,
    modifier: Modifier = Modifier,
    onUrl: (String) -> Unit,
) {
    val hasLink = text.getStringAnnotations(URL_TAG, 0, text.length).isNotEmpty()
    if (hasLink) {
        ClickableText(
            text = text,
            style = style,
            modifier = modifier,
            onClick = { offset ->
                text.getStringAnnotations(URL_TAG, offset, offset).firstOrNull()?.let { onUrl(it.item) }
            }
        )
    } else {
        Text(text = text, style = style, modifier = modifier)
    }
}

private class InlineColors(
    val link: Color,
    val codeBg: Color,
    val codeFg: Color,
)

private fun buildInline(text: String, colors: InlineColors): AnnotatedString =
    buildAnnotatedString { appendInline(text, colors) }

// `code` | **bold** | ~~strike~~ | *italic* | [label](url) | bare https://url
private val inlineRegex = Regex(
    """(`[^`]+`)|(\*\*[^*]+\*\*)|(~~[^~]+~~)|(\*[^*\s][^*]*\*)|(\[[^\]]+]\([^)]+\))|(https?://[^\s)]+)"""
)

private fun AnnotatedString.Builder.appendInline(text: String, colors: InlineColors) {
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
                    appendInline(token.removeSurrounding("**"), colors)
                }

            token.startsWith("~~") ->
                withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                    appendInline(token.removeSurrounding("~~"), colors)
                }

            token.startsWith("*") ->
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    appendInline(token.removeSurrounding("*"), colors)
                }

            token.startsWith("[") -> {
                val label = token.substringBefore("](").removePrefix("[")
                val url = token.substringAfter("](").removeSuffix(")")
                appendLink(label, url, colors)
            }

            else -> {
                // Bare URL: keep trailing punctuation ("see https://x.y.") out of the link.
                val url = token.trimEnd('.', ',', ';', ':', '!', '?')
                appendLink(url, url, colors)
                append(token.substring(url.length))
            }
        }
        cursor = match.range.last + 1
    }
    if (cursor < text.length) append(text.substring(cursor))
}

private fun AnnotatedString.Builder.appendLink(label: String, url: String, colors: InlineColors) {
    pushStringAnnotation(URL_TAG, url)
    withStyle(SpanStyle(color = colors.link, textDecoration = TextDecoration.Underline)) {
        append(label)
    }
    pop()
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
    val blocks = mutableListOf<MdBlock>()
    val lines = source
        .replace("\r\n", "\n")
        .replace(lineBreakTagRegex, "\n")
        .lines()

    var inCode = false
    val code = StringBuilder()

    for (raw in lines) {
        // Fenced code block (```), kept verbatim until the closing fence.
        if (raw.trimStart().startsWith("```")) {
            if (inCode) {
                blocks.add(MdBlock.Code(code.toString().trimEnd()))
                code.clear()
                inCode = false
            } else {
                inCode = true
            }
            continue
        }
        if (inCode) {
            code.appendLine(raw)
            continue
        }

        val line = raw.trimEnd()
        val t = line.trim()
        if (t.isEmpty()) continue

        val indent = line.length - line.trimStart().length
        val level = (indent / 2).coerceIn(0, 3)

        val heading = headingRegex.find(t)
        val bullet = bulletRegex.find(t)
        val numbered = numberedRegex.find(t)

        when {
            ruleRegex.matches(t) -> blocks.add(MdBlock.Rule)
            heading != null -> blocks.add(
                MdBlock.Heading(heading.groupValues[1].length, heading.groupValues[2].trim())
            )
            t.startsWith(">") -> blocks.add(MdBlock.Quote(t.removePrefix(">").trim()))
            bullet != null -> blocks.add(MdBlock.Bullet(level, taskBox(bullet.groupValues[1])))
            numbered != null -> blocks.add(
                MdBlock.Numbered(numbered.groupValues[1], level, numbered.groupValues[2])
            )
            else -> blocks.add(MdBlock.Paragraph(t))
        }
    }

    // A code fence that was never closed still shows what was written.
    if (inCode && code.isNotEmpty()) blocks.add(MdBlock.Code(code.toString().trimEnd()))

    return blocks
}

/** "- [ ] item" / "- [x] item" show as ☐ / ☑. */
private fun taskBox(text: String): String = when {
    text.startsWith("[ ] ") -> "☐ " + text.drop(4)
    text.startsWith("[x] ", ignoreCase = true) -> "☑ " + text.drop(4)
    else -> text
}