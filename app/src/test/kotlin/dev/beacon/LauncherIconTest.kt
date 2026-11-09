package dev.beacon

import android.content.Context
import android.graphics.drawable.AdaptiveIconDrawable
import androidx.compose.ui.graphics.Color
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.beacon.ui.theme.BeaconColors
import dev.beacon.ui.theme.contrastRatio
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.math.hypot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.w3c.dom.Element

/*
 * Why this test exists: the launcher icon is drawn in XML, outside Compose,
 * so nothing else ties its colours to the design's tokens, keeps its bolt the
 * design's bolt, or keeps that bolt inside the part of the icon every
 * launcher mask shows. Robolectric inflates the icons to prove they are
 * adaptive icons with a monochrome layer; the layer files are read as plain
 * XML for the colours, the path and the geometry.
 */

/** Checks Beacon's adaptive launcher icon: wiring, layers, colours and the safe zone. */
@RunWith(AndroidJUnit4::class)
class LauncherIconTest {

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    /** Both launcher icons inflate as adaptive icons with all three layers, monochrome included. */
    @Test
    fun bothIconsAreAdaptiveWithAMonochromeLayer() {
        listOf(R.mipmap.ic_launcher, R.mipmap.ic_launcher_round).forEach { id ->
            val icon = context.getDrawable(id)
            assertTrue("${context.resources.getResourceEntryName(id)} is not adaptive", icon is AdaptiveIconDrawable)
            icon as AdaptiveIconDrawable
            assertNotNull("background", icon.background)
            assertNotNull("foreground", icon.foreground)
            // The monochrome layer is what Android 13 and later tint for themed icons.
            assertNotNull("monochrome", icon.monochrome)
        }
    }

    /** The installed app reports ic_launcher as its icon. */
    @Test
    fun applicationUsesTheLauncherIcon() {
        val info = context.packageManager.getApplicationInfo(context.packageName, 0)
        assertEquals(R.mipmap.ic_launcher, info.icon)
    }

    /**
     * The manifest names both icons. ApplicationInfo hides the round icon, so
     * the manifest file itself is read for it.
     */
    @Test
    fun manifestSetsIconAndRoundIcon() {
        val application = parse("src/main/AndroidManifest.xml").getElementsByTagName("application").item(0) as Element
        assertEquals("@mipmap/ic_launcher", application.android("icon"))
        assertEquals("@mipmap/ic_launcher_round", application.android("roundIcon"))
    }

    /** The square and round icons use the same three layer drawables. */
    @Test
    fun bothMipmapsUseTheSameThreeLayers() {
        listOf("ic_launcher", "ic_launcher_round").forEach { name ->
            val icon = parse("src/main/res/mipmap-anydpi/$name.xml")
            assertEquals("adaptive-icon", icon.tagName)
            mapOf(
                "background" to "@drawable/ic_launcher_background",
                "foreground" to "@drawable/ic_launcher_foreground",
                "monochrome" to "@drawable/ic_launcher_monochrome",
            ).forEach { (layer, drawable) ->
                val element = icon.getElementsByTagName(layer).item(0) as Element?
                assertNotNull("$name has no $layer layer", element)
                assertEquals("$name $layer", drawable, element!!.android("drawable"))
            }
        }
    }

    /** The background is one full-size square in the design's yellow. */
    @Test
    fun backgroundIsTheDesignYellow() {
        val vector = layer("ic_launcher_background")
        val paths = vector.getElementsByTagName("path")
        assertEquals(1, paths.length)
        val path = paths.item(0) as Element
        assertEquals("M0,0h108v108h-108z", path.android("pathData"))
        assertEquals(BeaconColors.Accent, colour(path.android("fillColor")))
    }

    /**
     * The foreground is the design's bolt, filled and stroked in the design's
     * near-black, and it reaches 7:1 on the yellow. WCAG asks only 3:1 of a
     * graphic; Beacon holds its icon to the 7:1 of its text.
     */
    @Test
    fun foregroundIsTheDesignBoltInNearBlack() {
        val path = boltPath(layer("ic_launcher_foreground"))
        assertEquals(BeaconColors.OnAccent, colour(path.android("fillColor")))
        assertEquals(BeaconColors.OnAccent, colour(path.android("strokeColor")))
        val ratio = contrastRatio(BeaconColors.OnAccent, BeaconColors.Accent)
        assertTrue("bolt on yellow: $ratio", ratio >= 7.0)
    }

    /** The monochrome layer is the same bolt at the same place and size, so a themed icon looks the same. */
    @Test
    fun monochromeIsTheSameBolt() {
        val foreground = layer("ic_launcher_foreground")
        val monochrome = layer("ic_launcher_monochrome")
        val bolt = boltPath(monochrome)
        assertEquals(placement(foreground), placement(monochrome))
        // Android uses only the monochrome layer's alpha, so the bolt must be fully opaque.
        assertEquals(1f, colour(bolt.android("fillColor")).alpha)
    }

