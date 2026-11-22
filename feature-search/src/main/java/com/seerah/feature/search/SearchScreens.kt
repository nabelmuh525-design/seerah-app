package com.seerah.feature.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.seerah.core.components.*
import com.seerah.core.theme.SeerahEmerald
import com.seerah.data.repository.SeerahRepository
import com.seerah.domain.model.SearchContentFilter
import com.seerah.domain.model.SearchResultItem
import kotlinx.coroutines.launch

@Composable
fun GlobalSearchScreen(
    repository: SeerahRepository,
    onBack: () -> Unit,
    onSubmitQuery: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    Scaffold(topBar = { SeerahTopBar(title = "البحث الشامل دون إنترنت", onBack = onBack) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("ابحث بالعربية (يدعم توحيد الهمزات والألف المقصورة والياء)") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Button(
                onClick = { if (query.isNotBlank()) onSubmitQuery(query.trim()) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
            ) {
                Text("بحث في الموسوعة والمكتبة")
            }
            SeerahEncyclopediaCard(parchmentTone = true) {
                Text("نطاق البحث المحلي:", style = MaterialTheme.typography.titleMedium)
                Text("الأحداث • الغزوات • السرايا • الشخصيات • الأماكن • الكتب المستوردة • الروايات • الآيات • الأحاديث", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun SearchResultsScreen(
    initialQuery: String,
    repository: SeerahRepository,
    onBack: () -> Unit,
    onOpenResult: (SearchResultItem) -> Unit
) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf(initialQuery) }
    var filter by remember { mutableStateOf(SearchContentFilter.ALL) }
    var results by remember { mutableStateOf<List<SearchResultItem>>(emptyList()) }

    LaunchedEffect(query, filter) {
        results = repository.searchOffline(query, filter)
    }

    Scaffold(topBar = { SeerahTopBar(title = "نتائج البحث (${results.size})", subtitle = query, onBack = onBack) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("تعديل عبارة البحث") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = { scope.launch { results = repository.searchOffline(query, filter) } }) {
                            Icon(Icons.Outlined.Search, contentDescription = "بحث")
                        }
                    }
                )
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(SearchContentFilter.entries) { f ->
                        FilterChip(selected = filter == f, onClick = { filter = f }, label = { Text(f.arabicLabel) })
                    }
                }
            }
            if (results.isEmpty()) {
                item {
                    EmptyVerifiedSourceState(
                        title = "لا توجد نتائج مطابقة",
                        message = "جرب كلمات أخرى أو تأكد من استيراد الكتب والمصادر في المكتبة العلمية."
                    )
                }
            } else {
                items(results) { item ->
                    SeerahEncyclopediaCard(onClick = { onOpenResult(item) }) {
                        CitationBadge(label = "${item.contentType.arabicLabel} • ${item.sourceCitation}")
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(item.title, style = MaterialTheme.typography.titleMedium)
                        if (item.snippet.isNotBlank()) Text(item.snippet, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
