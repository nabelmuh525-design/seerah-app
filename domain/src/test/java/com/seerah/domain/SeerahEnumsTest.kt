package com.seerah.domain

import com.seerah.domain.model.AtlasMapDefinition
import com.seerah.domain.model.MapLayerType
import com.seerah.domain.model.PersonCategory
import com.seerah.domain.model.SeerahStage
import org.junit.Assert.assertEquals
import org.junit.Test

class SeerahEnumsTest {
    @Test
    fun verifiesCompleteDomainCatalogCounts() {
        assertEquals(20, SeerahStage.entries.size)
        assertEquals(11, AtlasMapDefinition.entries.size)
        assertEquals(10, MapLayerType.entries.size)
        assertEquals(9, PersonCategory.entries.size)
    }
}
