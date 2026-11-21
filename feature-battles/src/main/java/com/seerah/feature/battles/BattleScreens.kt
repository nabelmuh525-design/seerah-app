package com.seerah.feature.battles

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.seerah.core.components.*
import com.seerah.core.theme.SeerahEmerald
import com.seerah.data.repository.SeerahRepository
import com.seerah.domain.model.AtlasMapDefinition

@Composable
fun BattleEncyclopediaScreen(
    repository: SeerahRepository,
    onOpenBattleDetails: (Long) -> Unit,
    onOpenBattleMap: (String) -> Unit,
    onOpenExpeditions: () -> Unit,
    onOpenImport: () -> Unit
) {
    val battles by repository.observeBattles().collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            SeerahTopBar(
                title = "موسوعة الغزوات",
                subtitle = "قسم مستقل عن السرايا والبعوث",
                actions = {
                    TextButton(onClick = onOpenExpeditions) {
                        Text("السرايا والبعوث", color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SeerahHeroBanner(
                    badge = "الأطلس العسكري للسيرة النبوية",
                    title = "خرائط الغزوات المستقلة",
                    subtitle = "اختر غزوة لفتح خريطتها التفاعلية، ولا تُعرض أعداد القوات أو تفاصيل التحركات إلا بمستند علمي."
                )
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf(AtlasMapDefinition.BADR, AtlasMapDefinition.UHUD, AtlasMapDefinition.KHANDAQ).forEach { def ->
                        OutlinedButton(onClick = { onOpenBattleMap(def.mapId) }, modifier = Modifier.weight(1f)) {
                            Text(def.arabicTitle)
                        }
                    }
                }
            }

            if (battles.isEmpty()) {
                item {
                    EmptyVerifiedSourceState(
                        title = "لا توجد غزوات مستوردة بعد",
                        message = "استورد حزمة بيانات الغزوات الموثقة أو كتب المغازي والسيرة لعرض التفاصيل والأسباب والنتائج والروايات المختلفة.",
                        actionLabel = "استيراد المصادر",
                        onAction = onOpenImport
                    )
                }
            } else {
                items(battles) { battle ->
                    SeerahEncyclopediaCard(onClick = { onOpenBattleDetails(battle.id) }) {
                        Text(battle.title, style = MaterialTheme.typography.titleLarge)
                        if (battle.shortSummary.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(battle.shortSummary, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BattleDetailsScreen(
    battleId: Long,
    repository: SeerahRepository,
    onBack: () -> Unit,
    onOpenBattleMap: (String) -> Unit,
    onOpenNarrationCompare: (Long) -> Unit
) {
    var battle by remember { mutableStateOf<com.seerah.database.entity.EventEntity?>(null) }
    val narrations by repository.observeNarrationsForEvent(battleId).collectAsState(initial = emptyList())

    LaunchedEffect(battleId) {
        battle = repository.getEventById(battleId)
    }

    Scaffold(topBar = { SeerahTopBar(title = battle?.title ?: "تفاصيل الغزوة", onBack = onBack) }) { padding ->
        val b = battle
        if (b == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                EmptyVerifiedSourceState("الغزوة غير متوفرة", "يرجى استيراد بيانات الغزوة من المصادر الموثقة.")
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
                    Text(b.title, style = MaterialTheme.typography.headlineMedium)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SeerahEncyclopediaCard(modifier = Modifier.weight(1f)) {
                            Text("النوع", style = MaterialTheme.typography.labelSmall)
                            Text("غزوة موثقة", style = MaterialTheme.typography.labelLarge)
                        }
                        SeerahEncyclopediaCard(modifier = Modifier.weight(1f)) {
                            Text("الخريطة", style = MaterialTheme.typography.labelSmall)
                            Text(b.mapId ?: "غير محددة", style = MaterialTheme.typography.labelLarge)
                        }
                        SeerahEncyclopediaCard(modifier = Modifier.weight(1f)) {
                            Text("حالة التوثيق", style = MaterialTheme.typography.labelSmall)
                            Text(b.verificationStatus, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                    if (b.causeText.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("سبب الغزوة:", style = MaterialTheme.typography.labelLarge)
                        Text(b.causeText, style = MaterialTheme.typography.bodyMedium)
                    }
                    if (b.preparationText.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("الاستعداد:", style = MaterialTheme.typography.labelLarge)
                        Text(b.preparationText, style = MaterialTheme.typography.bodyMedium)
                    }
                    if (b.outcomeText.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("النتائج:", style = MaterialTheme.typography.labelLarge)
                        Text(b.outcomeText, style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onOpenBattleMap(b.mapId ?: "badr") },
                            colors = ButtonDefaults.buttonColors(containerColor = SeerahEmerald)
                        ) { Text("الخريطة التفاعلية") }
                        OutlinedButton(onClick = { onOpenNarrationCompare(b.id) }) { Text("الروايات (${narrations.size})") }
                    }
                }
            }
        }
    }
}

@Composable
fun ExpeditionsScreen(
    repository: SeerahRepository,
    onBack: () -> Unit,
    onOpenDetails: (Long) -> Unit,
    onOpenImport: () -> Unit
) {
    val expeditions by repository.observeExpeditions().collectAsState(initial = emptyList())
    Scaffold(
        topBar = { SeerahTopBar(title = "السرايا والبعوث", subtitle = "مفصولة علميًا عن الغزوات", onBack = onBack) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (expeditions.isEmpty()) {
                item {
                    EmptyVerifiedSourceState(
                        title = "لا توجد سرايا أو بعوث مستوردة بعد",
                        message = "يُفصل هذا القسم السرايا والبعوث عن الغزوات. استورد المصادر الموثقة لعرضها.",
                        actionLabel = "استيراد المصادر",
                        onAction = onOpenImport
                    )
                }
            } else {
                items(expeditions) { exp ->
                    SeerahEncyclopediaCard(onClick = { onOpenDetails(exp.id) }) {
                        Text(exp.title, style = MaterialTheme.typography.titleMedium)
                        Text(exp.shortSummary, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
