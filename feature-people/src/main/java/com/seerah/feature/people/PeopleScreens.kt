package com.seerah.feature.people

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    var prophetTab by remember { mutableIntStateOf(0) }
    val filtered = remember(allPeople, selectedCategory) {
        if (selectedCategory == null) allPeople else allPeople.filter { it.category == selectedCategory?.code }
    }

    Scaffold(topBar = { SeerahTopBar(title = "موسوعة الشخصيات", onBack = onBack) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // بطاقة النبي محمد ﷺ بالشمسة الزمردية والذهبية (دون أي تجسيد بصري)
            item {
                SeerahEncyclopediaCard(parchmentTone = true) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(148.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = CoreR.drawable.art_shamsa_medallion),
                                contentDescription = "شمسة إسلامية",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                            Text(
                                text = "محمد ﷺ",
                                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 24.sp),
                                color = SeerahGoldLight,
                                textAlign = TextAlign.Center
                            )
                        }
                        Text("النبي محمد ﷺ", style = MaterialTheme.typography.headlineMedium, color = SeerahEmerald)
                        Text("رسول الله وخاتم الأنبياء", style = MaterialTheme.typography.bodyMedium, color = SeerahHistoricalBrown)

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("سيرته", "صفاته", "مواقفه").forEachIndexed { idx, label ->
                                FilterChip(
                                    selected = prophetTab == idx,
                                    onClick = { prophetTab = idx },
                                    label = { Text(label) }
                                )
                            }
                        }

                        val tabText = when (prophetTab) {
                            0 -> "هو محمد بن عبد الله بن عبد المطلب الهاشمي القرشي ﷺ، بعثه الله رحمةً للعالمين فبلّغ الرسالة وأدى الأمانة (صحيح البخاري؛ سيرة ابن هشام)."
                            1 -> "كان ﷺ أحسن الناس خُلقًا، صادقًا أمينًا رحيمًا بالمؤمنين، قالت عائشة رضي الله عنها: «كان خُلقه القرآن» (صحيح مسلم)."
                            else -> "عفا عن أهل مكة يوم الفتح رغم ما لقيه منهم، وآخى بين المهاجرين والأنصار، ورسّخ العدل والرحمة (الصحيحان)."
                        }
                        Text(tabText, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                    }
                }
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                            label = { Text("الكل (${allPeople.size})") }
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

            items(filtered) { person ->
                SeerahEncyclopediaCard(onClick = { onOpenPerson(person.id) }) {
                    CitationBadge(label = PersonCategory.fromCode(person.category).arabicTitle)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(person.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (person.roleInSeerah.isNotBlank()) {
                        Text(person.roleInSeerah, style = MaterialTheme.typography.bodyMedium)
                    }
                    if (person.primarySources.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("المصدر: ${person.primarySources}", style = MaterialTheme.typography.labelSmall, color = SeerahHistoricalBrown)
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
    LaunchedEffect(personId) { person = repository.getPersonById(personId) }

    Scaffold(topBar = { SeerahTopBar(title = person?.name ?: "تفاصيل الشخصية", onBack = onBack) }) { padding ->
        val p = person ?: return@Scaffold
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SeerahEncyclopediaCard(parchmentTone = true) {
                    CitationBadge(label = PersonCategory.fromCode(p.category).arabicTitle)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(p.name, style = MaterialTheme.typography.headlineMedium, color = SeerahEmerald)
                    if (p.kunyah.isNotBlank()) Text("الكنية: ${p.kunyah}", style = MaterialTheme.typography.bodyMedium)
                    if (p.lineage.isNotBlank()) Text("النسب: ${p.lineage}", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(p.historicalBio, style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    CitationBadge(label = "المصادر: ${p.primarySources}")
                }
            }
        }
    }
}
