package com.seerah.feature.timeline

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.seerah.core.R as CoreR
import com.seerah.core.components.*
import com.seerah.core.theme.*
import com.seerah.data.repository.SeerahRepository
import com.seerah.domain.model.HadithGradeStatus
import com.seerah.domain.model.ContentReviewStatus
import com.seerah.domain.model.SeerahStage
import kotlinx.coroutines.launch

@Composable
fun TimelineScreen(
    repository: SeerahRepository,
    onOpenEvent: (Long) -> Unit,
    onOpenMap: (String) -> Unit,
    onOpenImport: () -> Unit
) {
    val allEvents by repository.observeAllEvents().collectAsState(initial = emptyList())
    var selectedType by remember { mutableStateOf("ALL") }
    var highlightedIndex by remember { mutableIntStateOf(4) } // الهجرة إلى المدينة مميزة افتراضيًا

    val filtered = remember(allEvents, selectedType) {
        allEvents.filter { ev -> selectedType == "ALL" || ev.eventType == selectedType }
    }

    Scaffold(
        topBar = {
            SeerahTopBar(
                title = "الخط الزمني للسيرة",
                subtitle = "من مولد النبي ﷺ حتى الوفاة — موثّق بالمصادر"
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val filters = listOf(
                        "ALL" to "جميع المحطات",
                        "GHAZWAH" to "الغزوات",
                        "HIJRA_STAGE" to "الهجرة",
                        "TREATY" to "الصلح والعهد"
                    )
                    items(filters) { (code, label) ->
                        FilterChip(
                            selected = selectedType == code,
                            onClick = { selectedType = code },
                            label = { Text(label) }
                        )
                    }
                }
            }

            items(filtered.size) { index ->
                val event = filtered[index]
                val isSelected = index == highlightedIndex.coerceIn(0, (filtered.size - 1).coerceAtLeast(0))
                val thumbRes = when (event.mapId) {
                    "badr", "uhud", "khandaq" -> CoreR.drawable.art_battle_badr
                    "madinah", "hijra" -> CoreR.drawable.art_hero_masjid_nabawi
                    else -> CoreR.drawable.art_makkah_kaaba
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            highlightedIndex = index
                            onOpenEvent(event.id)
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // صورة دائرية مصغرة للمحطة التاريخية
                    Image(
                        painter = painterResource(id = thumbRes),
                        contentDescription = event.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .border(2.dp, if (isSelected) SeerahEmerald else SeerahAntiqueGold, CircleShape)
                    )

                    // العمود الزمني الذهبي والعقدة
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(28.dp)
                    ) {
                        Box(modifier = Modifier.width(2.5.dp).height(24.dp).background(SeerahAntiqueGold))
                        Box(
                            modifier = Modifier
                                .size(if (isSelected) 14.dp else 10.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) SeerahEmerald else SeerahAntiqueGold)
                                .border(2.dp, SeerahIvory, CircleShape)
                        )
                        Box(modifier = Modifier.width(2.5.dp).height(24.dp).background(SeerahAntiqueGold))
                    }

                    // بطاقة الحدث (خضراء زمردية عند التحديد، عاجية دافئة لبقية المحطات)
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) SeerahEmerald else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        border = BorderStroke(1.dp, SeerahAntiqueGold.copy(alpha = 0.7f))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = event.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) SeerahIvory else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = SeerahStage.fromCode(event.stageCode).arabicTitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) SeerahGoldLight else SeerahHistoricalBrown
                            )
                            if (event.shortSummary.isNotBlank()) {
                                Text(
                                    text = event.shortSummary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isSelected) SeerahParchment else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EventDetailsScreen(
    eventId: Long,
    repository: SeerahRepository,
    onBack: () -> Unit,
    onOpenMap: (String) -> Unit,
    onOpenNarrationCompare: (Long) -> Unit,
    onOpenEvent: (Long) -> Unit
) {
    val scope = rememberCoroutineScope()
    var event by remember { mutableStateOf<com.seerah.database.entity.EventEntity?>(null) }
    val dates by repository.observeEventDates(eventId).collectAsState(initial = emptyList())
    val narrations by repository.observeNarrationsForEvent(eventId).collectAsState(initial = emptyList())
    val formalSources by repository.observeEventSources(eventId).collectAsState(initial = emptyList())
    val isFav by repository.observeIsFavorite("EVENT", eventId).collectAsState(initial = false)

    LaunchedEffect(eventId) {
        event = repository.getEventById(eventId)
        event?.let { repository.preferences.saveLastRead(it.title, "event_details/${it.id}") }
    }

    Scaffold(
        topBar = {
            SeerahTopBar(
                title = event?.title ?: "تفاصيل الحدث",
                onBack = onBack,
                actions = {
                    IconButton(onClick = {
                        event?.let { ev ->
                            scope.launch {
                                repository.toggleFavorite("EVENT", ev.id, ev.title, ev.shortSummary, isFav)
                            }
                        }
                    }) {
                        Icon(
                            imageVector = if (isFav) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "حفظ في المفضلة"
                        )
                    }
                }
            )
        }
    ) { padding ->
        val ev = event ?: return@Scaffold
        val bannerRes = when (ev.mapId) {
            "badr", "uhud", "khandaq" -> CoreR.drawable.art_battle_badr
            "madinah", "hijra" -> CoreR.drawable.art_hero_masjid_nabawi
            else -> CoreR.drawable.art_makkah_kaaba
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Image(
                    painter = painterResource(id = bannerRes),
                    contentDescription = ev.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(200.dp)
                )
            }

            // شريط المعلومات الثلاثي كما في التصور
            item {
                val primaryDate = dates.firstOrNull()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SeerahEncyclopediaCard(modifier = Modifier.weight(1f), parchmentTone = true) {
                        Text("التاريخ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(primaryDate?.hijriDisplay ?: "موثق", style = MaterialTheme.typography.labelLarge)
                    }
                    SeerahEncyclopediaCard(modifier = Modifier.weight(1f), parchmentTone = true) {
                        Text("الموقع", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(ev.mapId ?: "الحجاز", style = MaterialTheme.typography.labelLarge)
                    }
                    SeerahEncyclopediaCard(modifier = Modifier.weight(1f), parchmentTone = true) {
                        Text("التوثيق", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        val hasFormalCitation = formalSources.any { it.volume.isNotBlank() && it.page.isNotBlank() }
                        val statusLabel = if (ev.verificationStatus == "VERIFIED" && hasFormalCitation) {
                            ContentReviewStatus.VERIFIED.arabicLabel
                        } else {
                            val parsed = ContentReviewStatus.fromCode(ev.verificationStatus)
                            if (parsed == ContentReviewStatus.VERIFIED) ContentReviewStatus.NEEDS_COMPLETION.arabicLabel else parsed.arabicLabel
                        }
                        Text(statusLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (ev.causeText.isNotBlank()) {
                        SeerahEncyclopediaCard {
                            Text("أسباب الحدث", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(ev.causeText, style = MaterialTheme.typography.bodyLarge)
                        }
                    }

                    SeerahEncyclopediaCard {
                        Text("تفاصيل الحدث", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(ev.shortSummary, style = MaterialTheme.typography.bodyLarge)
                    }

                    if (ev.outcomeText.isNotBlank()) {
                        SeerahEncyclopediaCard {
                            Text("النتائج والدروس", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(ev.outcomeText, style = MaterialTheme.typography.bodyLarge)
                        }
                    }

                    Button(
                        onClick = { onOpenMap(ev.mapId ?: "hijra") },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                    ) {
                        Icon(Icons.Outlined.Map, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("عرض على الخريطة التفاعلية", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }

            if (narrations.isNotEmpty()) {
                item {
                    Text(
                        "المصادر والروايات الموثقة",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                items(narrations) { nar ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        SeerahEncyclopediaCard(parchmentTone = true) {
                            CitationBadge(label = "${nar.sourceName} • ${HadithGradeStatus.fromCode(nar.gradeStatus).arabicLabel}")
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(nar.narrationText, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}
