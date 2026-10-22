package com.cmarcillovargas.turismovalencia.data.repo

import com.cmarcillovargas.turismovalencia.data.model.WebBlock
import com.cmarcillovargas.turismovalencia.data.model.WebFamily
import com.cmarcillovargas.turismovalencia.data.model.WebFilter
import com.cmarcillovargas.turismovalencia.data.model.WebItem
import com.cmarcillovargas.turismovalencia.data.model.WebPage
import com.cmarcillovargas.turismovalencia.data.model.WebTag
import org.json.JSONObject

/** De `assets/web/pages/<Pagina>.json` (el español importado de la web) al modelo. Kotlin puro, para probarlo en la JVM. */
internal object WebPageParser {
    fun parse(id: String, json: JSONObject): WebPage = WebPage(
        id = id,
        family = WebFamily.fromKey(json.getString("family")),
        title = json.getString("title"),
        eyebrow = json.optStringOrNull("eyebrow"),
        lead = json.optJSONArray("lead").strings(),
        hero = json.optStringOrNull("hero"),
        blocks = json.getJSONArray("blocks").objects().map(::block),
    )

    private fun block(b: JSONObject): WebBlock = when (b.getString("type")) {
        "facts" -> WebBlock.Facts(b.optString("title"), b.getJSONArray("rows").pairs())
        "bullets" -> WebBlock.Bullets(b.optString("title"), b.getJSONArray("items").strings())
        else -> WebBlock.Items(
            title = b.optString("title"),
            filters = b.optJSONArray("filters")?.objects()?.map { WebFilter(it.getString("id"), it.getString("label")) }.orEmpty(),
            items = b.getJSONArray("items").objects().map(::item),
        )
    }

    private fun item(o: JSONObject) = WebItem(
        num = o.optStringOrNull("num"),
        kicker = o.optStringOrNull("kicker"),
        title = o.optStringOrNull("title"),
        subtitle = o.optStringOrNull("subtitle"),
        body = o.optStringOrNull("body"),
        facts = o.optJSONArray("facts").pairs(),
        bullets = o.optJSONArray("bullets").strings(),
        bulletsTitle = o.optStringOrNull("bulletsTitle"),
        notes = o.optJSONArray("notes").pairs(),
        tags = o.optJSONArray("tags")?.objects()?.map { WebTag(it.getString("l"), it.optBoolean("free")) }.orEmpty(),
        image = o.optStringOrNull("image"),
        url = o.optStringOrNull("url"),
        category = o.optStringOrNull("cat"),
    )
}
