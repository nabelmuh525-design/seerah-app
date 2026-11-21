package com.seerah.feature.people

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.seerah.core.components.*
import com.seerah.data.repository.SeerahRepository
import com.seerah.domain.model.PersonCategory

@Composable
fun PeopleEncyclopediaScreen(
    repository: SeerahRepository,
    onBack: () -> Unit,
    onOpenPerson: (Long) -> Unit,
    onOpenImport: () -> Unit
) {
    val allPeople by repository.observeAllPeople().collectAsState(initial = emptyList())
    var selectedCategory by remember { mutableStateOf<PersonCategory?>(null) }
    val filtered = remember(allPeople, selectedCategory) {
        if (selectedCategory == null) allPeople else allPeople.filter { it.category == selectedCategory?.code }
    }

    Scaffold(topBar = { SeerahTopBar(title = "موسوعة الشخصيات", subtitle = "9 تصنيفات علمية موثقة", onBack = onBack) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                            label = { Text("الكل") }
                        )
                    }
                    items(PersonCategory.entries) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat.arabicTitle) }
                        )
                    }
                }
            }

            if (filtered.isEmpty()) {
                item {
                    EmptyVerifiedSourceState(
                        title = "لا توجد تراجم شخصيات مستوردة في هذا التصنيف بعد",
                        message = "استورد كتب التراجم (الإصابة، الاستيعاب، أسد الغابة، الطبقات الكبرى) أو حزمة JSON الموثقة. يلتزم التطبيق بعدم تجسيد النبي ﷺ أو ادعاء صور حقيقية للصحابة.",
                        icon = Icons.Outlined.PersonOutline,
                        actionLabel = "استيراد المصادر",
                        onAction = onOpenImport
                    )
                }
            } else {
                items(filtered) { person ->
                    SeerahEncyclopediaCard(onClick = { onOpenPerson(person.id) }) {
                        CitationBadge(label = PersonCategory.fromCode(person.category).arabicTitle)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(person.name, style = MaterialTheme.typography.titleLarge)
                        if (person.kunyah.isNotBlank()) {
                            Text("الكنية: ${person.kunyah}", style = MaterialTheme.typography.labelSmall)
                        }
                        if (person.roleInSeerah.isNotBlank()) {
                            Text(person.roleInSeerah, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PersonDetailsScreen(
    personId: Long,
    repository: SeerahRepository,
    onBack: () -> Unit,
    onOpenEvent: (Long) -> Unit
) {
    var person by remember { mutableStateOf<com.seerah.database.entity.PersonEntity?>(null) }
    val linkedEvents by repository.observePersonEvents(personId).collectAsState(initial = emptyList())

    LaunchedEffect(personId) {
        person = repository.getPersonById(personId)
    }

    Scaffold(topBar = { SeerahTopBar(title = person?.name ?: "تفاصيل الشخصية", onBack = onBack) }) { padding ->
        val p = person
        if (p == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                EmptyVerifiedSourceState("الشخصية غير متوفرة", "لم يتم العثور على الترجمة في قاعدة البيانات المحلية.")
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
                    CitationBadge(label = PersonCategory.fromCode(p.category).arabicTitle)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(p.name, style = MaterialTheme.typography.headlineMedium)
                    if (p.kunyah.isNotBlank()) Text("الكنية: ${p.kunyah}", style = MaterialTheme.typography.bodyMedium)
                    if (p.lineage.isNotBlank()) Text("النسب: ${p.lineage}", style = MaterialTheme.typography.bodyMedium)
                    if (p.historicalBio.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(p.historicalBio, style = MaterialTheme.typography.bodyLarge)
                    }
                    if (p.primarySources.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        CitationBadge(label = "المصادر: ${p.primarySources}")
                    }
                }
            }

            item { Text("الأحداث المرتبطة (${linkedEvents.size})", style = MaterialTheme.typography.titleLarge) }
            items(linkedEvents) { link ->
                SeerahEncyclopediaCard(onClick = { onOpenEvent(link.eventId) }) {
                    Text(link.roleDescription.ifBlank { "مشاركة في الحدث رقم ${link.eventId}" }, style = MaterialTheme.typography.bodyMedium)
                    if (link.sourceCitation.isNotBlank()) CitationBadge(label = link.sourceCitation)
                }
            }
        }
    }
}
