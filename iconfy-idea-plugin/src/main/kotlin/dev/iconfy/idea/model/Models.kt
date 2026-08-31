package dev.iconfy.idea.model

import kotlinx.serialization.Serializable

/** `GET /search` — `icons` are ready-to-insert `"prefix:name"` coordinates. */
@Serializable
data class SearchResult(
    val icons: List<String> = emptyList(),
    val total: Int = 0,
    val limit: Int = 0,
    val start: Int = 0,
)

/** One entry of `GET /collections` (keyed by prefix). */
@Serializable
data class CollectionInfo(
    val name: String = "",
    val total: Int = 0,
    val category: String = "",
    val palette: Boolean = false,
    val samples: List<String> = emptyList(),
)

/** `GET /collection?prefix=…` — bare icon names within a set. */
@Serializable
data class CollectionDetail(
    val prefix: String = "",
    val total: Int = 0,
    val title: String = "",
    val uncategorized: List<String> = emptyList(),
    val categories: Map<String, List<String>> = emptyMap(),
) {
    /** All icon names in the set, whether categorized or not. */
    fun allNames(): List<String> = uncategorized + categories.values.flatten()
}
