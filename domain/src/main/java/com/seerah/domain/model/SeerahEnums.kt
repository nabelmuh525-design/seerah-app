package com.seerah.domain.model

/**
 * مراحل الخط الزمني العشرون للسيرة النبوية
 */
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
 * تصنيفات الحكم الحديثي والتوثيقي (لا يُنشأ أي حكم آلياً دون مرجع علمي)
 */
enum class HadithGradeStatus(val code: String, val arabicLabel: String) {
    SAHIH("SAHIH", "صحيح"),
    HASAN("HASAN", "حسن"),
    DAIF("DAIF", "ضعيف"),
    DAIF_JIDDAN("DAIF_JIDDAN", "ضعيف جدًا"),
    MAWDU("MAWDU", "موضوع"),
    DISPUTED("DISPUTED", "مختلف فيه"),
    HISTORICAL_UNGRADED("HISTORICAL_UNGRADED", "خبر تاريخي غير محكوم عليه حديثيًا"),
    UNVERIFIED("UNVERIFIED", "غير متحقق منه");

    companion object {
        fun fromCode(code: String): HadithGradeStatus =
            entries.firstOrNull { it.code == code } ?: UNVERIFIED
    }
}

/**
 * درجة اليقين الجغرافي للمواقع والمسارات
 */
enum class GeographicCertainty(val code: String, val arabicLabel: String) {
    CONFIRMED("CONFIRMED", "موقع مؤكد جغرافيًا وتاريخيًا"),
    PROBABLE("PROBABLE", "موقع راجح وفق المصادر"),
    DISPUTED("DISPUTED", "موقع أو مسار مختلف فيه بين المصادر"),
    UNVERIFIED("UNVERIFIED", "غير متحقق منه — بانتظار بيانات جغرافية موثقة");

    companion object {
        fun fromCode(code: String): GeographicCertainty =
            entries.firstOrNull { it.code == code } ?: UNVERIFIED
    }
}

/**
 * تصنيفات الشخصيات التسعة في موسوعة الشخصيات
 */
enum class PersonCategory(val code: String, val arabicTitle: String) {
    MOTHERS_OF_BELIEVERS("MOTHERS_OF_BELIEVERS", "أمهات المؤمنين"),
    AHL_AL_BAYT("AHL_AL_BAYT", "أهل البيت"),
    RIGHTLY_GUIDED_CALIPHS("RIGHTLY_GUIDED_CALIPHS", "الخلفاء الراشدون"),
    MUHAJIRUN("MUHAJIRUN", "المهاجرون"),
    ANSAR("ANSAR", "الأنصار"),
    FEMALE_COMPANIONS("FEMALE_COMPANIONS", "الصحابيات"),
    COMMANDERS("COMMANDERS", "قادة الغزوات والسرايا"),
    NARRATORS("NARRATORS", "الرواة"),
    OTHER("OTHER", "الشخصيات التاريخية الأخرى");

    companion object {
        fun fromCode(code: String): PersonCategory =
            entries.firstOrNull { it.code == code } ?: OTHER
    }
}

/**
 * طبقات الأطلس العشر القابلة للتشغيل والإيقاف المستقل
 */
enum class MapLayerType(val key: String, val arabicTitle: String, val zIndex: Int) {
    TERRAIN("terrain", "طبقة التضاريس", 1),
    MOUNTAINS("mountains", "طبقة الجبال", 2),
    VALLEYS("valleys", "طبقة الأودية", 3),
    WELLS("wells", "طبقة الآبار", 4),
    ROADS("roads", "طبقة الطرق", 5),
    HISTORICAL_SITES("historical_sites", "طبقة المواقع التاريخية", 6),
    EVENTS("events", "طبقة الأحداث", 7),
    HIJRA_ROUTES("hijra_routes", "طبقة مسارات الهجرة", 8),
    BATTLES("battles", "طبقة الغزوات", 9),
    PEOPLE_LOCATIONS("people_locations", "طبقة المواقع المرتبطة بالشخصيات", 10)
}

/**
 * خرائط الأطلس التاريخي الإحدى عشرة المطلوبة
 */
enum class AtlasMapDefinition(
    val mapId: String,
    val arabicTitle: String,
    val isBattleMap: Boolean,
    val description: String
) {
    ARABIA("arabia", "شبه الجزيرة العربية", false, "النطاق الجغرافي العام لأحداث السيرة النبوية والبعوث والوفود."),
    MAKKAH("makkah", "مكة المكرمة", false, "المعالم التاريخية للعهد المكي ومواضع الدعوة وغار حراء وثور."),
    MADINAH("madinah", "المدينة المنورة", false, "معالم المدينة النبوية وقباء والمسجد النبوي والمناطق المحيطة."),
    BADR("badr", "غزوة بدر", true, "خريطة ميدان غزوة بدر الكبرى: التضاريس والأودية والآبار والمراحل."),
    UHUD("uhud", "غزوة أحد", true, "خريطة جبل أحد وميدان الغزوة والمواقع الثابتة في المصادر."),
    KHANDAQ("khandaq", "غزوة الخندق", true, "خريطة المدينة ومواضع الخندق وفق المصادر التاريخية والجغرافية."),
    HIJRA("hijra", "مسار الهجرة النبوية", false, "مراحل طريق الهجرة من مكة إلى قباء والمدينة مع بيان درجة ثبوت كل نقطة."),
    HUDAYBIYYAH("hudaybiyyah", "الحديبية", true, "موضع صلح الحديبية والطريق المؤدي إليه من المدينة إلى أطراف الحرم."),
    KHAYBAR("khaybar", "غزوة خيبر", true, "واحة خيبر وحصونها وفق البيانات التاريخية والجغرافية المتاحة."),
    HUNAYN_TAIF("hunayn_taif", "حنين والطائف", true, "وادي حنين والطريق إلى الطائف ومواقع الغزوة."),
    TABUK("tabuk", "غزوة تبوك", true, "المسار الشمالي من المدينة إلى تبوك والمواقع المرتبطة به.");

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
