package com.seerah.feature.places

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
import com.seerah.domain.model.GeographicCertainty

@Composable
fun HistoricalPlacesScreen(
    repository: SeerahRepository,
    onBack: () -> Unit,
    onOpenPlace: (Long) -> Unit,
    onOpenAtlas: (String) -> Unit,
    onOpenImport: () -> Unit
) {
    val places by repository.observeAllPlaces().collectAsState(initial = emptyList())

    Scaffold(topBar = { SeerahTopBar(title = "موسوعة الأماكن التاريخية", onBack = onBack) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (places.isEmpty()) {
                item {
                    EmptyVerifiedSourceState(
                        title = "لا توجد أماكن تاريخية مستوردة بعد",
                        message = "استورد حزمة الأماكن والإحداثيات الموثقة (مكة، المدينة، غار حراء، غار ثور، قباء، بدر، أحد، الخندق، الحديبية، خيبر، الطائف، تبوك) مع درجة اليقين الجغرافي ومصادرها.",
                        actionLabel = "استيراد حزمة الأماكن",
                        onAction = onOpenImport
                    )
                }
            } else {
                items(places) { place ->
                    SeerahEncyclopediaCard(onClick = { onOpenPlace(place.id) }) {
                        CitationBadge(label = GeographicCertainty.fromCode(place.certaintyLevel).arabicLabel)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(place.name, style = MaterialTheme.typography.titleLarge)
                        if (place.description.isNotBlank()) {
                            Text(place.description, style = MaterialTheme.typography.bodyMedium)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        TextButton(onClick = { onOpenAtlas(place.mapId) }) { Text("عرض في الأطلس") }
                    }
                }
            }
        }
    }
}

@Composable
fun PlaceDetailsScreen(
    placeId: Long,
    repository: SeerahRepository,
    onBack: () -> Unit,
    onOpenAtlas: (String) -> Unit
) {
    var place by remember { mutableStateOf<com.seerah.database.entity.PlaceEntity?>(null) }
    val coords by repository.observeCoordinatesForPlace(placeId).collectAsState(initial = emptyList())

    LaunchedEffect(placeId) {
        place = repository.getPlaceById(placeId)
    }

    Scaffold(topBar = { SeerahTopBar(title = place?.name ?: "تفاصيل المكان", onBack = onBack) }) { padding ->
        val pl = place
        if (pl == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                EmptyVerifiedSourceState("الموقع غير متوفر", "لم يتم العثور على بيانات الموقع في القاعدة المحلية.")
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
                    CitationBadge(label = GeographicCertainty.fromCode(pl.certaintyLevel).arabicLabel)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(pl.name, style = MaterialTheme.typography.headlineMedium)
                    if (pl.historicalName.isNotBlank()) Text("الاسم التاريخي: ${pl.historicalName}", style = MaterialTheme.typography.labelLarge)
                    if (pl.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(pl.description, style = MaterialTheme.typography.bodyLarge)
                    }
                    if (pl.historicalSignificance.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("الأهمية التاريخية: ${pl.historicalSignificance}", style = MaterialTheme.typography.bodyMedium)
                    }
                    coords.forEach { c ->
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("الإحداثيات الموثقة: ${c.latitude}, ${c.longitude}", style = MaterialTheme.typography.labelLarge)
                        if (c.sourceCitation.isNotBlank()) CitationBadge(label = c.sourceCitation)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { onOpenAtlas(pl.mapId) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                    ) { Text("فتح الموقع على الخريطة") }
                }
            }
        }
    }
}
