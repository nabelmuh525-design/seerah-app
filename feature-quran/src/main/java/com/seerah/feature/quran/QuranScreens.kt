package com.seerah.feature.quran

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.seerah.core.components.*
import com.seerah.data.repository.SeerahRepository

@Composable
fun QuranScreen(
    repository: SeerahRepository,
    onBack: () -> Unit,
    onOpenTafsir: (Long) -> Unit,
    onOpenImport: () -> Unit
) {
    val verses by repository.observeAllVerses().collectAsState(initial = emptyList())
    Scaffold(topBar = { SeerahTopBar(title = "القرآن الكريم والسيرة", subtitle = "النص العثماني وأسباب النزول الثابتة", onBack = onBack) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (verses.isEmpty()) {
                item {
                    EmptyVerifiedSourceState(
                        title = "لم يتم استيراد حزمة الآيات المرتبطة بالسيرة بعد",
                        message = "استورد حزمة النص العثماني والتفسير وأسباب النزول الثابتة لتفعيل الربط المباشر مع أحداث السيرة.",
                        actionLabel = "استيراد الحزمة",
                        onAction = onOpenImport
                    )
                }
            } else {
                items(verses) { v ->
                    SeerahEncyclopediaCard(parchmentTone = true, onClick = { onOpenTafsir(v.id) }) {
                        CitationBadge(label = "سورة ${v.surahName} — آية ${v.ayahNumber}")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(v.uthmaniText, style = MaterialTheme.typography.headlineMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun TafsirScreen(
    verseId: Long,
    repository: SeerahRepository,
    onBack: () -> Unit
) {
    var verse by remember { mutableStateOf<com.seerah.database.entity.QuranVerseEntity?>(null) }
    LaunchedEffect(verseId) { verse = repository.getVerseById(verseId) }

    Scaffold(topBar = { SeerahTopBar(title = "التفسير وأسباب النزول", onBack = onBack) }) { padding ->
        val v = verse
        if (v == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                EmptyVerifiedSourceState("الآية غير متوفرة", "يرجى استيراد حزمة القرآن والتفسير.")
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
                    Text("سورة ${v.surahName} — آية ${v.ayahNumber}", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(v.uthmaniText, style = MaterialTheme.typography.headlineMedium)
                }
            }
            item {
                SeerahEncyclopediaCard {
                    Text("التفسير", style = MaterialTheme.typography.titleMedium)
                    Text(v.tafsirSummary.ifBlank { "التفسير غير مستورد لهذه الآية بعد." }, style = MaterialTheme.typography.bodyLarge)
                    if (v.tafsirSource.isNotBlank()) CitationBadge(label = v.tafsirSource)
                }
            }
            item {
                SeerahEncyclopediaCard {
                    Text("سبب النزول", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = if (v.asbabAlNuzul.isBlank()) "لا يوجد سبب نزول ثابت مسجل لهذه الآية."
                        else "${v.asbabAlNuzul} ${if (!v.asbabVerified) "(يحتاج مراجعة وتثبت)" else ""}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (v.asbabSource.isNotBlank()) CitationBadge(label = v.asbabSource)
                }
            }
        }
    }
}
