package com.seerah.domain

import com.seerah.domain.model.*
import org.junit.Assert.assertEquals
import org.junit.Test

class SeerahEnumsTest {
    @Test
    fun verifiesCompleteV3CatalogCounts() {
        assertEquals(20, SeerahStage.entries.size)
        assertEquals(14, AtlasMapDefinition.entries.size)
        assertEquals(12, MapLayerType.entries.size)
        assertEquals(9, PersonCategory.entries.size)
        assertEquals(10, HadithGradeStatus.entries.size)
        assertEquals(7, ContentReviewStatus.entries.size)
        assertEquals(5, GeographicCertainty.entries.size)
    }

    @Test
    fun rejectsSahihGradeWithoutScholarAndReference() {
        val rejected = HadithGradeStatus.validateGrade("SAHIH", "", "")
        assertEquals(HadithGradeStatus.INCOMPLETE_CITATION, rejected)

        val accepted = HadithGradeStatus.validateGrade("SAHIH", "الإمام البخاري", "صحيح البخاري (حديث 3)")
        assertEquals(HadithGradeStatus.SAHIH, accepted)
    }
}
