package com.seerah.domain.model

enum class SeerahStage(val code: String, val arabicTitle: String, val order: Int) {
    LINEAGE_AND_BIRTH("LINEAGE_AND_BIRTH", "النسب والمولد", 1),
    CHILDHOOD("CHILDHOOD", "الطفولة", 2),
    YOUTH("YOUTH", "الشباب", 3),
    BEGINNING_OF_REVELATION("BEGINNING_OF_REVELATION", "بداية الوحي", 4),
    SECRET_DAWAH("SECRET_DAWAH", "الدعوة السرية", 5),
    PUBLIC_DAWAH("PUBLIC_DAWAH", "الدعوة الجهرية", 6),
    ABYSSINIA_MIGRATION("ABYSSINIA_MIGRATION", "الهجرة إلى الحبشة", 7),
    YEAR_OF_SORROW("YEAR_OF_SORROW", "عام الحزن", 8),
    ISRA_AND_MIRAJ("ISRA_AND_MIRAJ", "الإسراء والمعراج", 9),
    AQABAH_PLEDGES("AQABAH_PLEDGES", "بيعتا العقبة", 10),
    HIJRA("HIJRA", "الهجرة النبوية", 11),
    BUILDING_MASJID_NABAWI("BUILDING_MASJID_NABAWI", "بناء المسجد النبوي", 12),
    BROTHERHOOD("BROTHERHOOD", "المؤاخاة", 13),
    BATTLES_AND_EXPEDITIONS("BATTLES_AND_EXPEDITIONS", "الغزوات والسرايا", 14),
    TREATY_OF_HUDAYBIYYAH("TREATY_OF_HUDAYBIYYAH", "صلح الحديبية", 15),
    CONQUEST_OF_MAKKAH("CONQUEST_OF_MAKKAH", "فتح مكة", 16),
    YEAR_OF_DELEGATIONS("YEAR_OF_DELEGATIONS", "عام الوفود", 17),
    FAREWELL_PILGRIMAGE("FAREWELL_PILGRIMAGE", "حجة الوداع", 18),
    PROPHET_ILLNESS("PROPHET_ILLNESS", "مرض النبي ﷺ", 19),
    PASSING("PASSING", "الوفاة", 20);

    companion object {
        fun fromCode(code: String): SeerahStage =
            entries.firstOrNull { it.code == code } ?: LINEAGE_AND_BIRTH
    }
}

/**
 * حالات التوثيق العشر المنفصلة (القسم الرابع — v3.0).
 * لا يُستنتج أي حكم حديثي تلقائيًا من اسم الكتاب أو المؤلف.
 */
enum class HadithGradeStatus(val code: String, val arabicLabel: String) {
    PRIMARY_DOCUMENTED("PRIMARY_DOCUMENTED", "موثق من مصدر أصلي"),
    SAHIH("SAHIH", "حديث صحيح"),
    HASAN("HASAN", "حديث حسن"),
    DAIF("DAIF", "حديث ضعيف"),
    DAIF_JIDDAN("DAIF_JIDDAN", "حديث شديد الضعف"),
    MAWDU("MAWDU", "رواية موضوعة"),
    HISTORICAL_UNGRADED("HISTORICAL_UNGRADED", "خبر تاريخي بلا حكم حديثي"),
    DISPUTED("DISPUTED", "مختلف فيه"),
    INCOMPLETE_CITATION("INCOMPLETE_CITATION", "غير مكتمل التوثيق"),
    UNDER_REVIEW("UNDER_REVIEW", "قيد المراجعة");

    companion object {
        fun fromCode(code: String): HadithGradeStatus =
            entries.firstOrNull { it.code == code } ?: UNDER_REVIEW

        /**
         * قاعدة التحقق العلمي: لا يُقبل تصنيف SAHIH أو HASAN إلا بوجود
         * اسم العالم الحاكم ومرجع الحكم الصريح.
         */
        fun validateGrade(requestedCode: String, gradingScholar: String, gradingSource: String): HadithGradeStatus {
            val status = fromCode(requestedCode)
            if (status in setOf(SAHIH, HASAN, DAIF, DAIF_JIDDAN, MAWDU)) {
                if (gradingScholar.isBlank() || gradingSource.isBlank()) {
                    return INCOMPLETE_CITATION
                }
            }
            return status
        }
    }
}

