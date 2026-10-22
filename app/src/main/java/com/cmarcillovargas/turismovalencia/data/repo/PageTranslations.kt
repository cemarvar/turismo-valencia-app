package com.cmarcillovargas.turismovalencia.data.repo

import com.cmarcillovargas.turismovalencia.data.model.LanguageCode
import com.cmarcillovargas.turismovalencia.data.model.WebBlock
import com.cmarcillovargas.turismovalencia.data.model.WebItem
import com.cmarcillovargas.turismovalencia.data.model.WebPage
import com.cmarcillovargas.turismovalencia.data.model.WebTag
import org.json.JSONObject

/**
 * El texto de una página de la web en otro idioma (`assets/i18n/<va|en|fr>/pages/<Pagina>.json`; ver `docs/idiomas.md` y
 * `tools/i18n/pages.py`): la misma forma que la página en español, pero solo con los campos que se traducen. El título y la etiqueta
 * de la página salen de `content.json`; las fotos, los enlaces, los números de sección, el filtro de cada elemento y si una etiqueta
 * es «gratuita» salen de la página original.
 */
class PageTranslation(val lead: List<String>, val blocks: List<BlockTranslation>) {
    companion object {
        fun pathOf(language: LanguageCode, pageId: String) = "i18n/${language.name.lowercase()}/pages/$pageId.json"

        fun parse(json: String): PageTranslation {
            val root = JSONObject(json)
            return PageTranslation(root.optJSONArray("lead").strings(), root.optJSONArray("blocks").objects().map(::block))
        }

        private fun block(o: JSONObject): BlockTranslation {
            // `items` son elementos en un bloque de elementos y cadenas en uno de consejos.
            val items = o.optJSONArray("items")
            val isBullets = items != null && items.length() > 0 && items.get(0) is String
            return BlockTranslation(
                title = o.optStringOrNull("title"),
                filters = o.optJSONArray("filters").strings(),
                items = if (isBullets) emptyList() else items.objects().map(::item),
                rows = o.optJSONArray("rows").pairs(),
                bullets = if (isBullets) items.strings() else emptyList(),
            )
        }

        private fun item(o: JSONObject) = ItemTranslation(
            kicker = o.optStringOrNull("kicker"),
            title = o.optStringOrNull("title"),
            subtitle = o.optStringOrNull("subtitle"),
            body = o.optStringOrNull("body"),
            facts = o.optJSONArray("facts").pairs(),
            bullets = o.optJSONArray("bullets").strings(),
            bulletsTitle = o.optStringOrNull("bulletsTitle"),
            notes = o.optJSONArray("notes").pairs(),
            tags = o.optJSONArray("tags").strings(),
        )
    }
}

class BlockTranslation(
    val title: String?,
    val filters: List<String>,
    val items: List<ItemTranslation>,
    val rows: List<Pair<String, String>>,
    val bullets: List<String>,
)

class ItemTranslation(
    val kicker: String?,
    val title: String?,
    val subtitle: String?,
    val body: String?,
    val facts: List<Pair<String, String>>,
    val bullets: List<String>,
    val bulletsTitle: String?,
    val notes: List<Pair<String, String>>,
    val tags: List<String>,
)

/**
 * La página con los textos de [t] en lugar de los españoles, o `null` si no tiene su misma forma (otro número de bloques, elementos,
 * datos…): una traducción a medias no se enseña, se enseña la página en español (con su aviso). `textLanguage` queda en [language].
 */
fun WebPage.translated(t: PageTranslation, language: LanguageCode): WebPage? {
    if (t.lead.size != lead.size || t.blocks.size != blocks.size) return null
    val translatedBlocks = blocks.zip(t.blocks).map { (block, text) -> block.translated(text) ?: return null }
    return copy(lead = t.lead, blocks = translatedBlocks, textLanguage = language)
}

private fun WebBlock.translated(t: BlockTranslation): WebBlock? = when (this) {
    is WebBlock.Items -> {
        if (t.items.size != items.size || t.filters.size != filters.size || (title.isNotBlank() && t.title == null)) {
            null
        } else {
            val translatedItems = items.zip(t.items).map { (item, text) -> item.translated(text) ?: return null }
            WebBlock.Items(t.title ?: title, filters.zip(t.filters).map { (filter, label) -> filter.copy(label = label) }, translatedItems)
        }
    }
    is WebBlock.Facts -> if (t.rows.size != rows.size || (title.isNotBlank() && t.title == null)) null else WebBlock.Facts(t.title ?: title, t.rows)
    is WebBlock.Bullets -> if (t.bullets.size != items.size || (title.isNotBlank() && t.title == null)) null else WebBlock.Bullets(t.title ?: title, t.bullets)
}

private fun WebItem.translated(t: ItemTranslation): WebItem? {
    val sameShape = (kicker != null) == (t.kicker != null) && (title != null) == (t.title != null) &&
        (subtitle != null) == (t.subtitle != null) && (body != null) == (t.body != null) &&
        (bulletsTitle != null) == (t.bulletsTitle != null) &&
        facts.size == t.facts.size && bullets.size == t.bullets.size && notes.size == t.notes.size && tags.size == t.tags.size
    if (!sameShape) return null
    return copy(
        kicker = t.kicker,
        title = t.title,
        subtitle = t.subtitle,
        body = t.body,
        facts = t.facts,
        bullets = t.bullets,
        bulletsTitle = t.bulletsTitle,
        notes = t.notes,
        tags = tags.zip(t.tags).map { (tag, label) -> WebTag(label, tag.free) },
    )
}
