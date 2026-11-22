# تقرير التدقيق الهندسي والعلمي — سِيرَة | SEERAH v3.0.0-alpha.3

> مبدأ التقرير: الفصل الصريح بين ما نُفّذ برمجيًا في الكود وما لم يُكتمل أو لم يُختبر بعد على بيئة Android/GitHub Actions.

## 1. الملفات المعدّلة والمضافة في alpha.3
- `app/src/main/assets/seerah_encyclopedia_v2.json` (تصحيح التسلسل التاريخي + تدقيق جغرافي فردي لـ 17 موقعًا)
- `GEOGRAPHIC_COORDINATES_REVIEW_REPORT.md` (تقرير المواقع التي لا يمكن اعتماد إحداثياتها كـ CONFIRMED حاليًا)
- `domain/src/main/java/com/seerah/domain/model/SeerahEnums.kt` (`validateGradeStrict`)
- `domain/src/main/java/com/seerah/domain/model/DomainModels.kt` (`GeoPolygonRings`, `GeoJsonRejection`, `GeoJsonParseReport`)
- `data/src/main/java/com/seerah/data/geo/GeoJsonParser.kt` (دعم الأنواع السبعة الكاملة RFC 7946 + الثقوب + تقرير الرفض)
- `data/src/main/java/com/seerah/data/geo/MbTilesReader.kt` (قارئ MBTiles + تحويل TMS/XYZ + `LruCache`)
- `data/src/main/java/com/seerah/data/importer/EpubOpfParser.kt` + `BookImportEngine.kt` (محلل OPF كامل لـ EPUB)
- `data/src/main/java/com/seerah/data/seed/SeerahCatalogSeeder.kt` (استيراد ذري عبر `database.withTransaction` + تتبع الأقسام في `content_versions` + ربط علائقي فعلي)
- `feature-atlas/src/main/java/com/seerah/feature/atlas/HistoricalMapCanvas.kt` + `AtlasScreens.kt` (ربط عرض بلاطات `MbTilesReader` فعليًا تحت شبكة WGS84 وGeoJSON)
- `data/src/test/java/com/seerah/data/Alpha3ComprehensiveTest.kt` (اختبارات GeoJSON 7 أنواع، الثقوب، الملفات التالفة، MBTiles TMS/XYZ، EPUB OPF، والحكم الحديثي الصارم)
- `.github/workflows/build.yml` (التحقق من الـ 4 Secrets + فحص التوقيع عبر `apksigner verify`)

## 2. الأخطاء التي تم إصلاحها
1. تصحيح التسلسل التاريخي: الهجرة الأولى إلى الحبشة (17) والثانية (18) تسبقان الآن حصار شعب أبي طالب (20).
2. إزالة الوسم الجماعي `CONFIRMED` من الإحداثيات في `seerah_encyclopedia_v2.json` واستبداله بـ `APPROXIMATE`/`DISPUTED` مع فصل ثبوت الحدث عن دقة الإحداثية.
3. تحويل `SeerahCatalogSeeder` إلى معاملة ذرية (`withTransaction`) مع إلغاء شرط `count() == 0` وتسجيل حالة كل قسم في `content_versions` فقط عند تطابق العدد.
4. استكمال أنواع GeoJSON السبعة (بما في ذلك `MultiPolygon` و`GeometryCollection` وثقوب `Polygon`) وتقرير العناصر المرفوضة.
5. ربط `MbTilesReader` و`LruCache` مباشرة بـ `HistoricalMapCanvas` لعرض البلاطات المحلية دون إنترنت.
6. تنفيذ محلل `EPUB OPF` يحترم ترتيب `spine` ويستخرج عناوين الفصول.
7. تشديد `validateGradeStrict` وإضافة فحص `apksigner verify` في مسار النشر.

## 3. الوظائف المكتملة برمجيًا (بانتظار تشغيل Gradle على GitHub Actions)
- محرك GeoJSON RFC 7946 بجميع أنواعه السبعة وتقرير الرفض.
- محرك عرض بلاطات MBTiles المحلي بـ `LruCache` وتحويل TMS/XYZ.
- الاستيراد الذري للمحتوى (`withTransaction`) وتسجيل البصمات لكل قسم.
- محلل EPUB OPF (manifest + spine).
- التحقق الأمني بـ SHA-256 و`apksigner` في خط النشر.

## 4. الوظائف التي لم تكتمل بعد (بكل شفافية)
- **استخراج نص PDF العربي وOCR المحلي:** لم تُدمج مكتبة OCR محلية (مثل Tesseract Arabic) بعد؛ الملفات المصوّرة تُوسَم فقط بـ `needsReview = true`.
- **Gradle Wrapper الثنائي (`gradle-wrapper.jar`):** لم يُضمّن ملف jar الثنائي لعدم توفر إنترنت في بيئة التوليد؛ يستخدم `build.yml` إجراء `gradle/actions/setup-gradle@v4` لتشغيل Gradle 8.9.
- **استكمال جميع السرايا والبعوث وشجرة الأنساب الكاملة وربط جميع الـ 46 حدثًا بصفحات ومجلدات طبعات محققة:** لا يزال قيد الاستكمال في المرحلة القادمة.
- **حزمة بلاطات `.mbtiles` مدمجة للحجاز:** المحرك جاهز للعرض، ويتطلب استيراد ملف `.mbtiles` محلي من المستخدم.

## 5–6. نتائج الاختبارات وبناء APK
- **الفحص الساكن (Static Audit) داخل بيئة التوليد:** نجح فحص جميع ملفات Kotlin وملف JSON دون أخطاء أقواس أو صياغة.
- **`gradle testDebugUnitTest` و`assembleDebug` و`assembleRelease`:** لم تُشغَّل محليًا لعدم توفر Android SDK في بيئة التوليد؛ يجب تشغيلها ومراجعة نتائجها عبر GitHub Actions.

## 7–12. الإحصاءات الفعلية لقاعدة البيانات الحالية
- الأحداث: **46 حدثًا** عبر 20 مرحلة (حالتها الحالية `NEEDS_COMPLETION` لحين استكمال أرقام المجلدات والصفحات المطبوعة).
- الشخصيات: **22 شخصية** عبر 9 تصنيفات.
- الأماكن: **17 موقعًا** — عدد المواقع ذات الإحداثيات المؤكدة مساحيًا (`CONFIRMED`): **0** (جميعها مصنّفة بصدق كـ `APPROXIMATE` أو `DISPUTED` لحين مراجعة مساحية متخصصة).
- الآيات: **9 آيات** | الأحاديث: **8 أحاديث**.
- الروايات ذات التوثيق الكامل (مجلد + صفحة مطبوعة + طبعة): **0** (تم منع توليد روايات منسوخة من الملخصات).
- الكتب المستوردة افتراضيًا: **0** (22 مرجعًا مسجلًا في الفهرس بانتظار استيراد ملفات المستخدم).
- الخرائط: **14 نطاقًا جغرافيًا (`bbox`)** + محرك `MBTiles` + `GeoJSON RFC 7946`، بانتظار ملفات `.mbtiles` و`.geojson` الموثقة.

## 13. الأخطاء والقيود المعروفة
1. يتطلب نشر أول Release موقّع إنشاء مفتاح توقيع جديد بالكامل وإضافته إلى GitHub Secrets الأربعة.
2. اختبارات `SeerahMigrationTest` (Instrumented) تتطلب محاكي Android أو جهازًا حقيقيًا.
