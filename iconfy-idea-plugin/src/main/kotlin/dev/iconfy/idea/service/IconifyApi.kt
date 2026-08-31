package dev.iconfy.idea.service

import dev.iconfy.idea.model.CollectionDetail
import dev.iconfy.idea.model.CollectionInfo
import dev.iconfy.idea.model.SearchResult
import kotlinx.serialization.json.Json
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * Thin client for the public Iconify API (https://iconify.design/docs/api/). Pure JVM (no IntelliJ
 * dependencies) so it is unit-testable against the live API. Callers must add debouncing/caching on
 * top and never call these on the EDT/read thread.
 */
class IconifyApi(private val baseUrl: String = "https://api.iconify.design") {

    private val client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(15))
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    /** Search icons; results come back as `"prefix:name"` strings. */
    fun search(query: String, limit: Int = 64, prefixes: List<String> = emptyList()): SearchResult {
        val q = enc(query)
        val pfx = if (prefixes.isEmpty()) "" else "&prefixes=${prefixes.joinToString(",")}"
        return json.decodeFromString(SearchResult.serializer(), get("/search?query=$q&limit=$limit$pfx"))
    }

    /** All icon sets, keyed by prefix. */
    fun collections(): Map<String, CollectionInfo> =
        json.decodeFromString(get("/collections"))

    /** Bare icon names within a single set. */
    fun collection(prefix: String): CollectionDetail =
        json.decodeFromString(CollectionDetail.serializer(), get("/collection?prefix=${enc(prefix)}"))

    /** Raw SVG for preview; [colorHex] (e.g. "#40C4FF") tints monochrome icons. */
    fun svg(prefix: String, name: String, colorHex: String? = null, height: Int = 24): String {
        val color = if (colorHex != null) "&color=${enc(colorHex)}" else ""
        return get("/$prefix/$name.svg?height=$height$color")
    }

    private fun get(path: String): String {
        val request = HttpRequest.newBuilder(URI.create("$baseUrl$path"))
            .header("User-Agent", "iconfy-idea-plugin")
            .timeout(Duration.ofSeconds(30))
            .GET()
            .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        require(response.statusCode() == 200) { "Iconify API returned HTTP ${response.statusCode()} for $path" }
        return response.body()
    }

    private fun enc(s: String) = URLEncoder.encode(s, Charsets.UTF_8)
}
