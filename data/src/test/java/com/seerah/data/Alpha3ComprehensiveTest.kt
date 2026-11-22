package com.seerah.data

import com.seerah.data.geo.GeoJsonParser
import com.seerah.data.geo.MbTilesReader
import com.seerah.data.importer.EpubOpfParser
import com.seerah.domain.model.HadithGradeStatus
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class Alpha3ComprehensiveTest {

    @Test
    fun parsesAllSevenRfc7946GeometryTypesAndPolygonHoles() {
        val geoJson = """
        {
          "type": "FeatureCollection",
          "features": [
            {"id":"p1","type":"Feature","properties":{"name":"مكة"},"geometry":{"type":"Point","coordinates":[39.8262,21.4225]}},
            {"id":"mp1","type":"Feature","properties":{"name":"نقاط"},"geometry":{"type":"MultiPoint","coordinates":[[39.8,21.4],[39.6,24.4]]}},
            {"id":"ls1","type":"Feature","properties":{"name":"مسار"},"geometry":{"type":"LineString","coordinates":[[39.8,21.4],[39.6,24.4]]}},
            {"id":"mls1","type":"Feature","properties":{"name":"مسارات"},"geometry":{"type":"MultiLineString","coordinates":[[[39.8,21.4],[39.7,22.0]],[[39.7,22.0],[39.6,24.4]]]}},
            {"id":"poly1","type":"Feature","properties":{"name":"منطقة بثقب"},"geometry":{"type":"Polygon","coordinates":[
              [[39.0,21.0],[40.0,21.0],[40.0,22.0],[39.0,22.0],[39.0,21.0]],
              [[39.2,21.2],[39.5,21.2],[39.5,21.5],[39.2,21.5],[39.2,21.2]]
            ]}},
            {"id":"mpoly1","type":"Feature","properties":{"name":"مناطق"},"geometry":{"type":"MultiPolygon","coordinates":[
              [[[38.0,23.0],[39.0,23.0],[39.0,24.0],[38.0,24.0],[38.0,23.0]]],
              [[[39.5,24.0],[39.8,24.0],[39.8,24.5],[39.5,24.5],[39.5,24.0]]]
            ]}},
            {"id":"gc1","type":"Feature","properties":{"name":"مجموعة"},"geometry":{"type":"GeometryCollection","geometries":[
              {"type":"Point","coordinates":[39.611,24.467]},
              {"type":"LineString","coordinates":[[39.611,24.467],[39.617,24.439]]}
            ]}}
          ]
        }
        """.trimIndent()

        val report = GeoJsonParser.parseFeatureCollectionWithReport(geoJson)
        assertEquals(0, report.rejected.size)
        assertEquals(8, report.features.size) // 6 + 2 from GeometryCollection
        val poly = report.features.first { it.id == "poly1" }
        assertEquals(1, poly.polygons.size)
        assertEquals(1, poly.polygons.first().holes.size)
        val mpoly = report.features.first { it.id == "mpoly1" }
        assertEquals(2, mpoly.polygons.size)
    }

    @Test
    fun rejectsUnclosedPolygonAndMalformedJsonWithExplicitReport() {
        val unclosed = """
        {"type":"FeatureCollection","features":[
          {"id":"bad_poly","type":"Feature","properties":{"name":"مضلع مفتوح"},"geometry":{"type":"Polygon","coordinates":[[[39.0,21.0],[40.0,21.0],[40.0,22.0],[39.0,22.0]]]}}
        ]}
        """.trimIndent()
        val rep1 = GeoJsonParser.parseFeatureCollectionWithReport(unclosed)
        assertEquals(0, rep1.features.size)
        assertEquals(1, rep1.rejected.size)
        assertEquals("bad_poly", rep1.rejected.first().featureId)

        val rep2 = GeoJsonParser.parseFeatureCollectionWithReport("{broken_json")
        assertEquals(0, rep2.features.size)
        assertEquals(1, rep2.rejected.size)
    }

    @Test
    fun convertsMbTilesTmsAndXyzAndLatLonAccurately() {
        assertEquals(10, MbTilesReader.xyzToTmsY(4, 5))
        assertEquals(5, MbTilesReader.tmsToXyzY(4, 10))
        val (x, y) = MbTilesReader.latLonToTileXY(21.4225, 39.8262, 6)
        assertTrue(x in 0..63 && y in 0..63)
    }

    @Test
    fun parsesEpubOpfInExactSpineOrder() {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            fun add(name: String, content: String) {
                zos.putNextEntry(ZipEntry(name))
                zos.write(content.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }
            add("META-INF/container.xml", """<container><rootfiles><rootfile full-path="OEBPS/content.opf"/></rootfiles></container>""")
            add("OEBPS/content.opf", """
                <package>
                  <manifest>
                    <item id="ch2" href="chapter2.xhtml" media-type="application/xhtml+xml"/>
                    <item id="ch1" href="chapter1.xhtml" media-type="application/xhtml+xml"/>
                  </manifest>
                  <spine>
                    <itemref idref="ch1"/>
                    <itemref idref="ch2"/>
                  </spine>
                </package>
            """.trimIndent())
            add("OEBPS/chapter2.xhtml", "<html><body><h1>الفصل الثاني</h1><p>نص الفصل الثاني</p></body></html>")
            add("OEBPS/chapter1.xhtml", "<html><body><h1>الفصل الأول</h1><p>نص الفصل الأول</p></body></html>")
        }

        val chapters = EpubOpfParser.parseChaptersFromZip(ByteArrayInputStream(baos.toByteArray()))
        assertEquals(2, chapters.size)
        assertEquals("الفصل الأول", chapters[0].title)
        assertEquals("الفصل الثاني", chapters[1].title)
    }

    @Test
    fun strictHadithGradeRejectsMissingReferenceOrOriginalText() {
        assertEquals(
            HadithGradeStatus.INCOMPLETE_CITATION,
            HadithGradeStatus.validateGradeStrict("SAHIH", "الإمام البخاري", "صحيح البخاري", "", "")
        )
        assertEquals(
            HadithGradeStatus.SAHIH,
            HadithGradeStatus.validateGradeStrict("SAHIH", "الإمام البخاري", "صحيح البخاري", "3", "متفق عليه", isCitationVerifiedByReviewer = true)
        )
    }

    @Test
    fun tileToBoundsContainsOriginalLatLon() {
        val lat = 21.4225
        val lon = 39.8262
        val zoom = 10
        val (tx, ty) = MbTilesReader.latLonToTileXY(lat, lon, zoom)
        val b = MbTilesReader.tileToBounds(zoom, tx, ty)
        assertTrue("Lat $lat must be inside ${b.southLat}..${b.northLat}", lat in b.southLat..b.northLat)
        assertTrue("Lon $lon must be inside ${b.westLon}..${b.eastLon}", lon in b.westLon..b.eastLon)
    }

    @Test(expected = SecurityException::class)
    fun epubParserBlocksZipSlipEntry() {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            zos.putNextEntry(ZipEntry("../../evil.xhtml"))
            zos.write("<html/>".toByteArray(Charsets.UTF_8))
            zos.closeEntry()
        }
        EpubOpfParser.parseChaptersFromZip(ByteArrayInputStream(baos.toByteArray()))
    }
}
