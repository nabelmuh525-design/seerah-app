package com.seerah.feature.atlas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.seerah.core.components.*
import com.seerah.core.theme.*
import com.seerah.data.repository.SeerahRepository
import com.seerah.domain.model.AtlasMapDefinition
import com.seerah.domain.model.GeographicCertainty
import com.seerah.domain.model.MapLayerType

@Composable
fun HistoricalAtlasScreen(
    initialMapId: String,
    repository: SeerahRepository,
    onOpenLayers: () -> Unit,
    onOpenLegend: () -> Unit,
    onOpenBattleMap: (String) -> Unit,
    onOpenPlaceDetails: (Long) -> Unit,
    onOpenImport: () -> Unit
) {
    var selectedMap by remember { mutableStateOf(AtlasMapDefinition.fromId(initialMapId)) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var selectedFeature by remember { mutableStateOf<com.seerah.domain.model.GeoFeatureModel?>(null) }
    val enabledLayers = remember { mutableStateListOf<String>().apply { addAll(MapLayerType.entries.map { it.key }) } }

    val features by repository.observeBattleFeatures(selectedMap.mapId).collectAsState(initial = emptyList())
    val places by repository.observePlacesForMap(selectedMap.mapId).collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            SeerahTopBar(
                title = "الأطلس التاريخي التفاعلي",
                subtitle = selectedMap.arabicTitle,
                actions = {
                    IconButton(onClick = onOpenLayers) {
                        Icon(Icons.Outlined.Layers, contentDescription = "طبقات الخريطة")
                    }
                    IconButton(onClick = onOpenLegend) {
                        Icon(Icons.Outlined.HelpOutline, contentDescription = "مفتاح الرموز")
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
            // اختيار الخريطة من الخرائط الـ 11
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(AtlasMapDefinition.entries) { mapDef ->
                        FilterChip(
                            selected = selectedMap == mapDef,
                            onClick = {
                                selectedMap = mapDef
                                zoom = 1f
                                panOffset = Offset.Zero
                            },
                            label = { Text(mapDef.arabicTitle) }
                        )
                    }
                }
            }

            // لوحة الخريطة التفاعلية مع أزرار التكبير العائمة وبطاقة المسار السفلية
            item {
                Box(modifier = Modifier.fillMaxWidth().height(390.dp)) {
                    HistoricalMapCanvas(
                        mapDefinition = selectedMap,
                        enabledLayers = enabledLayers.toSet(),
                        features = features,
                        zoom = zoom,
                        panOffset = panOffset,
                        onTransform = { zDelta, pDelta ->
                            zoom = (zoom * zDelta).coerceIn(0.6f, 4.0f)
                            panOffset += pDelta
                        },
                        onFeatureSelected = { selectedFeature = it },
                        modifier = Modifier.fillMaxSize()
                    )

                    // أزرار + / - العائمة على جانب الخريطة
                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilledTonalIconButton(onClick = { zoom = (zoom + 0.35f).coerceAtMost(4.0f) }) {
                            Text("+", style = MaterialTheme.typography.titleLarge)
                        }
                        FilledTonalIconButton(onClick = { zoom = (zoom - 0.35f).coerceAtLeast(0.6f) }) {
                            Text("-", style = MaterialTheme.typography.titleLarge)
                        }
                        FilledTonalIconButton(onClick = { zoom = 1f; panOffset = Offset.Zero }) {
                            Icon(Icons.Outlined.MyLocation, contentDescription = "إعادة ضبط")
                        }
                    }

                    // بطاقة المعلومات العائمة أسفل الخريطة
                    selectedFeature?.let { feat ->
                        Card(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(12.dp),
                            colors = CardDefaults.cardColors(containerColor = SeerahIvory.copy(alpha = 0.96f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SeerahAntiqueGold)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(feat.label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                Text(feat.certainty.arabicLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("المصدر: ${feat.sourceCitation}", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }

            // بطاقة المعلم المختار باللمس على الخريطة
            selectedFeature?.let { feat ->
                item {
                    SeerahEncyclopediaCard(parchmentTone = true) {
                        CitationBadge(label = feat.certainty.arabicLabel)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(feat.label, style = MaterialTheme.typography.titleMedium)
                        Text("النوع: ${feat.featureType} • الطبقة: ${feat.layerKey}", style = MaterialTheme.typography.labelSmall)
                        Text("المصدر: ${feat.sourceCitation}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // أدوات التحكم بالخريطة (التكبير، التصغير، إعادة الضبط، الطبقات، الاستيراد)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = { zoom = (zoom + 0.3f).coerceAtMost(4f) }, modifier = Modifier.weight(1f)) {
                        Text("تكبير +")
                    }
                    OutlinedButton(onClick = { zoom = (zoom - 0.3f).coerceAtLeast(0.6f) }, modifier = Modifier.weight(1f)) {
                        Text("تصغير -")
                    }
                    OutlinedButton(onClick = { zoom = 1f; panOffset = Offset.Zero }, modifier = Modifier.weight(1f)) {
                        Text("إعادة ضبط")
                    }
                }
            }

            if (selectedMap.isBattleMap) {
                item {
                    Button(
                        onClick = { onOpenBattleMap(selectedMap.mapId) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                    ) {
                        Text("فتح خريطة «${selectedMap.arabicTitle}» مع الشريط الزمني للمراحل")
                    }
                }
            }

            // المواقع المرتبطة بهذه الخريطة
            item {
                Text("المواقع الجغرافية على هذه الخريطة (${places.size})", style = MaterialTheme.typography.titleMedium)
            }
            if (places.isEmpty()) {
                item {
                    EmptyVerifiedSourceState(
                        title = "لا توجد مواقع جغرافية مستوردة لهذه الخريطة بعد",
                        message = "استورد حزمة GeoJSON أو MBTiles أو JSON الموثقة لإظهار المواقع والآبار والجبال والأودية ودرجة يقينها الجغرافي.",
                        actionLabel = "استيراد حزمة خريطة",
                        onAction = onOpenImport
                    )
                }
            } else {
                items(places) { place ->
                    SeerahEncyclopediaCard(onClick = { onOpenPlaceDetails(place.id) }) {
                        CitationBadge(label = GeographicCertainty.fromCode(place.certaintyLevel).arabicLabel)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(place.name, style = MaterialTheme.typography.titleMedium)
                        if (place.historicalName.isNotBlank() && place.historicalName != place.name) {
                            Text("الاسم التاريخي: ${place.historicalName}", style = MaterialTheme.typography.labelSmall)
                        }
                        if (place.description.isNotBlank()) {
                            Text(place.description, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BattleMapScreen(
    battleMapId: String,
    repository: SeerahRepository,
    onBack: () -> Unit,
    onOpenLayers: () -> Unit,
    onOpenLegend: () -> Unit,
    onOpenImport: () -> Unit
) {
    val mapDef = AtlasMapDefinition.fromId(battleMapId)
    val phases by repository.observeBattlePhases(battleMapId).collectAsState(initial = emptyList())
    val allFeatures by repository.observeBattleFeatures(battleMapId).collectAsState(initial = emptyList())
    var selectedPhaseIndex by remember { mutableIntStateOf(0) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    val activePhase = phases.getOrNull(selectedPhaseIndex)
    val visibleFeatures = remember(allFeatures, activePhase) {
        if (activePhase == null) allFeatures
        else allFeatures.filter { it.phaseId == null || it.phaseId == activePhase.id }
    }

    Scaffold(
        topBar = {
            SeerahTopBar(
                title = "الأطلس العسكري — ${mapDef.arabicTitle}",
                onBack = onBack,
                actions = {
                    IconButton(onClick = onOpenLayers) { Icon(Icons.Outlined.Layers, contentDescription = "الطبقات") }
                    IconButton(onClick = onOpenLegend) { Icon(Icons.Outlined.Info, contentDescription = "مفتاح الرموز") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HistoricalMapCanvas(
                mapDefinition = mapDef,
                enabledLayers = MapLayerType.entries.map { it.key }.toSet(),
                features = visibleFeatures,
                zoom = zoom,
                panOffset = panOffset,
                onTransform = { z, p ->
                    zoom = (zoom * z).coerceIn(0.6f, 4f)
                    panOffset += p
                },
                modifier = Modifier.weight(1f)
            )

            // الشريط الزمني للمراحل أسفل خريطة الغزوة
            SeerahEncyclopediaCard(parchmentTone = true) {
                Text("الشريط الزمني لمراحل ${mapDef.arabicTitle}", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(6.dp))
                if (phases.isEmpty()) {
                    Text(
                        "لم تُستورد مراحل هذه الغزوة بعد. استورد ملف GeoJSON/JSON الموثق لعرض مراحل التحرك والمواقع حسب المصادر.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onOpenImport, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)) {
                        Text("استيراد بيانات الخريطة")
                    }
                } else {
                    Slider(
                        value = selectedPhaseIndex.toFloat(),
                        onValueChange = { selectedPhaseIndex = it.toInt() },
                        valueRange = 0f..phases.lastIndex.toFloat(),
                        steps = (phases.size - 2).coerceAtLeast(0)
                    )
                    activePhase?.let { phase ->
                        Text("${phase.phaseOrder}. ${phase.title}", style = MaterialTheme.typography.titleMedium)
                        Text(phase.description, style = MaterialTheme.typography.bodyMedium)
                        if (phase.sourceCitation.isNotBlank()) {
                            CitationBadge(label = phase.sourceCitation)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MapLayersScreen(
    repository: SeerahRepository,
    onBack: () -> Unit
) {
    val layers by repository.observeLayers().collectAsState(initial = emptyList())
    val toggleStates = remember { mutableStateMapOf<String, Boolean>() }

    Scaffold(topBar = { SeerahTopBar(title = "طبقات الخريطة (10 طبقات)", onBack = onBack) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(MapLayerType.entries) { layer ->
                val dbLayer = layers.firstOrNull { it.layerKey == layer.key }
                val checked = toggleStates[layer.key] ?: dbLayer?.isEnabledByDefault ?: true
                SeerahEncyclopediaCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(layer.arabicTitle, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = if (dbLayer?.geojsonPath.isNullOrBlank()) "جاهزة لاستقبال بيانات GeoJSON" else "مرتبطة بملف محلي",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = checked,
                            onCheckedChange = { toggleStates[layer.key] = it }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MapLegendScreen(onBack: () -> Unit) {
    val legendItems = listOf(
        "مثلث بني داكن" to "التضاريس والجبال التاريخية (مثل جبل أحد وجبل ثور)",
        "خط أزرق رمادي" to "الأودية والمسارات المائية (مثل وادي بدر ووادي العقيق)",
        "دائرة زرقاء بإطار أخضر" to "الآبار والموارد المائية الثابتة في المصادر",
        "مربع أخضر زمردي" to "مواقع المعسكرات عند ثبوتها في المصادر",
        "سهم أخضر" to "مسار الهجرة النبوية أو تحركات المسلمين الموثقة",
        "سهم أحمر" to "مسارات القوى المقابلة عند توفر دليل تاريخي",
        "نقطة ذهبية" to "موقع تاريخي أو معلم مرتبط بحدث أو شخصية"
    )
    Scaffold(topBar = { SeerahTopBar(title = "مفتاح رموز الأطلس", onBack = onBack) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(legendItems) { (symbol, meaning) ->
                SeerahEncyclopediaCard(parchmentTone = true) {
                    Text(symbol, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(meaning, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
fun HijraJourneyScreen(
    repository: SeerahRepository,
    onBack: () -> Unit,
    onOpenAtlas: () -> Unit
) {
    val routes by repository.observeRoutesForMap("hijra").collectAsState(initial = emptyList())
    val stages = listOf(
        "1. أحداث ما قبل الهجرة والإذن بالهجرة",
        "2. الخروج من مكة المكرمة",
        "3. غار ثور",
        "4. مراحل الطريق الساحلي (مع بيان النقاط المؤكدة والمختلف فيها)",
        "5. الوصول إلى قباء وبناء مسجد قباء",
        "6. الوصول إلى المدينة المنورة"
    )
    var selectedStage by remember { mutableIntStateOf(0) }

    Scaffold(topBar = { SeerahTopBar(title = "موسوعة الهجرة النبوية", onBack = onBack) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SeerahHeroBanner(
                    badge = "تنبيه علمي وجغرافي",
                    title = "مسار الهجرة النبوية الشريفة",
                    subtitle = "لا تُعامل جميع نقاط الطريق كقطعية؛ يُفصل في كل مرحلة بين المواضع الثابتة في الصحيح والمواضع المختلف فيها."
                )
            }
            item {
                Button(
                    onClick = onOpenAtlas,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                ) {
                    Text("استعراض مسار الهجرة على الخريطة التفاعلية")
                }
            }
            items(stages.indices.toList()) { idx ->
                SeerahEncyclopediaCard(
                    parchmentTone = idx == selectedStage,
                    onClick = { selectedStage = idx }
                ) {
                    Text(stages[idx], style = MaterialTheme.typography.titleMedium)
                    if (idx == selectedStage) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (routes.isEmpty()) {
                                "بانتظار استيراد نقاط المسار الموثقة والروايات المرتبطة بهذه المرحلة من مكتبتك العلمية."
                            } else {
                                "عدد المسارات المستوردة المرتبطة بالهجرة: ${routes.size}"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