    /**
     * Every corner of the stroked bolt lies inside the 66 dp circle at the
     * centre of the 108 dp layer, the only part no launcher mask ever clips.
     */
    @Test
    fun boltStaysInsideTheSafeZone() {
        listOf("ic_launcher_foreground", "ic_launcher_monochrome").forEach { name ->
            val vector = layer(name)
            val place = placement(vector)
            val path = boltPath(vector)
            // A round join reaches at most half the stroke width past a corner.
            val halfStroke = path.android("strokeWidth").toFloat() / 2f * place.scale
            BOLT_CORNERS.forEach { (x, y) ->
                val reach = hypot(place.scale * x + place.translateX - CENTRE, place.scale * y + place.translateY - CENTRE) + halfStroke
                assertTrue("$name corner ($x, $y) reaches $reach dp from the centre", reach <= SAFE_ZONE_RADIUS)
            }
        }
    }

    /** Where a layer's bolt group puts the 24-unit bolt: one uniform scale, then a move. */
    private data class Placement(val scale: Float, val translateX: Float, val translateY: Float)

    /**
     * Reads the bolt group's transform from a layer.
     *
     * @param vector the layer's root vector element.
     * @return the group's scale and move.
     * @throws AssertionError if the scale isn't uniform or the group rotates or pivots.
     */
    private fun placement(vector: Element): Placement {
        val group = vector.getElementsByTagName("group").item(0) as Element
        // The safe-zone check assumes no pivot and no rotation; fail loudly if someone adds one.
        listOf("pivotX", "pivotY", "rotation").forEach { assertNull(it, group.androidOrNull(it)) }
        val scaleX = group.android("scaleX").toFloat()
        assertEquals("scale must be uniform", scaleX, group.android("scaleY").toFloat())
        return Placement(scaleX, group.android("translateX").toFloat(), group.android("translateY").toFloat())
    }

    /**
     * Finds the layer's only path and checks that it is the design's bolt.
     *
     * @param vector the layer's root vector element.
     * @return the bolt's path element.
     * @throws AssertionError if the layer has another path or a different shape.
     */
    private fun boltPath(vector: Element): Element {
        // The 108 dp layer is drawn in dp units, so the safe-zone numbers below are dp.
        listOf("width" to "108dp", "height" to "108dp", "viewportWidth" to "108", "viewportHeight" to "108")
            .forEach { (attr, value) -> assertEquals(attr, value, vector.android(attr)) }
        val paths = vector.getElementsByTagName("path")
        assertEquals(1, paths.length)
        val path = paths.item(0) as Element
        assertEquals(BOLT, path.android("pathData"))
        return path
    }

    /**
     * Parses one of the layer drawables.
     *
     * @param name the drawable's file name without `.xml`.
     * @return its root `<vector>` element.
     */
    private fun layer(name: String): Element = parse("src/main/res/drawable/$name.xml")

    /**
     * Parses an XML file of the app module.
     *
     * @param path the file's path from the module folder, which is where Gradle runs unit tests.
     * @return the document's root element.
     * @throws AssertionError if the file is missing.
     */
    private fun parse(path: String): Element {
        val file = File(path)
        assertTrue("missing ${file.absolutePath}", file.isFile)
        val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        return factory.newDocumentBuilder().parse(file).documentElement
    }

    /**
     * Reads an `android:` attribute.
     *
     * @param name the attribute's name without the prefix.
     * @return its value.
     * @throws AssertionError if it is absent.
     */
    private fun Element.android(name: String): String {
        val value = androidOrNull(name)
        assertNotNull("<$tagName> has no android:$name", value)
        return value!!
    }

    /**
     * Reads an `android:` attribute that may be absent.
     *
     * @param name the attribute's name without the prefix.
     * @return its value, or null when it isn't set.
     */
    private fun Element.androidOrNull(name: String): String? =
        if (hasAttributeNS(ANDROID_NS, name)) getAttributeNS(ANDROID_NS, name) else null

    /**
     * Turns an `#AARRGGBB` colour into a Compose colour.
     *
     * @param hex the colour as written in the XML.
     * @return the same colour.
     * @throws AssertionError if it isn't written with all eight digits.
     */
    private fun colour(hex: String): Color {
        // Eight digits only, like the widget's drawables, so the alpha is never implied.
        assertTrue("write $hex as #AARRGGBB", Regex("#[0-9A-Fa-f]{8}").matches(hex))
        return Color(hex.substring(1).toLong(16))
    }

    private companion object {
        const val ANDROID_NS = "http://schemas.android.com/apk/res/android"

        /** The design's bolt, as in Widget.dc.html, BeaconIcons.Bolt and widget_bolt.xml. */
        const val BOLT = "M13 2L3 14h9l-1 8 10-12h-9l1-8z"

        /** The corners of [BOLT] on its 24-unit view box, worked out by hand from the path. */
        val BOLT_CORNERS = listOf(13f to 2f, 3f to 14f, 12f to 14f, 11f to 22f, 21f to 10f, 12f to 10f)

        /** The centre of the 108 dp layer. */
        const val CENTRE = 54f

        /** Half of the 66 dp safe zone that every launcher mask leaves visible. */
        const val SAFE_ZONE_RADIUS = 33f
    }
}