/**
 * حالات مراجعة المحتوى السبع (القسم السادس عشر — إدارة الجودة العلمية)
 */
enum class ContentReviewStatus(val code: String, val arabicLabel: String) {
    DRAFT("DRAFT", "مسودة"),
    RESEARCHING("RESEARCHING", "قيد البحث"),
    UNDER_REVIEW("UNDER_REVIEW", "قيد المراجعة"),
    NEEDS_COMPLETION("NEEDS_COMPLETION", "يحتاج إلى استكمال"),
    VERIFIED("VERIFIED", "موثق"),
    DISPUTED("DISPUTED", "مختلف فيه"),
    REJECTED("REJECTED", "مرفوض");

    companion object {
        fun fromCode(code: String): ContentReviewStatus =
            entries.firstOrNull { it.code == code } ?: NEEDS_COMPLETION
    }
}

/**
 * درجات الثقة الجغرافية الخمس (القسم الخامس — GIS)
 */
enum class GeographicCertainty(val code: String, val arabicLabel: String) {
    CONFIRMED("CONFIRMED", "مؤكد جغرافيًا"),
    PROBABLE("PROBABLE", "راجح"),
    APPROXIMATE("APPROXIMATE", "تقريبي"),
    DISPUTED("DISPUTED", "مختلف في تحديده"),
    UNSPECIFIED("UNSPECIFIED", "غير محدد");

    companion object {
        fun fromCode(code: String): GeographicCertainty =
            entries.firstOrNull { it.code == code } ?: UNSPECIFIED
    }
}

enum class PersonCategory(val code: String, val arabicTitle: String) {
    MOTHERS_OF_BELIEVERS("MOTHERS_OF_BELIEVERS", "أمهات المؤمنين"),
    AHL_AL_BAYT("AHL_AL_BAYT", "أهل البيت"),
    RIGHTLY_GUIDED_CALIPHS("RIGHTLY_GUIDED_CALIPHS", "الخلفاء الراشدون"),
    MUHAJIRUN("MUHAJIRUN", "المهاجرون"),
    ANSAR("ANSAR", "الأنصار"),
    FEMALE_COMPANIONS("FEMALE_COMPANIONS", "الصحابيات"),
    COMMANDERS("COMMANDERS", "قادة الغزوات والسرايا"),
    NARRATORS("NARRATORS", "الرواة"),
    OTHER("OTHER", "الشخصيات والملوك والوفود");

    companion object {
        fun fromCode(code: String): PersonCategory =
            entries.firstOrNull { it.code == code } ?: OTHER
    }
}

/**
 * طبقات الأطلس الجغرافي الـ 12 (القسم الخامس — v3.0)
 */
enum class MapLayerType(val key: String, val arabicTitle: String, val zIndex: Int) {
    CITIES_VILLAGES("cities_villages", "المدن والقرى", 1),
    HISTORICAL_SITES("historical_sites", "المواقع التاريخية", 2),
    MOUNTAINS_TERRAIN("mountains", "الجبال والتضاريس", 3),
    VALLEYS("valleys", "الأودية والشعاب", 4),
    WELLS("wells", "الآبار ومصادر المياه", 5),
    CARAVAN_ROUTES("roads", "طرق القوافل", 6),
    HIJRA_ROUTES("hijra_routes", "طرق الهجرة", 7),
    BATTLE_ROUTES("battles", "مسارات الغزوات", 8),
    CAMPS("camps", "مواقع المعسكرات", 9),
    HISTORICAL_REGIONS("terrain", "المناطق التاريخية", 10),
    MILITARY_ROADS("military_roads", "الطرق العسكرية", 11),
    PEOPLE_LOCATIONS("people_locations", "مواقع الأحداث المرتبطة بالشخصيات", 12)
}

