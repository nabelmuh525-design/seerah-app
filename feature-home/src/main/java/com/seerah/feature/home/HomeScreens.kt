package com.seerah.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.seerah.core.components.*
import com.seerah.core.theme.*
import com.seerah.data.repository.SeerahRepository
import com.seerah.domain.model.AtlasMapDefinition
import com.seerah.domain.model.SeerahStage
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onboardingCompleted: Boolean,
    onNavigateNext: (Boolean) -> Unit
) {
    LaunchedEffect(onboardingCompleted) {
        delay(1100)
        onNavigateNext(onboardingCompleted)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SeerahDeepGreen, SeerahEmerald))),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Explore,
                contentDescription = "شعار سِيرَة",
                tint = SeerahAntiqueGold,
                modifier = Modifier.size(72.dp)
            )
            Text(
                text = "سِيرَة",
                style = MaterialTheme.typography.displayLarge,
                color = SeerahIvory
            )
            Text(
                text = "SEERAH",
                style = MaterialTheme.typography.titleMedium,
                color = SeerahAntiqueGold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "«رحلة موثقة في حياة رسول الله محمد ﷺ»",
                style = MaterialTheme.typography.bodyLarge,
                color = SeerahParchment,
                textAlign = TextAlign.Center
            )
            Text(
                text = "نقرأ الأحداث، ونشاهد الأماكن، ونتتبع التاريخ، ونرجع إلى المصادر.",
                style = MaterialTheme.typography.bodyMedium,
                color = SeerahParchment.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit
) {
    val pages = listOf(
        Triple(
            "القراءة التاريخية والتوثيق العلمي",
            "كل حدث ورواية في سِيرَة مرتبطة بمصدرها وطبعتها ومحققها دون اختلاق أو أحكام حديثية آلية.",
            Icons.Outlined.MenuBook
        ),
        Triple(
            "الأطلس التاريخي والعسكري التفاعلي",
            "استكشف خرائط مكة والمدينة وبدر وأحد والخندق والهجرة بطبقات مستقلة تعمل دون إنترنت عبر GeoJSON وMBTiles.",
            Icons.Outlined.Map
        ),
        Triple(
            "مكتبتك العلمية الخاصة",
            "استورد كتبك ومصادرك بصيغ PDF وEPUB وTXT وJSON وXML وSQLite وفهرسها محليًا للبحث الفوري دون إنترنت.",
            Icons.Outlined.LibraryBooks
        )
    )
    var step by remember { mutableIntStateOf(0) }
    val current = pages[step]

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("سِيرَة", style = MaterialTheme.typography.headlineMedium, color = SeerahEmerald)
                TextButton(onClick = onFinish) { Text("تخطي") }
            }

            SeerahEncyclopediaCard(parchmentTone = true) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
                ) {
                    Icon(
                        imageVector = current.third,
                        contentDescription = null,
                        tint = SeerahEmerald,
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = current.first,
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = current.second,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${step + 1} / ${pages.size}", style = MaterialTheme.typography.labelLarge)
                Button(
                    onClick = {
                        if (step < pages.lastIndex) step++ else onFinish()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SeerahEmerald)
                ) {
                    Text(if (step < pages.lastIndex) "التالي" else "ابدأ الرحلة")
                }
            }
        }
    }
}

