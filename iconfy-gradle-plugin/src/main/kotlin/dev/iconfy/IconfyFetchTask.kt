package dev.iconfy

import com.google.gson.JsonParser
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.time.Duration
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/**
 * Downloads declared Iconify icons and normalizes each into a self-contained `.svg` file under the
 * cache directory (`<prefix>/<name>.svg`). Cache-first: an icon already on disk is never re-fetched,
 * so incremental and `--offline` builds do no network I/O.
 */
abstract class IconfyFetchTask : DefaultTask() {

    /** Encoded placements; only the (prefix, name) part matters for fetching. */
    @get:Input
    abstract val placements: SetProperty<String>

    @get:Input
    abstract val apiUrl: Property<String>

    @get:Input
    abstract val failOnMissing: Property<Boolean>

    /** True when Gradle runs with `--offline`; suppresses all network access. */
    @get:Input
    abstract val offline: Property<Boolean>

    @get:OutputDirectory
    abstract val cacheDir: DirectoryProperty

    @TaskAction
    fun run() {
        val placementList = placements.get()
        if (placementList.isEmpty()) {
            logger.lifecycle("iconfy: no icons declared in the iconfy { } block.")
            return
        }

        val base = apiUrl.get().trimEnd('/')
        val outRoot = cacheDir.get().asFile
        val isOffline = offline.get()
        val missing = mutableListOf<String>()

        if (!isOffline && !base.startsWith("https://", ignoreCase = true)) {
            logger.warn("iconfy: apiUrl '$base' is not HTTPS — icon data will be fetched over cleartext.")
        }

        // One client, with timeouts, so a stalled server fails the task instead of hanging the build.
        val client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build()

        // Fetching is category-agnostic: reduce placements to unique (prefix, name), grouped by set.
        val byPrefix = placementList
            .map { IconfyExtension.decode(it) }
            .map { it.prefix to it.name }
            .distinct()
            .sortedWith(compareBy({ it.first }, { it.second }))
            .groupBy(keySelector = { it.first }, valueTransform = { it.second })

        for ((prefix, names) in byPrefix.toSortedMap()) {
            val needed = names.filterNot { cacheFile(outRoot, prefix, it).isFile }.distinct().sorted()
            if (needed.isEmpty()) {
                logger.info("iconfy: '$prefix' fully cached (${names.size} icon(s)).")
                continue
            }
            if (isOffline) {
                missing += needed.map { "$prefix:$it" }
                continue
            }
            fetchPrefix(client, base, prefix, needed, outRoot, missing)
        }

        if (missing.isNotEmpty()) {
            val detail = if (isOffline) " (offline: cache miss)" else ""
            val msg = "iconfy: ${missing.size} icon(s) unavailable$detail: ${missing.sorted().joinToString(", ")}"
            if (failOnMissing.get()) throw GradleException(msg) else logger.warn("WARNING — $msg")
        }
    }

    private fun fetchPrefix(
        client: HttpClient,
        base: String,
        prefix: String,
        names: List<String>,
        outRoot: File,
        missing: MutableList<String>,
    ) {
        val url = "$base/$prefix.json?icons=${names.joinToString(",")}"
        logger.lifecycle("iconfy: fetching ${names.size} icon(s) from '$prefix'…")

        val request = HttpRequest.newBuilder(URI.create(url))
            .header("User-Agent", "iconfy-gradle-plugin")
            .timeout(Duration.ofSeconds(30))
            .GET()
            .build()

        val response: HttpResponse<String> = try {
            client.send(request, HttpResponse.BodyHandlers.ofString())
        } catch (e: Exception) {
            throw GradleException("iconfy: network error fetching '$prefix' from $base — ${e.message}", e)
        }
        if (response.statusCode() != 200) {
            throw GradleException("iconfy: API returned HTTP ${response.statusCode()} for $url")
        }

        val root = JsonParser.parseString(response.body()).asJsonObject
        val setWidth = root.get("width")?.takeUnless { it.isJsonNull }?.asInt ?: DEFAULT_SIZE
        val setHeight = root.get("height")?.takeUnless { it.isJsonNull }?.asInt ?: DEFAULT_SIZE
        val icons = root.getAsJsonObject("icons")

        for (name in names) {
            val icon = icons?.getAsJsonObject(name)
            val body = icon?.get("body")?.takeUnless { it.isJsonNull }?.asString
            if (body == null) {
                missing += "$prefix:$name"
                continue
            }
            val width = icon.get("width")?.takeUnless { it.isJsonNull }?.asInt ?: setWidth
            val height = icon.get("height")?.takeUnless { it.isJsonNull }?.asInt ?: setHeight
            val svg = buildString {
                append("<svg xmlns=\"http://www.w3.org/2000/svg\" ")
                append("width=\"$width\" height=\"$height\" viewBox=\"0 0 $width $height\">")
                append(body)
                append("</svg>")
            }
            val file = cacheFile(outRoot, prefix, name)
            file.parentFile.mkdirs()
            file.writeText(svg)
        }

        // Iconify reports unknown names in a `not_found` array.
        root.getAsJsonArray("not_found")?.forEach { missing += "$prefix:${it.asString}" }
    }

    private fun cacheFile(root: File, prefix: String, name: String) = File(root, "$prefix/$name.svg")

    private companion object {
        const val DEFAULT_SIZE = 16
    }
}
