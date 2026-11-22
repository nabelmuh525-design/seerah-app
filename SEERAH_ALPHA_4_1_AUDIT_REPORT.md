# التقرير الهندسي لتدقيق الإصدار SEERAH v3.0.0-alpha.4.1

## 1. الملفات المعدّلة في هذا الإصدار
- `data/src/main/java/com/seerah/data/geo/MbTilesReader.kt` (إضافة `tileToBounds` للتحويل الدقيق من XYZ إلى WGS84 Web Mercator)
- `feature-atlas/src/main/java/com/seerah/feature/atlas/HistoricalMapCanvas.kt` (إسقاط بلاطات MBTiles بنفس تحويل المعالم `projectLatLon` + Viewport Culling + رسم فعلي للأنواع السبعة لـ GeoJSON بما فيها ثقوب `Polygon` عبر `EvenOdd` و`MultiPoint` و`MultiLineString` و`MultiPolygon`)
- `data/src/main/java/com/seerah/data/importer/EpubOpfParser.kt` (استبدال Regex بمحلل XML حقيقي `DocumentBuilderFactory` مع تعطيل XXE + قراءة محدودة الحجم `readBounded` + دعم المسارات النسبية وURL Encoding + منع Zip Slip)
- `data/src/main/java/com/seerah/data/importer/BookImportEngine.kt` (إزالة `readBytes()` و`BT/Tj` و`isOcr=true` لملفات PDF واستخدام `PdfRenderer` بالتدفق مع إشعار صريح بعدم دمج OCR بعد)
- `database/src/main/java/com/seerah/database/dao/SeerahDaos.kt` + `data/src/main/java/com/seerah/data/seed/SeerahCatalogSeeder.kt` (تحويل الاستيراد إلى Upsert حقيقي يحدث السجلات الموجودة دون تكرار الآيات أو الأحاديث ودون مساس بمفضلات وملاحظات المستخدم)
- `data/src/test/java/com/seerah/data/Alpha3ComprehensiveTest.kt` (اختبار تطابق `tileToBounds` مع الإحداثية الجغرافية + اختبار رفض Zip Slip داخل EPUB)
- توحيد رقم الإصدار `3.0.0-alpha.4.1` (`versionCode = 11`) في `app/build.gradle.kts`، و`seerah_encyclopedia_v2.json`، و`.github/workflows/build.yml`، و`README.md`، و`SettingsScreens.kt`، و`SeerahAppUpdater.kt`.

## 2. ما تم اختباره فعليًا مقابل ما لم يُختبر بعد
- **تم التحقق منه داخل بيئة التوليد:** الفحص الساكن لجميع ملفات Kotlin (0 أخطاء أقواس)، صحة بنية `seerah_encyclopedia_v2.json`، تطابق رقم الإصدار `3.0.0-alpha.4.1` في جميع الملفات.
- **لم يُشغَّل بعد (يتطلب GitHub Actions / جهاز Android):** تشغيل `gradle testDebugUnitTest`، وتشغيل `SeerahMigrationTest` على Android Emulator، وتجربة تثبيت `seerah-debug.apk` على جهاز حقيقي.

## 3. القيود المتبقية بشفافية
1. **PDF Text/OCR العربي:** لم تُضف مكتبة خارجية لاستخراج نص PDF أو OCR عربي بعد؛ `PdfRenderer` يتحقق من سلامة الملف وعدد الصفحات دون اختلاق نص.
2. **Gradle Wrapper الثنائي (`gradle-wrapper.jar`):** غير مضمّن لعدم توفر إنترنت في بيئة التوليد؛ يعتمد `build.yml` على `gradle/actions/setup-gradle@v4`.
3. **قراءة بلاطات MBTiles على الخيط الخلفي (Async Tile Loader):** القراءة الحالية عبر `LruCache` سريعة للبلاطات المخزنة مؤقتًا، ويُفضّل نقل قراءة أول بلاطة جديدة إلى `CoroutineDispatcher.IO` في المرحلة التالية للأداء الأمثل على الأجهزة الضعيفة.
4. **العلاقات الموسوعية الكاملة وتوثيق الصفحات المطبوعة:** يحتاج تزويد المشروع بمصادر وكتب محققة لربط جميع الأحداث الـ 46 بأرقام المجلدات والصفحات المطبوعة.
