package com.seerah.data

import com.seerah.data.geo.GeoJsonParser
import com.seerah.data.importer.ZipSecurityValidator
import com.seerah.domain.model.GeoPointModel
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class ZipSecurityAndGeoTest {

    @Test
    fun calculatesAccurateHaversineDistanceMakkahToMadinah() {
        val makkah = GeoPointModel(21.4225, 39.8262)
        val madinah = GeoPointModel(24.4672, 39.6111)
        val distKm = GeoJsonParser.haversineKm(makkah, madinah)
        // Straight-line geodesic distance between Makkah and Madinah is ~339 km
        assertTrue("Expected ~339 km, got $distKm", distKm in 330.0..348.0)
    }

    @Test
    fun validatesCoordinateBounds() {
        assertTrue(GeoJsonParser.isValidCoordinate(21.4225, 39.8262))
        assertFalse(GeoJsonParser.isValidCoordinate(95.0, 39.8))
        assertFalse(GeoJsonParser.isValidCoordinate(21.4, 200.0))
    }

    @Test(expected = SecurityException::class)
    fun blocksZipSlipTraversalAttack() {
        val dir = File(System.getProperty("java.io.tmpdir"), "seerah_zip_test")
        ZipSecurityValidator.resolveSafeEntryFile(dir, "../../etc/passwd")
    }

    @Test(expected = SecurityException::class)
    fun blocksZipBombUncompressedQuota() {
        ZipSecurityValidator.validateQuota(10, ZipSecurityValidator.MAX_TOTAL_UNCOMPRESSED_BYTES + 1L)
    }
}
