package com.seerah.data

import com.seerah.data.geo.GeoJsonParser
import com.seerah.data.geo.MapProjectionEngine
import com.seerah.data.importer.EpubOpfParser
import com.seerah.domain.model.HadithGradeStatus
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class Alpha42VerificationTest {

    @Test
    fun mapProjectionEngineHitTestsAllPointsInMultiPointAndPolygonHoles() {
        val engine = MapProjectionEngine(
            minLat = 20.0, maxLat = 25.0, minLon = 38.0, maxLon = 42.0,
            widthPx = 1000f, heightPx = 1000f, zoom = 1f, panX = 0f, panY = 0f
        )

        // 1. MultiPoint: clicking SECOND point must hit!
        val mpJson = """{"type":"FeatureCollection","features":[
            {"id":"mp","type":"Feature","properties":{"name":"نقاط"},"geometry":{"type":"MultiPoint","coordinates":[[39.0,21.0],[41.0,24.0]]}}
        ]}"""
        val mp = GeoJsonParser.parseFeatureCollection(mpJson).first()
        val p2 = engine.project(24.0, 41.0)
        assertTrue("Must hit second point of MultiPoint", engine.hitTest(p2.x, p2.y, mp))

        // 2. Polygon with hole: point inside hole must NOT hit, point in ring MUST hit!
        val polyJson = """{"type":"FeatureCollection","features":[
            {"id":"poly","type":"Feature","properties":{"name":"منطقة"},"geometry":{"type":"Polygon","coordinates":[
                [[39.0,21.0],[41.0,21.0],[41.0,23.0],[39.0,23.0],[39.0,21.0]],
                [[39.5,21.5],[40.5,21.5],[40.5,22.5],[39.5,22.5],[39.5,21.5]]
            ]}}
        ]}"""
        val poly = GeoJsonParser.parseFeatureCollection(polyJson).first()
        val holeCenter = engine.project(22.0, 40.0)
        val ringSpot = engine.project(21.2, 39.2)
        assertFalse("Point inside Polygon hole must NOT hit", engine.hitTest(holeCenter.x, holeCenter.y, poly))
        assertTrue("Point inside Polygon outer ring MUST hit", engine.hitTest(ringSpot.x, ringSpot.y, poly))

        // 3. MultiLineString: clicking midpoint of second line MUST hit!
        val mlsJson = """{"type":"FeatureCollection","features":[
            {"id":"mls","type":"Feature","properties":{"name":"مسارات"},"geometry":{"type":"MultiLineString","coordinates":[
                [[38.5,20.5],[39.0,21.0]],
                [[40.0,23.0],[41.0,24.0]]
            ]}}
        ]}"""
        val mls = GeoJsonParser.parseFeatureCollection(mlsJson).first()
        val midSecond = engine.project(23.5, 40.5)
        assertTrue("Must hit midpoint of second line in MultiLineString", engine.hitTest(midSecond.x, midSecond.y, mls))
    }

    @Test(expected = Exception::class)
    fun epubBlocksXxeDoctypePayload() {
        val xxeXml = """<?xml version="1.0"?>
            <!DOCTYPE foo [<!ENTITY xxe SYSTEM "file:///etc/passwd">]>
            <container><rootfiles><rootfile full-path="&xxe;"/></rootfiles></container>"""
        EpubOpfParser.parseContainerOpfPath(xxeXml)
    }

    @Test
    fun epubDecodesPercentAndPreservesPlusAndStripsFragments() {
        val cleaned = EpubOpfParser.stripQueryAndFragment("OEBPS/ch+1%20intro.xhtml?v=2#sec1")
        val decoded = EpubOpfParser.decodePercentPath(cleaned)
        assertEquals("OEBPS/ch+1 intro.xhtml", decoded)
    }

    @Test
    fun unreviewedHadithGradeStaysUnderReviewEvenWhenFieldsFilled() {
        val status = HadithGradeStatus.validateGradeStrict(
            requestedCode = "SAHIH",
            gradingScholar = "الإمام البخاري",
            gradingSource = "صحيح البخاري",
            referenceLocator = "3",
            originalGradeText = "متفق عليه",
            isCitationVerifiedByReviewer = false
        )
        assertEquals(HadithGradeStatus.UNDER_REVIEW, status)
    }
}
