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
        val rejected = HadithGradeStatus.validateGradeStrict("SAHIH", "", "", "", "")
        assertEquals(HadithGradeStatus.INCOMPLETE_CITATION, rejected)

        val unverified = HadithGradeStatus.validateGradeStrict(
            requestedCode = "SAHIH",
            gradingScholar = "الإمام البخاري",
            gradingSource = "صحيح البخاري",
            referenceLocator = "حديث 3",
            originalGradeText = "صحيح",
            isCitationVerifiedByReviewer = false
        )
        assertEquals(HadithGradeStatus.UNDER_REVIEW, unverified)

        val accepted = HadithGradeStatus.validateGradeStrict(
            requestedCode = "SAHIH",
            gradingScholar = "الإمام البخاري",
            gradingSource = "صحيح البخاري",
            referenceLocator = "حديث 3",
            originalGradeText = "صحيح",
            isCitationVerifiedByReviewer = true
        )
        assertEquals(HadithGradeStatus.SAHIH, accepted)
    }
}