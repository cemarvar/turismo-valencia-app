package com.cmarcillovargas.turismovalencia.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

private val Heading = Regex("""^\s{0,3}#{1,6}\s+(.*)$""")
private val Bullet = Regex("""^(\s*)[*\-•]\s+(.*)$""")

// 1 negrita (**) · 2 negrita (__) · 3 cursiva (*) · 4 código · 5 texto de un enlace · 6 su dirección
private val Inline = Regex(
    """\*\*(.+?)\*\*|__(.+?)__|(?<![*\w])\*(?![\s*])(.+?)(?<![\s*])\*(?![*\w])|`([^`]+)`|\[([^\]]+)]\(([^)]+)\)""",
)

/**
 * Pasa a texto con estilo el Markdown básico con el que suele responder Gemini: negritas, cursivas,
 * listas con viñetas, títulos y enlaces (solo se deja el texto). Lo que no reconoce se muestra tal cual.
 */
fun parseChatMarkdown(text: String): AnnotatedString = buildAnnotatedString {
    text.trim().lines().forEachIndexed { index, raw ->
        if (index > 0) append('\n')
        val line = raw.trimEnd()
        val heading = Heading.matchEntire(line)
        val bullet = Bullet.matchEntire(line)
        when {
            heading != null -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { appendInline(heading.groupValues[1]) }
            bullet != null -> {
                append(bullet.groupValues[1])
                append("• ")
                appendInline(bullet.groupValues[2])
            }
            else -> appendInline(line)
        }
    }
}

private fun AnnotatedString.Builder.appendInline(line: String) {
    var last = 0
    for (match in Inline.findAll(line)) {
        append(line.substring(last, match.range.first))
        val groups = match.groupValues
        when {
            groups[1].isNotEmpty() -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(groups[1]) }
            groups[2].isNotEmpty() -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(groups[2]) }
            groups[3].isNotEmpty() -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(groups[3]) }
            groups[4].isNotEmpty() -> append(groups[4])
            else -> append(groups[5])
        }
        last = match.range.last + 1
    }
    append(line.substring(last))
}
