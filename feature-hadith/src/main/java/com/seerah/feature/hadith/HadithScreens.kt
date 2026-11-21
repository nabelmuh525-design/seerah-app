package com.seerah.feature.hadith

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.seerah.core.components.*
import com.seerah.data.repository.SeerahRepository
import com.seerah.domain.model.HadithGradeStatus

@Composable
fun HadithLibraryScreen(
    repository: SeerahRepository,
    onBack: () -> Unit,
    onOpenHadith: (Long) -> Unit,
    onOpenImport: () -> Unit
) {
    val hadiths by repository.observeAllHadiths().collectAsState(initial = emptyList())
    Scaffold(topBar = { SeerahTopBar(title = "مكتبة الأحاديث النبوية", subtitle = "توثيق التخريج ومرجع الحكم العلمي", onBack = onBack) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (hadiths.isEmpty()) {
                item {
                    EmptyVerifiedSourceState(
                        title = "لا توجد أحاديث مستوردة بعد",
                        message = "استورد كتب السنة المعتمدة (البخاري، مسلم، السنن الأربع، الموطأ، المسند) لعرض الأحاديث المرتبطة بالسيرة مع تخريجها ودرجتها من مراجعها العلمية.",
                        actionLabel = "استيراد كتب الحديث",
                        onAction = onOpenImport
                    )
                }
            } else {
                items(hadiths) { h ->
                    SeerahEncyclopediaCard(onClick = { onOpenHadith(h.id) }) {
                        CitationBadge(label = "${h.sourceName} ${if (h.hadithNumber.isNotBlank()) "• رقم ${h.hadithNumber}" else ""}")
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(h.hadithText, style = MaterialTheme.typography.bodyLarge, maxLines = 4)
                        Text("الراوي: ${h.narrator}", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
fun HadithDetailsScreen(
    hadithId: Long,
    repository: SeerahRepository,
    onBack: () -> Unit
) {
    var hadith by remember { mutableStateOf<com.seerah.database.entity.HadithEntity?>(null) }
    val grades by repository.observeGradesForHadith(hadithId).collectAsState(initial = emptyList())
    LaunchedEffect(hadithId) { hadith = repository.getHadithById(hadithId) }

    Scaffold(topBar = { SeerahTopBar(title = "تفاصيل الحديث وتخريجه", onBack = onBack) }) { padding ->
        val h = hadith
        if (h == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                EmptyVerifiedSourceState("الحديث غير متوفر", "لم يُعثر على سجل الحديث في القاعدة المحلية.")
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
                    Text(h.hadithText, style = MaterialTheme.typography.headlineMedium)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("الراوي: ${h.narrator}", style = MaterialTheme.typography.bodyMedium)
                    Text("المصدر: ${h.sourceName}", style = MaterialTheme.typography.bodyMedium)
                    if (h.bookTitle.isNotBlank()) Text("الكتاب والباب: ${h.bookTitle} — ${h.chapterTitle}", style = MaterialTheme.typography.bodyMedium)
                    if (h.hadithNumber.isNotBlank()) Text("رقم الحديث: ${h.hadithNumber}", style = MaterialTheme.typography.bodyMedium)
                    if (h.takhrij.isNotBlank()) Text("التخريج: ${h.takhrij}", style = MaterialTheme.typography.bodyMedium)
                }
            }
            item { Text("درجة الحديث ومرجع الحكم", style = MaterialTheme.typography.titleMedium) }
            if (grades.isEmpty()) {
                item {
                    EmptyVerifiedSourceState(
                        title = "غير محكوم عليه في البيانات المستوردة",
                        message = "لا يُصدر تطبيق سِيرَة أحكامًا حديثية آليًا؛ يُعرض الحكم فقط عند توفر مرجع علمي صريح."
                    )
                }
            } else {
                items(grades) { g ->
                    SeerahEncyclopediaCard {
                        CitationBadge(label = HadithGradeStatus.fromCode(g.gradeCode).arabicLabel)
                        Text("الحاكم على الحديث: ${g.gradingScholar}", style = MaterialTheme.typography.bodyMedium)
                        Text("مرجع الحكم: ${g.gradingSource}", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}
