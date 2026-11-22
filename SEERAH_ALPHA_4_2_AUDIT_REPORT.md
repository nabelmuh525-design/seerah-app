# التقرير الهندسي لتدقيق الإصدار SEERAH v3.0.0-alpha.4.2

## 1. التغييرات المنفذة فعليًا في alpha.4.2
1. **إكمال Upsert الحقيقي و`stable_id` (`MIGRATION_2_3` إلى الإصدار 3):**
   - إضافة عمود `stable_id` لجداول `events`, `people`, `places`, `quran_verses`, `hadiths` مع ترحيل غير هدّام `MIGRATION_2_3`.
   - تحديث `SeerahCatalogSeeder.kt` لتحديث جميع حقول الأحداث وتواريخها والأماكن وإحداثياتها والشخصيات والآيات والأحاديث دون تكرار، مع منع تكرار `event_verses` و`person_events` و`hadith_grades`، والتحقق المسبق من عدم تكرار `stable_id` داخل كل قسم، وتصحيح القيمة الافتراضية للإصدار إلى `3.0.0-alpha.4.2`.
2. **تحصين EPUB الشامل (`EpubOpfParser.kt`):**
   - احتساب حجم **جميع** مدخلات ZIP (بما فيها الصور والملفات الثنائية عبر `drainAndCountBounded`) ضمن حد `MAX_TOTAL_UNCOMPRESSED_BYTES`.
   - تعطيل DTD وXXE وXInclude و`ACCESS_EXTERNAL_DTD`/`ACCESS_EXTERNAL_SCHEMA` إلزاميًا مع رمي `SecurityException` عند فشل التفعيل.
   - فك ترميز `%XX` دون تحويل `+` إلى مسافة، وحذف `#fragment` و`?query` ومنع المسارات التي تتجاوز جذر الأرشيف.
3. **أداء MBTiles غير المتزامن (`MbTilesReader.kt` و`HistoricalMapCanvas.kt`):**
   - استبدال سعة `LruCache` العددية بحساب حجم Bitmap الفعلي بالكيلوبايت (`sizeOf` بحد 16 MB).
   - إزالة أي استعلام SQLite أو فك `BitmapFactory` من داخل كتلة الرسم `Canvas`: القراءة الآن غير متزامنة بالكامل على `Dispatchers.IO` داخل `LaunchedEffect` (مع إلغاء تلقائي عند تغيير Zoom/Pan).
4. **محرك الإسقاط واختبار النقر (`MapProjectionEngine.kt` و`Alpha42VerificationTest.kt`):**
   - دعم اختبار النقر على جميع نقاط `MultiPoint`، وجميع مقاطع `LineString`/`MultiLineString`، ومساحات `Polygon`/`MultiPolygon` مع استبعاد الثقوب الداخلية (`Ray-Casting`).
5. **التوثيق الحديثي الصارم (`SeerahEnums.kt`):**
   - اشتراط `isCitationVerifiedByReviewer = true` قبل اعتماد `SAHIH`/`HASAN`/`DAIF`؛ وتبقى السجلات غير المراجعة يدويًا في حالة `UNDER_REVIEW`.
6. **توحيد الإصدار `3.0.0-alpha.4.2` (`versionCode = 12`):** في جميع ملفات المشروع مع الإبقاء على التقارير السابقة كسجل تاريخي.

## 2. ما لم يُختبر أو لم يُنفَّذ بعد (بكل شفافية)
- **تشغيل Gradle / Instrumented Tests / بناء Debug & Release APK:** لم يُشغَّل محليًا لعدم توفر Android SDK وإنترنت في بيئة التوليد؛ يتم التشغيل عبر GitHub Actions (Gradle 8.9 عبر `gradle/actions/setup-gradle@v4`).
- **Gradle Wrapper الثنائي (`gradle-wrapper.jar`):** غير مضمّن لعدم توفر إنترنت لتحميله.
- **مكتبة استخراج نص PDF العربي / OCR المحلي:** لم تُضف بعد.
- **اختبار الأداء الفعلي بملف `.mbtiles` حقيقي على جهاز متوسط:** يتطلب تشغيل النسخة المبنية من GitHub Actions مع ملف `.mbtiles` محلي.