@Composable
fun HomeScreen(
    repository: SeerahRepository,
    lastReadTitle: String,
    lastReadRoute: String,
    onOpenRoute: (String) -> Unit,
    onOpenTimeline: () -> Unit,
    onOpenAtlas: (String) -> Unit,
    onOpenBattles: () -> Unit,
    onOpenPeople: () -> Unit,
    onOpenPlaces: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenHijra: () -> Unit,
    onOpenEvent: (Long) -> Unit
) {
    val events by repository.observeAllEvents().collectAsState(initial = emptyList())
    val books by repository.observeBooks().collectAsState(initial = emptyList())
    val importedBooksCount = books.count { it.isImported }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1 & 2. شعار التطبيق وترحيب بالمستخدم
        item {
            SeerahHeroBanner(
                badge = "موسوعة السيرة النبوية التفاعلية",
                title = "سِيرَة | رحلة موثقة في حياة رسول الله محمد ﷺ",
                subtitle = "نقرأ الأحداث، ونشاهد الأماكن، ونتتبع التاريخ، ونرجع إلى المصادر."
            )
        }

        // 10. بطاقة البحث الشامل
        item {
            SeerahEncyclopediaCard(onClick = onOpenSearch) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Outlined.Search, contentDescription = null, tint = SeerahEmerald)
                    Column(modifier = Modifier.weight(1f)) {
                        Text("البحث الشامل دون إنترنت", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "ابحث في الأحداث والغزوات والشخصيات والأماكن والكتب والآيات والأحاديث",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 3. بطاقة استكمال القراءة
        item {
            SeerahEncyclopediaCard(
                parchmentTone = true,
                onClick = {
                    if (lastReadRoute.isNotBlank()) onOpenRoute(lastReadRoute) else onOpenLibrary()
                }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("استكمال القراءة", style = MaterialTheme.typography.labelLarge, color = SeerahHistoricalBrown)
                        Text(
                            text = lastReadTitle.ifBlank { "لم تبدأ القراءة بعد — استورد كتبك الموثقة أو تصفح الخط الزمني" },
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Icon(Icons.Outlined.AutoStories, contentDescription = null, tint = SeerahAntiqueGold, modifier = Modifier.size(30.dp))
                }
            }
        }

        // قسم: اكتشف السيرة على الخريطة (5. بطاقة الأطلس التاريخي + الخرائط السريعة)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("اكتشف السيرة على الخريطة", style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = { onOpenAtlas("arabia") }) { Text("فتح الأطلس") }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(listOf(
                        AtlasMapDefinition.BADR,
                        AtlasMapDefinition.UHUD,
                        AtlasMapDefinition.HIJRA,
                        AtlasMapDefinition.KHANDAQ,
                        AtlasMapDefinition.MAKKAH,
                        AtlasMapDefinition.MADINAH
                    )) { mapDef ->
                        Card(
                            onClick = {
                                if (mapDef == AtlasMapDefinition.HIJRA) onOpenHijra() else onOpenAtlas(mapDef.mapId)
                            },
                            modifier = Modifier.width(210.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = SeerahParchment)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Outlined.Map, contentDescription = null, tint = SeerahHistoricalBrown)
                                Text(mapDef.arabicTitle, style = MaterialTheme.typography.titleMedium, color = SeerahInkDark)
                                Text(
                                    mapDef.description,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SeerahInkMuted,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }
            }
        }

        // قسم: رحلة عبر أحداث السيرة (4. بطاقة رحلة عبر الزمن + حدث مميز)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("رحلة عبر أحداث السيرة", style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = onOpenTimeline) { Text("الخط الزمني الكامل") }
                }

                val featured = events.firstOrNull()
                if (featured != null) {
                    SeerahEncyclopediaCard(onClick = { onOpenEvent(featured.id) }) {
                        CitationBadge(label = SeerahStage.fromCode(featured.stageCode).arabicTitle)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(featured.title, style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(featured.shortSummary, style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    EmptyVerifiedSourceState(
                        title = "بانتظار استيراد حزمة الأحداث الموثقة",
                        message = "لا يعرض تطبيق سِيرَة أحداثًا مخترعة أو غير موثقة. استورد ملفات الكتب أو حزم السيرة (JSON/EPUB/PDF/SQLite) لتفعيل الحدث المميز والخط الزمني.",
                        actionLabel = "استيراد المصادر الآن",
                        onAction = onOpenLibrary
                    )
                }
            }
        }

        // بطاقات الأقسام الرئيسية (6. الغزوات، 7. الشخصيات، 8. الأماكن، 9. المكتبة العلمية) بتصميم متنوع غير مكرر
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("أقسام الموسوعة", style = MaterialTheme.typography.titleLarge)

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    HomeSectionTile(
                        title = "الغزوات والسرايا",
                        subtitle = "الأطلس العسكري والمراحل",
                        icon = Icons.Outlined.Shield,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenBattles
                    )
                    HomeSectionTile(
                        title = "شخصيات السيرة",
                        subtitle = "الصحابة والرواة والقادة",
                        icon = Icons.Outlined.Groups,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenPeople
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    HomeSectionTile(
                        title = "الأماكن التاريخية",
                        subtitle = "مع درجة اليقين الجغرافي",
                        icon = Icons.Outlined.Place,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenPlaces
                    )
                    HomeSectionTile(
                        title = "المكتبة العلمية",
                        subtitle = "$importedBooksCount مستورد من ${books.size} مرجع",
                        icon = Icons.Outlined.MenuBook,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenLibrary
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeSectionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    SeerahEncyclopediaCard(modifier = modifier, onClick = onClick) {
        Icon(icon, contentDescription = null, tint = SeerahEmerald, modifier = Modifier.size(28.dp))
        Spacer(modifier = Modifier.height(10.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
