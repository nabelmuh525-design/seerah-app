package com.seerah.feature.library

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.seerah.core.components.*
import com.seerah.core.theme.*
import com.seerah.database.entity.BookEntity
import com.seerah.data.repository.SeerahRepository
import com.seerah.domain.model.HadithGradeStatus
import com.seerah.domain.model.ImportReport
import kotlinx.coroutines.launch

@Composable
fun SourcesLibraryScreen(
    repository: SeerahRepository,
    onBack: () -> Unit,
    onOpenBook: (Long) -> Unit,
    onOpenImport: () -> Unit,
    onOpenManagement: () -> Unit
) {
    val books by repository.observeBooks().collectAsState(initial = emptyList())
    var filterPrimaryOnly by remember { mutableStateOf<Boolean?>(null) }
    val filtered = remember(books, filterPrimaryOnly) {
        when (filterPrimaryOnly) {
            true -> books.filter { it.isPrimarySource }
            false -> books.filter { !it.isPrimarySource }
            null -> books
        }
    }

    Scaffold(
        topBar = {
            SeerahTopBar(
                title = "المكتبة العلمية",
                subtitle = "الفصل بين المصادر الأصلية والدراسات الحديثة",
                onBack = onBack,
                actions = {
                    IconButton(onClick = onOpenImport) { Icon(Icons.Outlined.UploadFile, contentDescription = "استيراد") }
                    IconButton(onClick = onOpenManagement) { Icon(Icons.Outlined.Settings, contentDescription = "إدارة المكتبة") }
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = filterPrimaryOnly == null, onClick = { filterPrimaryOnly = null }, label = { Text("الكل (${books.size})") })
                    FilterChip(selected = filterPrimaryOnly == true, onClick = { filterPrimaryOnly = true }, label = { Text("المصادر الأصلية") })
                    FilterChip(selected = filterPrimaryOnly == false, onClick = { filterPrimaryOnly = false }, label = { Text("الدراسات الحديثة") })
                }
            }

            items(filtered) { book ->
                SeerahEncyclopediaCard(
                    parchmentTone = book.isImported,
                    onClick = { onOpenBook(book.id) }
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        CitationBadge(
                            label = if (book.isPrimarySource) "مصدر أصلي" else "دراسة حديثة",
                            toneColor = if (book.isPrimarySource) SeerahEmerald else SeerahAntiqueGold
                        )
                        Text(
                            text = if (book.isImported) "مستورد (${book.pageCount} ص)" else "بانتظار الملف",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (book.isImported) SeerahEmerald else SeerahHistoricalBrown
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(book.title, style = MaterialTheme.typography.titleMedium)
                    Text("المؤلف: ${book.authorName}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (book.isOcrExtracted) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("تنبيه: نص مستخرج آليًا (OCR) ويحتاج مراجعة.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
fun BookDetailsScreen(
    bookId: Long,
    repository: SeerahRepository,
    onBack: () -> Unit,
    onOpenImport: () -> Unit
) {
    var book by remember { mutableStateOf<BookEntity?>(null) }
    val pages by repository.observeBookPages(bookId).collectAsState(initial = emptyList())
    val editions by repository.observeEditions(bookId).collectAsState(initial = emptyList())

    LaunchedEffect(bookId) {
        book = repository.getBookById(bookId)
        book?.let { repository.preferences.saveLastRead(it.title, "book_details/${it.id}") }
    }

    Scaffold(topBar = { SeerahTopBar(title = book?.title ?: "تفاصيل الكتاب", onBack = onBack) }) { padding ->
        val b = book
        if (b == null) return@Scaffold
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SeerahEncyclopediaCard(parchmentTone = true) {
                    Text(b.title, style = MaterialTheme.typography.headlineMedium)
                    Text("المؤلف: ${b.authorName}", style = MaterialTheme.typography.bodyMedium)
                    Text("التصنيف: ${if (b.isPrimarySource) "مصدر أصلي" else "دراسة حديثة"}", style = MaterialTheme.typography.labelLarge)
                    editions.forEach { ed ->
                        Text("الطبعة: ${ed.editionName} — المحقق: ${ed.investigator.ifBlank { "غير محدد" }}", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            if (pages.isEmpty()) {
                item {
                    EmptyVerifiedSourceState(
                        title = "لم يتم استيراد نص هذا الكتاب بعد",
                        message = "يمكنك استيراد نسخة الكتاب من هاتفك بصيغة EPUB أو PDF أو TXT أو JSON أو XML أو SQLite.",
                        actionLabel = "استيراد ملف الكتاب",
                        onAction = onOpenImport
                    )
                }
            } else {
                items(pages) { page ->
                    SeerahEncyclopediaCard {
                        Text("ج${page.volumeNumber} — ص${page.pageNumber} ${if (page.chapterTitle.isNotBlank()) "• ${page.chapterTitle}" else ""}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(page.rawText, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}

@Composable
fun ImportBooksScreen(
    repository: SeerahRepository,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var authorName by remember { mutableStateOf("") }
    var editionName by remember { mutableStateOf("") }
    var investigator by remember { mutableStateOf("") }
    var isPrimarySource by remember { mutableStateOf(true) }
    var isImporting by remember { mutableStateOf(false) }
    var report by remember { mutableStateOf<ImportReport?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                isImporting = true
                report = repository.importFile(
                    uri = uri,
                    isPrimarySource = isPrimarySource,
                    authorName = authorName.ifBlank { "مؤلف غير محدد — يحتاج مراجعة" },
                    editionName = editionName,
                    investigator = investigator
                )
                isImporting = false
            }
        }
    }

    Scaffold(topBar = { SeerahTopBar(title = "استيراد الكتب والمصادر", onBack = onBack) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SeerahEncyclopediaCard(parchmentTone = true) {
                    Text("الصيغ المدعومة محليًا دون إنترنت:", style = MaterialTheme.typography.titleMedium)
                    Text("PDF • TXT • EPUB • XML • JSON / GeoJSON • CSV • SQLite / MBTiles • ZIP", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("تُحفظ الملفات محليًا فقط ولا تُرسل إلى أي خادم خارجي، مع كشف تلقائي للتكرار (SHA-256).", style = MaterialTheme.typography.bodyMedium)
                }
            }

            item {
                OutlinedTextField(
                    value = authorName,
                    onValueChange = { authorName = it },
                    label = { Text("اسم المؤلف (اختياري)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = editionName,
                    onValueChange = { editionName = it },
                    label = { Text("بيانات الطبعة (اختياري)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = investigator,
                    onValueChange = { investigator = it },
                    label = { Text("اسم المحقق (اختياري)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("تصنيف المصدر كمرجع أصلي", style = MaterialTheme.typography.bodyLarge)
                    Switch(checked = isPrimarySource, onCheckedChange = { isPrimarySource = it })
                }
            }
            item {
                Button(
                    onClick = { picker.launch(arrayOf("*/*")) },
                    enabled = !isImporting,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                ) {
                    Text(if (isImporting) "جارٍ الاستيراد والفهرسة..." else "اختيار ملف من الهاتف")
                }
            }

            report?.let { rep ->
                item {
                    SeerahEncyclopediaCard(parchmentTone = true) {
                        Text(if (rep.success) "نتيجة الاستيراد" else "تعذر الاستيراد", style = MaterialTheme.typography.titleMedium)
                        Text(rep.message, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun LibraryManagementScreen(
    repository: SeerahRepository,
    onBack: () -> Unit,
    onOpenImport: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val books by repository.observeBooks().collectAsState(initial = emptyList())
    val importedBooks = books.filter { it.isImported }
    var bookPendingDelete by remember { mutableStateOf<BookEntity?>(null) }

    bookPendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { bookPendingDelete = null },
            title = { Text("تأكيد حذف المصدر") },
            text = { Text("هل تريد حذف «${target.title}» وجميع صفحاته المفهرسة من المكتبة المحلية؟") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        repository.deleteImportedBook(target)
                        bookPendingDelete = null
                    }
                }) { Text("حذف", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { bookPendingDelete = null }) { Text("إلغاء") }
            }
        )
    }

    Scaffold(topBar = { SeerahTopBar(title = "إدارة المكتبة العلمية", onBack = onBack) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (importedBooks.isEmpty()) {
                item {
                    EmptyVerifiedSourceState(
                        title = "لا توجد ملفات مستوردة حاليًا",
                        message = "ابدأ باستيراد الكتب أو الحزم لإدارتها أو إعادة فهرستها من هنا.",
                        actionLabel = "استيراد الآن",
                        onAction = onOpenImport
                    )
                }
            } else {
                items(importedBooks) { book ->
                    SeerahEncyclopediaCard {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(book.title, style = MaterialTheme.typography.titleMedium)
                                Text("${book.fileFormat} • ${book.pageCount} صفحة", style = MaterialTheme.typography.labelSmall)
                            }
                            IconButton(onClick = { bookPendingDelete = book }) {
                                Icon(Icons.Outlined.DeleteOutline, contentDescription = "حذف بعد التأكيد", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NarrationComparisonScreen(
    eventId: Long,
    repository: SeerahRepository,
    onBack: () -> Unit
) {
    val narrations by repository.observeNarrationsForEvent(eventId).collectAsState(initial = emptyList())
    Scaffold(topBar = { SeerahTopBar(title = "مقارنة الروايات والمصادر", onBack = onBack) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (narrations.isEmpty()) {
                item {
                    EmptyVerifiedSourceState(
                        title = "لا توجد روايات متعددة للمقارنة بعد",
                        message = "عند استيراد روايات الحدث من أكثر من مصدر، تُعرض هنا جنبًا إلى جنب مع الإسناد ودرجة الحكم ومرجعه العلمي."
                    )
                }
            } else {
                items(narrations) { nar ->
                    SeerahEncyclopediaCard(parchmentTone = true) {
                        CitationBadge(label = "${nar.sourceName} — ${HadithGradeStatus.fromCode(nar.gradeStatus).arabicLabel}")
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(nar.narrationText, style = MaterialTheme.typography.bodyLarge)
                        if (nar.isnad.isNotBlank()) Text("الإسناد: ${nar.isnad}", style = MaterialTheme.typography.labelSmall)
                        if (nar.scholarlyCommentary.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("الشرح/التعليق (مفصول عن المتن): ${nar.scholarlyCommentary}", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}