enum class AtlasMapDefinition(
    val mapId: String,
    val arabicTitle: String,
    val isBattleMap: Boolean,
    val description: String
) {
    ARABIA("arabia", "الجزيرة العربية", false, "النطاق الجغرافي العام لأحداث السيرة النبوية والوفود والبعوث."),
    HIJAZ("hijaz", "الحجاز", false, "إقليم الحجاز بين مكة والمدينة والطائف والبحر الأحمر."),
    MAKKAH("makkah", "مكة المكرمة", false, "المعالم التاريخية للعهد المكي ومواضع الدعوة وغار حراء وثور."),
    MADINAH("madinah", "المدينة المنورة", false, "معالم المدينة النبوية وقباء والمسجد النبوي والمناطق المحيطة."),
    HIJRA("hijra", "الهجرة النبوية", false, "نقاط طريق الهجرة الثابتة والمختلف فيها مع المراجع العلمية."),
    BADR("badr", "غزوة بدر", true, "خريطة ميدان غزوة بدر الكبرى وفق البيانات الجغرافية الموثقة."),
    UHUD("uhud", "غزوة أحد", true, "خريطة جبل أحد وميدان الغزوة والمواقع الثابتة في المصادر."),
    KHANDAQ("khandaq", "غزوة الخندق", true, "خريطة المدينة ومواضع الخندق وفق المصادر التاريخية والجغرافية."),
    HUDAYBIYYAH("hudaybiyyah", "الحديبية", true, "موضع صلح الحديبية على أطراف الحرم المكي."),
    KHAYBAR("khaybar", "غزوة خيبر", true, "واحة خيبر وحصونها شمال المدينة المنورة."),
    CONQUEST_MAKKAH("conquest_makkah", "فتح مكة", true, "محاور دخول مكة المكرمة وفق المصادر التاريخية."),
    HUNAYN_TAIF("hunayn_taif", "حنين والطائف", true, "وادي حنين والطريق إلى الطائف ومواقع الغزوة."),
    TABUK("tabuk", "غزوة تبوك", true, "المسار الشمالي من المدينة إلى تبوك."),
    MUTAH("mutah", "غزوة/سرية مؤتة", true, "موضع مؤتة بجنوب بلاد الشام.");

    companion object {
        fun fromId(mapId: String): AtlasMapDefinition =
            entries.firstOrNull { it.mapId == mapId } ?: ARABIA
    }
}

enum class SearchContentFilter(val code: String, val arabicLabel: String) {
    ALL("ALL", "الكل"),
    EVENTS("EVENTS", "الأحداث"),
    BATTLES("BATTLES", "الغزوات"),
    EXPEDITIONS("EXPEDITIONS", "السرايا"),
    PEOPLE("PEOPLE", "الشخصيات"),
    PLACES("PLACES", "الأماكن"),
    BOOKS("BOOKS", "الكتب"),
    NARRATIONS("NARRATIONS", "الروايات"),
    VERSES("VERSES", "الآيات"),
    HADITHS("HADITHS", "الأحاديث")
}

enum class ImportFormat(val extension: String, val label: String) {
    PDF("pdf", "PDF"),
    TXT("txt", "TXT"),
    EPUB("epub", "EPUB"),
    XML("xml", "XML"),
    JSON("json", "JSON"),
    CSV("csv", "CSV"),
    SQLITE("sqlite", "SQLite"),
    ZIP("zip", "ZIP");

    companion object {
        fun fromFileName(name: String): ImportFormat? {
            val ext = name.substringAfterLast('.', "").lowercase()
            return when (ext) {
                "pdf" -> PDF
                "txt", "md" -> TXT
                "epub" -> EPUB
                "xml" -> XML
                "json", "geojson" -> JSON
                "csv" -> CSV
                "db", "sqlite", "sqlite3", "mbtiles" -> SQLITE
                "zip" -> ZIP
                else -> null
            }
        }
    }
}
