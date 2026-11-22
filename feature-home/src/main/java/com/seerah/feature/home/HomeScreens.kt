package com.seerah.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seerah.core.R as CoreR
import com.seerah.core.components.*
import com.seerah.core.theme.*
import com.seerah.data.repository.SeerahRepository
import com.seerah.domain.model.AtlasMapDefinition
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onboardingCompleted: Boolean,
    onNavigateNext: (Boolean) -> Unit
) {
    LaunchedEffect(onboardingCompleted) {
        delay(1200)
        onNavigateNext(onboardingCompleted)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = CoreR.drawable.art_hero_masjid_nabawi),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            SeerahDeepGreen.copy(alpha = 0.65f),
                            Color.Transparent,
                            SeerahDeepGreen.copy(alpha = 0.85f)
                        )
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 92.dp, start = 24.dp, end = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "السِّيرَة النَّبَوِيَّة",
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 38.sp),
                color = SeerahGoldLight,
                textAlign = TextAlign.Center
            )
            Text(
                text = "سِيرَة — موسوعة تفاعلية شاملة",
                style = MaterialTheme.typography.titleMedium,
                color = SeerahParchment,
                textAlign = TextAlign.Center
            )
        }
        Text(
            text = "«رحلة موثقة في حياة رسول الله محمد ﷺ»",
            style = MaterialTheme.typography.bodyLarge,
            color = SeerahParchment,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 52.dp, start = 24.dp, end = 24.dp)
        )
    }
}

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = CoreR.drawable.art_hero_masjid_nabawi),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(SeerahDeepGreen.copy(alpha = 0.6f), Color.Transparent, SeerahDeepGreen.copy(alpha = 0.92f))
                    )
                )
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.padding(top = 60.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "السِّيرَة النَّبَوِيَّة",
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 40.sp),
                    color = SeerahGoldLight
                )
                Text(
                    text = "موسوعة تفاعلية شاملة",
                    style = MaterialTheme.typography.titleLarge,
                    color = SeerahParchment
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                Text(
                    text = "نقرأ الأحداث، ونشاهد الأماكن، ونتتبع التاريخ، ونرجع إلى المصادر.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = SeerahIvory,
                    textAlign = TextAlign.Center
                )
                SeerahGoldButton(
                    text = "ابدأ الرحلة  ←",
                    onClick = onFinish,
                    modifier = Modifier.fillMaxWidth(0.75f)
                )
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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // بطاقة البداية الفخمة المطابقة تمامًا للتصور البصري
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(410.dp)
                    .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            ) {
                Image(
                    painter = painterResource(id = CoreR.drawable.art_hero_masjid_nabawi),
                    contentDescription = "المسجد النبوي",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    SeerahDeepGreen.copy(alpha = 0.55f),
                                    Color.Transparent,
                                    SeerahDeepGreen.copy(alpha = 0.88f)
                                )
                            )
                        )
                )
                // شريط علوي شفاف للبحث
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = SeerahDeepGreen.copy(alpha = 0.7f),
                        border = BorderStroke(1.dp, SeerahAntiqueGold.copy(alpha = 0.6f))
                    ) {
                        Text(
                            text = "سِيرَة | SEERAH",
                            style = MaterialTheme.typography.labelLarge,
                            color = SeerahGoldLight,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                    IconButton(
                        onClick = onOpenSearch,
                        colors = IconButtonDefaults.iconButtonColors(containerColor = SeerahDeepGreen.copy(alpha = 0.7f))
                    ) {
                        Icon(Icons.Outlined.Search, contentDescription = "بحث", tint = SeerahGoldLight)
                    }
                }

                // العنوان الذهبي وزر ابدأ الرحلة
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "السِّيرَة النَّبَوِيَّة",
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 36.sp),
                        color = SeerahGoldLight,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "موسوعة تفاعلية شاملة • خط زمني • خريطة تفاعلية • معرفة موثقة",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SeerahParchment,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    SeerahGoldButton(
                        text = "ابدأ الرحلة  ←",
                        onClick = onOpenTimeline,
                        modifier = Modifier.fillMaxWidth(0.72f)
                    )
                }
            }
        }

        // شريط استكمال القراءة
        if (lastReadTitle.isNotBlank()) {
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    SeerahEncyclopediaCard(
                        parchmentTone = true,
                        onClick = { onOpenRoute(lastReadRoute) }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("استكمال القراءة", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(lastReadTitle, style = MaterialTheme.typography.titleMedium)
                            }
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }

        // قسم: اكتشف السيرة على الخريطة التفاعلية
        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("اكتشف السيرة على الخريطة", style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = { onOpenAtlas("hijra") }) { Text("فتح الأطلس") }
                }

                Card(
                    onClick = { onOpenAtlas("hijra") },
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.5.dp, SeerahAntiqueGold),
                    modifier = Modifier.fillMaxWidth().height(175.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Image(
                            painter = painterResource(id = CoreR.drawable.art_map_hijaz_atlas),
                            contentDescription = "خريطة الحجاز التاريخية",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(SeerahDeepGreen.copy(alpha = 0.85f), Color.Transparent)
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(18.dp)
                                .fillMaxWidth(0.75f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("الأطلس التاريخي التفاعلي", style = MaterialTheme.typography.titleLarge, color = SeerahGoldLight)
                            Text("مكة المكرمة • المدينة المنورة • مسار الهجرة • بدر • أحد", style = MaterialTheme.typography.bodyMedium, color = SeerahIvory)
                        }
                    }
                }
            }
        }

        // قسم: أبرز محطات السيرة (بطاقات أفقية مصورة)
        if (events.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("رحلة عبر أحداث السيرة", style = MaterialTheme.typography.titleLarge)
                        TextButton(onClick = onOpenTimeline) { Text("عرض الكل") }
                    }
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(events.take(6)) { ev ->
                            val imgRes = when (ev.mapId) {
                                "badr", "uhud", "khandaq" -> CoreR.drawable.art_battle_badr
                                "madinah", "hijra" -> CoreR.drawable.art_hero_masjid_nabawi
                                else -> CoreR.drawable.art_makkah_kaaba
                            }
                            Card(
                                onClick = { onOpenEvent(ev.id) },
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, SeerahAntiqueGold.copy(alpha = 0.7f)),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier.width(210.dp)
                            ) {
                                Column {
                                    Image(
                                        painter = painterResource(id = imgRes),
                                        contentDescription = ev.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxWidth().height(105.dp)
                                    )
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(ev.title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                                        Text(ev.shortSummary, style = MaterialTheme.typography.labelSmall, maxLines = 2, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // بطاقات الوصول السريع للأقسام الرئيسية
        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("أقسام الموسوعة", style = MaterialTheme.typography.titleLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    LuxuryFeatureTile("الغزوات والسرايا", "بدر • أحد • الخندق", Icons.Outlined.Shield, Modifier.weight(1f), onOpenBattles)
                    LuxuryFeatureTile("موسوعة الشخصيات", "النبي ﷺ والصحابة", Icons.Outlined.Groups, Modifier.weight(1f), onOpenPeople)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    LuxuryFeatureTile("مسار الهجرة", "من مكة إلى المدينة", Icons.Outlined.Route, Modifier.weight(1f), onOpenHijra)
                    LuxuryFeatureTile("المكتبة والمصادر", "توثيق واستيراد الكتب", Icons.Outlined.MenuBook, Modifier.weight(1f), onOpenLibrary)
                }
            }
        }
    }
}

@Composable
private fun LuxuryFeatureTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    SeerahEncyclopediaCard(modifier = modifier, parchmentTone = true, onClick = onClick) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
