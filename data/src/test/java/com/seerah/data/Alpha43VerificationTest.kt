package com.seerah.data

import com.seerah.data.geo.GeoJsonParser
import com.seerah.data.geo.MapProjectionEngine
import com.seerah.data.geo.MbTilesReader
import com.seerah.data.importer.EpubOpfParser
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class Alpha43VerificationTest {

    @Test
    fun unprojectRoundTripMatchesProjectUnderZoomAndPan() {
        val engine = MapProjectionEngine(
            minLat = 20.0, maxLat = 26.0, minLon = 37.0, maxLon = 42.0,
            widthPx = 1000f, heightPx = 1200f, zoom = 2.5f, panX = 120f, panY = -85f
        )
        val lat = 21.4225
        val lon = 39.8262
        val pt = engine.project(lat, lon)
        val back = engine.unproject(pt.x, pt.y)
        assertEquals(lat, back.latitude, 0.001)
        assertEquals(lon, back.longitude, 0.001)
    }

    @Test
    fun parsesMbTilesMetadataMinMaxZoomSchemeAndBounds() {
        val meta = MbTilesReader.parseMetadata(
            mapOf(
                "scheme" to "xyz",
                "format" to "webp",
                "minzoom" to "5",
                "maxzoom" to "12",
                "bounds" to "34.5,19.0,42.5,31.5"
            )
        )
        assertEquals("xyz", meta.scheme)
        assertEquals("webp", meta.format)
        assertEquals(5, meta.minZoom)
        assertEquals(12, meta.maxZoom)
        assertNotNull(meta.bounds)
        assertEquals(19.0, meta.bounds!!.southLat, 0.001)
        assertEquals(42.5, meta.bounds!!.eastLon, 0.001)
    }

    @Test
    fun multiPolygonHitTestsBothSeparateSubPolygons() {
        val engine = MapProjectionEngine(20.0, 26.0, 38.0, 42.0, 1000f, 1000f, 1f, 0f, 0f)
        val json = """{"type":"FeatureCollection","features":[
            {"id":"mpoly","type":"Feature","properties":{"name":"منطقتان"},"geometry":{"type":"MultiPolygon","coordinates":[
                [[[38.5,21.0],[39.5,21.0],[39.5,22.0],[38.5,22.0],[38.5,21.0]]],
                [[[40.5,24.0],[41.5,24.0],[41.5,25.0],[40.5,25.0],[40.5,24.0]]]
            ]}}
        ]}"""
        val feat = GeoJsonParser.parseFeatureCollection(json).first()
        val p1 = engine.project(21.5, 39.0)
        val p2 = engine.project(24.5, 41.0)
        val outside = engine.project(23.0, 40.0)
        assertTrue(engine.hitTest(p1.x, p1.y, feat))
        assertTrue(engine.hitTest(p2.x, p2.y, feat))
        assertFalse(engine.hitTest(outside.x, outside.y, feat))
    }

    @Test
    fun epubHandlesComplexRelativePathsAndPlusFilenames() {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            fun add(name: String, content: String) {
                zos.putNextEntry(ZipEntry(name))
                zos.write(content.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }
            add("META-INF/container.xml", """<container><rootfiles><rootfile full-path="OEBPS/meta/book.opf"/></rootfiles></container>""")
            add("OEBPS/meta/book.opf", """
                <package>
                  <manifest>
                    <item id="c1" href="../text/ch+1%20intro.xhtml#top" media-type="application/xhtml+xml"/>
                  </manifest>
                  <spine><itemref idref="c1"/></spine>
                </package>
            """.trimIndent())
            add("OEBPS/text/ch+1 intro.xhtml", "<html><body><h1>مقدمة</h1><p>محتوى الفصل</p></body></html>")
        }
        val chapters = EpubOpfParser.parseChaptersFromZip(ByteArrayInputStream(baos.toByteArray()))
        assertEquals(1, chapters.size)
        assertEquals("مقدمة", chapters[0].title)
    }

    @Test
    fun epubRejectsCorruptTruncatedArchiveGracefully() {
        val corrupt = byteArrayOf(0x50, 0x4B, 0x03, 0x04, 0x00, 0x01)
        val result = runCatching { EpubOpfParser.parseChaptersFromZip(ByteArrayInputStream(corrupt)) }
        // Must either throw a clean exception or return empty without OOM/hang
        assertTrue(result.isFailure || result.getOrNull().orEmpty().isEmpty())
    }
}
