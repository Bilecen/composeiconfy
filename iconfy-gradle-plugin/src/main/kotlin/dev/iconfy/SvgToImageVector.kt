package dev.iconfy

import com.android.ide.common.vectordrawable.Svg2Vector
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.ByteArrayOutputStream
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/** Parsed vector image: viewport + default size + a tree of paths/groups. */
internal data class VectorImage(
    val defaultWidth: Float,
    val defaultHeight: Float,
    val viewportWidth: Float,
    val viewportHeight: Float,
    val nodes: List<VNode>,
)

internal sealed interface VNode

internal data class VPath(
    val pathData: String,
    val fillColor: Long?,
    val fillAlpha: Float,
    val strokeColor: Long?,
    val strokeAlpha: Float,
    val strokeWidth: Float,
    val strokeCap: String,
    val strokeJoin: String,
    val strokeMiter: Float,
    val fillType: String,
) : VNode

internal data class VGroup(
    val rotation: Float,
    val pivotX: Float,
    val pivotY: Float,
    val scaleX: Float,
    val scaleY: Float,
    val translateX: Float,
    val translateY: Float,
    val clipPathData: String?,
    val children: List<VNode>,
) : VNode

/**
 * Converts an Iconify SVG into a [VectorImage] by normalizing through Android's [Svg2Vector]
 * (which flattens shapes/transforms into VectorDrawable `pathData`) and parsing the result.
 * We keep raw `pathData` strings and let Compose's `addPathNodes(...)` parse them at runtime, so
 * no SVG path-command parser lives here.
 */
internal object SvgToImageVector {

    fun convert(svgFile: File): Result<VectorImage> {
        // VectorDrawable has no `currentColor`; Iconify uses it for monochrome icons. Map to opaque
        // black so the vector is valid and later tintable via the Icon composable.
        val prepared = svgFile.readText().replace("currentColor", "#000000")
        val tmp = File.createTempFile("iconfy-", ".svg")
        return try {
            tmp.writeText(prepared)
            val out = ByteArrayOutputStream()
            val error = Svg2Vector.parseSvgToXml(tmp.toPath(), out)
            val xml = out.toString(Charsets.UTF_8.name())
            if (xml.isBlank()) {
                Result.failure(IllegalStateException(error.ifBlank { "Svg2Vector produced no output" }))
            } else {
                Result.success(parseVectorDrawable(xml))
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            tmp.delete()
        }
    }

    private fun parseVectorDrawable(xml: String): VectorImage {
        val doc = DocumentBuilderFactory.newInstance()
            .apply { isNamespaceAware = false }
            .newDocumentBuilder()
            .parse(xml.byteInputStream())
        val root = doc.documentElement // <vector>
        val vpW = root.attrFloat("android:viewportWidth") ?: 24f
        val vpH = root.attrFloat("android:viewportHeight") ?: 24f
        return VectorImage(
            defaultWidth = root.attrFloat("android:width") ?: vpW,
            defaultHeight = root.attrFloat("android:height") ?: vpH,
            viewportWidth = vpW,
            viewportHeight = vpH,
            nodes = parseChildren(root),
        )
    }

    private fun parseChildren(parent: Element): List<VNode> {
        val nodes = mutableListOf<VNode>()
        val children = parent.childNodes
        for (i in 0 until children.length) {
            val n = children.item(i)
            if (n.nodeType != Node.ELEMENT_NODE) continue
            val el = n as Element
            when (el.tagName) {
                "path" -> parsePath(el)?.let { nodes += it }
                "group" -> nodes += parseGroup(el)
            }
        }
        return nodes
    }

    private fun parsePath(el: Element): VPath? {
        val d = el.getAttribute("android:pathData").ifBlank { return null }
        return VPath(
            pathData = d,
            fillColor = el.attrColor("android:fillColor"),
            fillAlpha = el.attrFloat("android:fillAlpha") ?: 1f,
            strokeColor = el.attrColor("android:strokeColor"),
            strokeAlpha = el.attrFloat("android:strokeAlpha") ?: 1f,
            strokeWidth = el.attrFloat("android:strokeWidth") ?: 0f,
            strokeCap = el.getAttribute("android:strokeLineCap").ifBlank { "butt" },
            strokeJoin = el.getAttribute("android:strokeLineJoin").ifBlank { "miter" },
            strokeMiter = el.attrFloat("android:strokeMiterLimit") ?: 4f,
            fillType = el.getAttribute("android:fillType").ifBlank { "nonZero" },
        )
    }

    private fun parseGroup(el: Element): VGroup {
        var clip: String? = null
        val cs = el.childNodes
        for (i in 0 until cs.length) {
            val c = cs.item(i)
            if (c.nodeType == Node.ELEMENT_NODE && (c as Element).tagName == "clip-path") {
                clip = c.getAttribute("android:pathData").ifBlank { null }
            }
        }
        return VGroup(
            rotation = el.attrFloat("android:rotation") ?: 0f,
            pivotX = el.attrFloat("android:pivotX") ?: 0f,
            pivotY = el.attrFloat("android:pivotY") ?: 0f,
            scaleX = el.attrFloat("android:scaleX") ?: 1f,
            scaleY = el.attrFloat("android:scaleY") ?: 1f,
            translateX = el.attrFloat("android:translateX") ?: 0f,
            translateY = el.attrFloat("android:translateY") ?: 0f,
            clipPathData = clip,
            children = parseChildren(el),
        )
    }

    private fun Element.attrFloat(name: String): Float? =
        getAttribute(name).takeIf { it.isNotBlank() }?.removeSuffix("dp")?.toFloatOrNull()

    private fun Element.attrColor(name: String): Long? =
        getAttribute(name).takeIf { it.isNotBlank() }?.let(::parseColor)

    private fun parseColor(raw: String): Long? {
        val s = raw.trim().removePrefix("#")
        return when (s.length) {
            6 -> "FF$s".toLongOrNull(16)
            8 -> s.toLongOrNull(16)
            3 -> "FF${s[0]}${s[0]}${s[1]}${s[1]}${s[2]}${s[2]}".toLongOrNull(16)
            else -> null
        }
    }
}
