package com.seerah.feature.timeline

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.seerah.core.components.*
import com.seerah.core.theme.*
import com.seerah.data.repository.SeerahRepository
import com.seerah.domain.model.HadithGradeStatus
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
    var selectedStage by remember { mutableStateOf<SeerahStage?>(null) }
    var selectedType by remember { mutableStateOf("ALL") }

    val filtered = remember(allEvents, selectedStage, selectedType) {
        allEvents.filter { ev ->
            (selectedStage == null || ev.stageCode == selectedStage?.code) &&
                (selectedType == "ALL" || ev.eventType == selectedType)
        }
    }

    Scaffold(
        topBar = {
            SeerahTopBar(
                title = "الخط الزمني للسيرة النبوية",
                subtitle = "20 مرحلة تاريخية من المولد إلى الوفاة"
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = selectedType == "ALL",
                            onClick = { selectedType = "ALL" },
                            label = { Text("جميع الأحداث") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedType == "GHAZWAH",
                            onClick = { selectedType = "GHAZWAH" },
                            label = { Text("الغزوات") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedType == "SARIYYAH",
                            onClick = { selectedType = "SARIYYAH" },
                            label = { Text("السرايا") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedType == "HIJRA_STAGE",
                            onClick = { selectedType = "HIJRA_STAGE" },
                            label = { Text("الهجرة") }
                        )
                    }
                }
            }

            item {
                Text("المراحل العشرون للسيرة", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = selectedStage == null,
                            onClick = { selectedStage = null },
                            label = { Text("كل المراحل") }
                        )
                    }
                    items(SeerahStage.entries) { stage ->
                        FilterChip(
                            selected = selectedStage == stage,
                            onClick = { selectedStage = stage },
                            label = { Text("${stage.order}. ${stage.arabicTitle}") }
                        )
                    }
                }
            }

            if (filtered.isEmpty()) {
                item {
                    EmptyVerifiedSourceState(
                        title = "لا توجد أحداث مستوردة في هذه المرحلة بعد",
                        message = "البنية الزمنية جاهزة للمراحل العشرين. استورد ملفات المصادر أو حزم البيانات الموثقة لعرض الأحداث وتواريخها وربطها بالخريطة.",
                        actionLabel = "استيراد ملفات المصادر",
                        onAction = onOpenImport
                    )
                }
            } else {
                items(filtered) { event ->
                    SeerahEncyclopediaCard(onClick = { onOpenEvent(event.id) }) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            CitationBadge(label = SeerahStage.fromCode(event.stageCode).arabicTitle)
                            if (!event.mapId.isNullOrBlank()) {
                                TextButton(onClick = { onOpenMap(event.mapId!!) }) {
                                    Icon(Icons.Outlined.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("على الخريطة")
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(event.title, style = MaterialTheme.typography.titleLarge)
                        if (event.shortSummary.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(event.shortSummary, style = MaterialTheme.typography.bodyMedium)
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
    val verses by repository.observeVersesForEvent(eventId).collectAsState(initial = emptyList())
    val hadiths by repository.observeHadithsForEvent(eventId).collectAsState(initial = emptyList())
    val isFav by repository.observeIsFavorite("EVENT", eventId).collectAsState(initial = false)

    LaunchedEffect(eventId) {
        event = repository.getEventById(eventId)
        event?.let {
            repository.preferences.saveLastRead(it.title, "event_details/${it.id}")
        }
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
        val ev = event
        if (ev == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                EmptyVerifiedSourceState(
                    title = "الحدث غير متوفر في القاعدة المحلية",
                    message = "يرجى التأكد من استيراد حزمة الأحداث والمصادر الموثقة."
                )
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SeerahEncyclopediaCard(parchmentTone = true) {
                    CitationBadge(label = SeerahStage.fromCode(ev.stageCode).arabicTitle)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(ev.title, style = MaterialTheme.typography.headlineMedium)
                    dates.forEach { d ->
                        Text(
                            text = "التاريخ: ${d.hijriDisplay} ${if (d.isDisputed) "(مختلف فيه بين المصادر)" else ""}",
                            style = MaterialTheme.typography.labelLarge,
                            color = SeerahHistoricalBrown
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(ev.shortSummary.ifBlank { "الوصف غير متوفر بعد في المصادر المستوردة." }, style = MaterialTheme.typography.bodyLarge)
                    if (ev.detailedAnalysis.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("التحليل والشرح (مفصول عن نص الرواية):", style = MaterialTheme.typography.labelLarge)
                        Text(ev.detailedAnalysis, style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!ev.mapId.isNullOrBlank()) {
                            Button(
                                onClick = { onOpenMap(ev.mapId!!) },
                                colors = ButtonDefaults.buttonColors(containerColor = SeerahEmerald)
                            ) {
                                Text("عرض على الخريطة")
                            }
                        }
                        OutlinedButton(onClick = { onOpenNarrationCompare(ev.id) }) {
                            Text("مقارنة الروايات")
                        }
                    }
                }
            }

            item { Text("الروايات الموثقة (${narrations.size})", style = MaterialTheme.typography.titleLarge) }
            if (narrations.isEmpty()) {
                item {
                    EmptyVerifiedSourceState(
                        title = "لا توجد روايات مرتبطة بعد",
                        message = "تُعرض الروايات هنا مع الإسناد والجزء والصفحة ودرجة الحكم عند استيراد المصادر."
                    )
                }
            } else {
                items(narrations) { nar ->
                    SeerahEncyclopediaCard {
                        CitationBadge(label = "${nar.sourceName} — ${HadithGradeStatus.fromCode(nar.gradeStatus).arabicLabel}")
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(nar.narrationText, style = MaterialTheme.typography.bodyLarge)
                        if (nar.gradeReference.isNotBlank()) {
                            Text("مرجع الحكم: ${nar.gradeReference}", style = MaterialTheme.typography.labelSmall, color = SeerahHistoricalBrown)
                        }
                    }
                }
            }

            if (verses.isNotEmpty()) {
                item { Text("الآيات القرآنية المرتبطة", style = MaterialTheme.typography.titleLarge) }
                items(verses) { v ->
                    SeerahEncyclopediaCard(parchmentTone = true) {
                        Text("سورة ${v.surahName} — آية ${v.ayahNumber}", style = MaterialTheme.typography.labelLarge)
                        Text(v.uthmaniText, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            if (hadiths.isNotEmpty()) {
                item { Text("الأحاديث المرتبطة", style = MaterialTheme.typography.titleLarge) }
                items(hadiths) { h ->
                    SeerahEncyclopediaCard {
                        Text("${h.sourceName} (${h.hadithNumber})", style = MaterialTheme.typography.labelLarge)
                        Text(h.hadithText, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    ev.previousEventId?.let { prev ->
                        OutlinedButton(onClick = { onOpenEvent(prev) }) { Text("الحدث السابق") }
                    }
                    ev.nextEventId?.let { next ->
                        Button(onClick = { onOpenEvent(next) }) { Text("الحدث اللاحق") }
                    }
                }
            }
        }
    }
}
