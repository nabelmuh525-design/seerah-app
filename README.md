# سِيرَة | SEERAH
**موسوعة السيرة النبوية التفاعلية الشاملة**
«رحلة موثقة في حياة رسول الله محمد ﷺ»

> نقرأ الأحداث، ونشاهد الأماكن، ونتتبع التاريخ، ونرجع إلى المصادر.

## البنية الهندسية (16 وحدة مستقلة)
- `:app` — التطبيق الرئيسي ونظام التنقل للشاشات الـ 34
- `:core` — الهوية البصرية (Design System)، ودعم RTL والوضع الليلي، ومعالج البحث العربي `ArabicNormalizer`
- `:database` — قاعدة بيانات Room بـ 26 جدولًا علائقيًا + جدولي FTS4 + ترحيل آمن وفحص سلامة SQLite
- `:domain` — النماذج والتصنيفات العلمية الـ 20 مرحلة والـ 11 خريطة والـ 10 طبقات والـ 9 فئات شخصيات
- `:data` — محرك استيراد الكتب (`PDF`, `TXT`, `EPUB`, `XML`, `JSON/GeoJSON`, `CSV`, `SQLite/MBTiles`, `ZIP`) + كشف التكرار SHA-256 + النسخ الاحتياطي + WorkManager + DataStore
- الوحدات الوظيفية الـ 11: `feature-home`, `feature-timeline`, `feature-atlas`, `feature-battles`, `feature-people`, `feature-places`, `feature-library`, `feature-search`, `feature-quran`, `feature-hadith`, `feature-settings`

## بناء ملف APK عبر GitHub Actions
يحتوي المشروع على ملف `.github/workflows/build.yml` جاهز لتشغيل اختبارات الوحدة وبناء ملف `seerah-debug-apk` تلقائيًا دون الحاجة لتثبيت Android Studio محليًا.
