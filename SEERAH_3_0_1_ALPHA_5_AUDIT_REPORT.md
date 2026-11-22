# التقرير الهندسي لتدقيق الإصدار SEERAH v3.0.1-alpha.5

## 1. الإصلاحات المطبقة فعليًا في الكود (`v3.0.1-alpha.5`)
1. **حماية المراجعات الحديثية البشرية (`MIGRATION_3_4` — قاعدة البيانات الإصدار 4):**
   - أُضيفت أعمدة `stable_id`, `is_human_review`, `review_state`, `reviewed_at_epoch_ms` إلى `hadith_grades`.
   - لا تُحذف أي مراجعة بشرية (`is_human_review = 1`) عند تحديث الحزمة؛ وعند تغيّر نص الحديث أو مصدره تُوسَم بـ `NEEDS_REVALIDATION` عبر `markHumanReviewsNeedsRevalidation`.
2. **إزالة جميع العلاقات المكتوبة يدويًا داخل الكود (`SeerahCatalogSeeder.kt` + `seerah_encyclopedia_v2.json`):**
   - أُزيلت شروط `name.contains(...)` وأُزيل تخمين `placeIdsByMap[mapId]`.
   - أُضيف `place_stable_id` صريح للأحداث، ومصفوفات `person_events` (10 روابط) و`event_verses` (8 روابط) داخل ملف الحزمة مع التحقق المسبق من وجود جميع المعرفات قبل الإدراج.
3. **ترقية `MbTilesReader.kt` و`MapProjectionEngine.kt` و`HistoricalMapCanvas.kt`:**
   - فتح قاعدة البيانات وقراءة `metadata` (`minzoom`, `maxzoom`, `bounds`, `scheme`, `format`) على `Dispatchers.IO` عبر `openAsync()`.
   - إضافة `ReentrantReadWriteLock` لمنع إغلاق قاعدة البيانات أثناء قراءة نشطة، و`ConcurrentHashMap` لمنع تكرار طلب البلاطة نفسها بالتزامن (In-Flight Deduplication).
   - حساب النطاق المرئي الفعلي بعد Pan/Zoom عبر `MapProjectionEngine.visibleGeoBounds()` واحترام `minzoom..maxzoom`.
   - إزالة صف أزرار التكبير المكررة من `AtlasScreens.kt`.
4. **الاختبارات وCI (`Alpha43VerificationTest.kt` + `build.yml`):**
   - إضافة اختبارات `unproject()` مع Zoom/Pan، وتحليل `MbTilesMetadata`، واختبار نقر `MultiPolygon` على المضلعين، واختبار EPUB بالمسارات النسبية و`+` والأرشيف التالف، ورفع `unit-test-reports` كـ Artifact في GitHub Actions.

## 2. ما لم يُنفَّذ أو لم يُشغَّل بعد (بكل شفافية)
- **دمج `MapLibre Native for Android`:** لم يُستبدل محرك الخريطة الحالي بـ MapLibre بعد لأن التحقق من توافقه وبناء مكتباته الأصلية (`.so`) يتطلب بيئة Gradle متصلة بالإنترنت ومحاكي Android؛ المحرك الحالي (`MbTilesReader` + `MapProjectionEngine`) يعمل محليًا بالكامل دون إنترنت.
- **تشغيل `gradle testDebugUnitTest` و`connectedDebugAndroidTest` (Emulator) وبناء APK:** لم تُشغَّل داخل بيئة التوليد الحالية لعدم توفر Android SDK وإنترنت؛ تُشغَّل عبر GitHub Actions.
- **استخراج نص PDF العربي / OCR المحلي وملف `gradle-wrapper.jar`:** لا يزالان مؤجلين للمرحلة التالية.
