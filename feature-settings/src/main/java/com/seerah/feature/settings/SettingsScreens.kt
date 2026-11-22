package com.seerah.feature.settings

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.seerah.core.components.*
import com.seerah.core.theme.SeerahEmerald
import com.seerah.core.theme.SeerahRouteRed
import com.seerah.data.repository.SeerahRepository
import com.seerah.data.updater.AppReleaseInfo
import com.seerah.data.updater.UpdateDownloadProgress
import com.seerah.database.dao.QualityAuditSnapshot
import com.seerah.domain.model.DatabaseHealthReport
import com.seerah.domain.model.UserPreferencesState
import kotlinx.coroutines.launch

@Composable
fun MoreHubScreen(
    onOpenPeople: () -> Unit,
    onOpenPlaces: () -> Unit,
    onOpenHijra: () -> Unit,
    onOpenQuran: () -> Unit,
    onOpenHadith: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenNotes: () -> Unit,
    onOpenQuiz: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit
) {
    val entries = listOf(
        Triple("موسوعة الشخصيات", Icons.Outlined.Groups, onOpenPeople),
        Triple("موسوعة الأماكن التاريخية", Icons.Outlined.Place, onOpenPlaces),
        Triple("موسوعة الهجرة النبوية", Icons.Outlined.Route, onOpenHijra),
        Triple("القرآن الكريم والتفسير", Icons.Outlined.MenuBook, onOpenQuran),
        Triple("مكتبة الأحاديث النبوية", Icons.Outlined.LibraryBooks, onOpenHadith),
        Triple("المكتبة العلمية واستيراد الكتب", Icons.Outlined.UploadFile, onOpenLibrary),
        Triple("المفضلة", Icons.Outlined.BookmarkBorder, onOpenFavorites),
        Triple("الملاحظات العلمية", Icons.Outlined.EditNote, onOpenNotes),
        Triple("اختبار ومراجعة السيرة", Icons.Outlined.Quiz, onOpenQuiz),
        Triple("الحزم والتنزيلات المحلية", Icons.Outlined.DownloadForOffline, onOpenDownloads),
        Triple("الإعدادات والنسخ الاحتياطي", Icons.Outlined.Settings, onOpenSettings),
        Triple("عن تطبيق سِيرَة والمنهج العلمي", Icons.Outlined.Info, onOpenAbout)
    )

    Scaffold(topBar = { SeerahTopBar(title = "المزيد — أقسام سِيرَة") }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(entries) { (label, icon, action) ->
                SeerahEncyclopediaCard(onClick = action) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(label, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun FavoritesScreen(repository: SeerahRepository, onBack: () -> Unit) {
    val favorites by repository.observeFavorites().collectAsState(initial = emptyList())
    Scaffold(topBar = { SeerahTopBar(title = "المفضلة", onBack = onBack) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (favorites.isEmpty()) {
                item { EmptyVerifiedSourceState("المفضلة فارغة", "احفظ الأحداث أو الشخصيات أو الأماكن للوصول السريع إليها دون إنترنت.") }
            } else {
                items(favorites) { fav ->
                    SeerahEncyclopediaCard {
                        CitationBadge(label = fav.itemType)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(fav.title, style = MaterialTheme.typography.titleMedium)
                        if (fav.subtitle.isNotBlank()) Text(fav.subtitle, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun NotesScreen(repository: SeerahRepository, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val notes by repository.observeNotes().collectAsState(initial = emptyList())
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }

    Scaffold(topBar = { SeerahTopBar(title = "الملاحظات العلمية", onBack = onBack) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SeerahEncyclopediaCard(parchmentTone = true) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("عنوان الملاحظة") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = content, onValueChange = { content = it }, label = { Text("نص الملاحظة أو الفائدة العلمية") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (title.isNotBlank() && content.isNotBlank()) {
                                scope.launch {
                                    repository.saveNote("GENERAL", System.currentTimeMillis(), title.trim(), content.trim())
                                    title = ""
                                    content = ""
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                    ) { Text("حفظ الملاحظة") }
                }
            }
            items(notes) { note ->
                SeerahEncyclopediaCard {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(note.title, style = MaterialTheme.typography.titleMedium)
                        IconButton(onClick = { scope.launch { repository.deleteNote(note) } }) {
                            Icon(Icons.Outlined.DeleteOutline, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    Text(note.content, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
fun QuizScreen(repository: SeerahRepository, onBack: () -> Unit, onOpenImport: () -> Unit) {
    data class VerifiedQuestion(
        val question: String,
        val options: List<String>,
        val correctIndex: Int,
        val source: String
    )
    val questions = remember {
        listOf(
            VerifiedQuestion(
                "ما السبب المباشر لخروج المسلمين في غزوة بدر الكبرى؟",
                listOf("اعتراض قافلة قريش استردادًا لحقوق المهاجرين", "الهجوم على مكة", "عقد صلح مع القبائل", "فتح خيبر"),
                0,
                "سيرة ابن هشام؛ صحيح البخاري (كتاب المغازي)"
            ),
            VerifiedQuestion(
                "في أي غار نزل الوحي لأول مرة على النبي ﷺ؟",
                listOf("غار ثور", "غار حراء", "جبل أحد", "وادي بدر"),
                1,
                "صحيح البخاري (حديث 3)"
            ),
            VerifiedQuestion(
                "من الذي رافق النبي ﷺ في الهجرة النبوية وفي غار ثور؟",
                listOf("عمر بن الخطاب رضي الله عنه", "أبو بكر الصديق رضي الله عنه", "عثمان بن عفان رضي الله عنه", "خالد بن الوليد رضي الله عنه"),
                1,
                "القرآن الكريم (التوبة 40)؛ صحيح البخاري"
            ),
            VerifiedQuestion(
                "من أشار على النبي ﷺ بحفر الخندق في غزوة الأحزاب؟",
                listOf("سعد بن معاذ رضي الله عنه", "سلمان الفارسي رضي الله عنه", "حمزة بن عبد المطلب رضي الله عنه", "بلال بن رباح رضي الله عنه"),
                1,
                "سيرة ابن هشام؛ البداية والنهاية"
            ),
            VerifiedQuestion(
                "في أي سنة هجرية وقع فتح مكة الأعظم؟",
                listOf("السنة الثانية للهجرة", "السنة السادسة للهجرة", "السنة الثامنة للهجرة", "السنة العاشرة للهجرة"),
                2,
                "صحيح البخاري (حديث 4276)"
            )
        )
    }

    var currentIndex by remember { mutableIntStateOf(0) }
    var selectedOption by remember { mutableStateOf<Int?>(null) }
    var score by remember { mutableIntStateOf(0) }
    val q = questions[currentIndex]

    Scaffold(
        topBar = {
            SeerahTopBar(
                title = "اختبر معلوماتك",
                subtitle = "أسئلة موثقة من مصادر السيرة",
                onBack = onBack,
                actions = {
                    Surface(
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
                        color = com.seerah.core.theme.SeerahAntiqueGold.copy(alpha = 0.25f),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Text(
                            text = "★ $score",
                            style = MaterialTheme.typography.titleMedium,
                            color = com.seerah.core.theme.SeerahGoldLight,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SeerahEncyclopediaCard(parchmentTone = true) {
                Text("سؤال ${currentIndex + 1} من ${questions.size}", style = MaterialTheme.typography.labelSmall)
                Spacer(modifier = Modifier.height(6.dp))
                Text(q.question, style = MaterialTheme.typography.titleLarge)
            }

            q.options.forEachIndexed { idx, opt ->
                val isPicked = selectedOption == idx
                val isCorrect = idx == q.correctIndex
                val cardBg = when {
                    selectedOption == null -> MaterialTheme.colorScheme.surface
                    isCorrect -> SeerahEmerald.copy(alpha = 0.16f)
                    isPicked && !isCorrect -> SeerahRouteRed.copy(alpha = 0.16f)
                    else -> MaterialTheme.colorScheme.surface
                }
                Card(
                    onClick = {
                        if (selectedOption == null) {
                            selectedOption = idx
                            if (idx == q.correctIndex) score += 10
                        }
                    },
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (selectedOption != null && isCorrect) SeerahEmerald else com.seerah.core.theme.SeerahAntiqueGold
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(opt, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        if (selectedOption != null && isCorrect) {
                            Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            if (selectedOption != null) {
                SeerahEncyclopediaCard(parchmentTone = true) {
                    Text(
                        text = if (selectedOption == q.correctIndex) "✓ الإجابة صحيحة" else "الإجابة الصحيحة: ${q.options[q.correctIndex]}",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (selectedOption == q.correctIndex) SeerahEmerald else SeerahRouteRed
                    )
                    Text("المصدر: ${q.source}", style = MaterialTheme.typography.labelSmall)
                }
                Button(
                    onClick = {
                        selectedOption = null
                        currentIndex = (currentIndex + 1) % questions.size
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                ) {
                    Text(if (currentIndex < questions.lastIndex) "السؤال التالي" else "إعادة الاختبار")
                }
            }
        }
    }
}

@Composable
fun DownloadsScreen(repository: SeerahRepository, onBack: () -> Unit, onOpenImport: () -> Unit) {
    val downloads by repository.observeDownloads().collectAsState(initial = emptyList())
    Scaffold(topBar = { SeerahTopBar(title = "إدارة الحزم والتنزيلات المحلية", onBack = onBack) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                SeerahEncyclopediaCard(parchmentTone = true) {
                    Text("التطبيق يعمل دون إنترنت (Offline-First) ولا يقوم بتنزيل أي ملفات تلقائيًا دون موافقتك.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onOpenImport, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)) {
                        Text("استيراد حزمة كتب أو خرائط من الهاتف")
                    }
                }
            }
            items(downloads) { pkg ->
                SeerahEncyclopediaCard {
                    Text(pkg.title, style = MaterialTheme.typography.titleMedium)
                    Text("النوع: ${pkg.packageType} • الحالة: ${pkg.status}", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    repository: SeerahRepository,
    prefsState: UserPreferencesState,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var statusMessage by remember { mutableStateOf("") }
    var healthReport by remember { mutableStateOf<DatabaseHealthReport?>(null) }
    var auditSnapshot by remember { mutableStateOf<QualityAuditSnapshot?>(null) }
    LaunchedEffect(Unit) { auditSnapshot = repository.getQualityAuditSnapshot() }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var releaseInfo by remember { mutableStateOf<AppReleaseInfo?>(null) }
    var updateNotice by remember { mutableStateOf("") }
    var downloadProgress by remember { mutableStateOf<UpdateDownloadProgress>(UpdateDownloadProgress.Idle) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val res = repository.backupRestoreManager.exportUserDataToUri(uri)
                statusMessage = res.getOrElse { it.message ?: "فشل التصدير" }
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val res = repository.backupRestoreManager.importUserDataFromUri(uri)
                statusMessage = res.getOrElse { it.message ?: "فشلت الاستعادة" }
            }
        }
    }

    Scaffold(topBar = { SeerahTopBar(title = "الإعدادات", onBack = onBack) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SeerahEncyclopediaCard {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("الوضع الليلي", style = MaterialTheme.typography.titleMedium)
                        Switch(
                            checked = prefsState.darkMode,
                            onCheckedChange = { enabled -> scope.launch { repository.preferences.setDarkMode(enabled) } }
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("حجم الخط: ${"%.0f".format(prefsState.fontScale * 100)}%", style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = prefsState.fontScale,
                        onValueChange = { scale -> scope.launch { repository.preferences.setFontScale(scale) } },
                        valueRange = 0.85f..1.35f
                    )
                }
            }

            item {
                SeerahEncyclopediaCard(parchmentTone = true) {
                    Text("التحديث المباشر من داخل التطبيق", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "الإصدار المثبّت حاليًا: v${repository.appUpdater.currentVersionName()}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                isCheckingUpdate = true
                                updateNotice = ""
                                val res = repository.appUpdater.checkLatestRelease()
                                isCheckingUpdate = false
                                res.onSuccess { info ->
                                    releaseInfo = info
                                    updateNotice = if (info.isUpdateAvailable) {
                                        "يتوفر إصدار جديد: ${info.latestVersion}"
                                    } else {
                                        "أنت تستخدم أحدث إصدار من تطبيق سِيرَة (${info.latestVersion})."
                                    }
                                }.onFailure { err ->
                                    updateNotice = err.message ?: "تعذر التحقق من التحديثات."
                                }
                            }
                        },
                        enabled = !isCheckingUpdate,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                    ) {
                        Text(if (isCheckingUpdate) "جارٍ التحقق من التحديثات..." else "التحقق من وجود تحديث")
                    }

                    if (updateNotice.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(updateNotice, style = MaterialTheme.typography.bodyMedium)
                    }

                    releaseInfo?.takeIf { it.isUpdateAvailable }?.let { info ->
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(info.releaseNotes, style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        when (val prog = downloadProgress) {
                            is UpdateDownloadProgress.Downloading -> {
                                LinearProgressIndicator(
                                    progress = { prog.percent / 100f },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text("جارٍ التحميل... ${prog.percent}%", style = MaterialTheme.typography.labelSmall)
                            }
                            is UpdateDownloadProgress.ReadyToInstall -> {
                                Button(
                                    onClick = {
                                        repository.appUpdater.installApk(prog.apkFile).onFailure {
                                            updateNotice = it.message ?: "تعذر بدء التثبيت"
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                                ) { Text("تثبيت التحديث الآن") }
                            }
                            is UpdateDownloadProgress.Error -> {
                                Text(prog.message, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                            }
                            UpdateDownloadProgress.Idle -> {
                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            repository.appUpdater.downloadUpdateApk(info.apkDownloadUrl, info.sha256DownloadUrl).collect { state ->
                                                downloadProgress = state
                                                if (state is UpdateDownloadProgress.ReadyToInstall) {
                                                    repository.appUpdater.installApk(state.apkFile).onFailure {
                                                        updateNotice = it.message ?: "يرجى السماح بالتثبيت ثم المحاولة مجددًا"
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("تحميل وتثبيت الإصدار ${info.latestVersion}") }
                            }
                        }
                    }
                }
            }


            item {
                SeerahEncyclopediaCard(parchmentTone = true) {
                    Text("لوحة مراجعة الجودة العلمية والتدقيق (v3.0)", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    val snap = auditSnapshot
                    if (snap != null) {
                        Text("• إجمالي الأحداث: ${snap.totalEvents}", style = MaterialTheme.typography.bodyMedium)
                        Text("• أحداث تحتاج ربطًا هيكليًا بجدول المصادر: ${snap.eventsWithoutFormalSourceLinks}", style = MaterialTheme.typography.bodyMedium)
                        Text("• روايات تحتاج استكمال مرجع الحكم: ${snap.narrationsWithoutGradeRef}", style = MaterialTheme.typography.bodyMedium)
                        Text("• أحاديث بلا حكم موثق: ${snap.hadithsWithoutGrades}", style = MaterialTheme.typography.bodyMedium)
                        Text("• مواقع بلا إحداثيات: ${snap.placesWithoutCoordinates} | مختلف فيها: ${snap.disputedPlaces}", style = MaterialTheme.typography.bodyMedium)
                        Text("• كتب مستوردة بـ OCR وتحتاج مراجعة: ${snap.booksNeedingOcrReview}", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        Text("جارٍ فحص حالة التوثيق العلمي...", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            item {
                SeerahEncyclopediaCard(parchmentTone = true) {
                    Text("النسخ الاحتياطي والاستعادة وسلامة البيانات", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(onClick = { exportLauncher.launch("seerah_backup.json") }, modifier = Modifier.weight(1f)) { Text("تصدير نسخة") }
                        OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json")) }, modifier = Modifier.weight(1f)) { Text("استعادة نسخة") }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { scope.launch { healthReport = repository.checkDatabaseHealth() } },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("فحص سلامة قاعدة البيانات (SQLite Integrity Check)") }

                    if (statusMessage.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(statusMessage, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    }
                    healthReport?.let { hr ->
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "الحالة: ${if (hr.isHealthy) "سليمة (OK)" else "تحتاج فحص"} • الكتب المسجلة: ${hr.booksCount} • الأحداث: ${hr.eventsCount}",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AboutScreen(onBack: () -> Unit) {
    Scaffold(topBar = { SeerahTopBar(title = "عن سِيرَة | SEERAH", onBack = onBack) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SeerahHeroBanner(
                    badge = "الإصدار 3.0.1-alpha.5",
                    title = "سِيرَة | SEERAH",
                    subtitle = "موسوعة السيرة النبوية التفاعلية — «رحلة موثقة في حياة رسول الله محمد ﷺ»"
                )
            }
            item {
                SeerahEncyclopediaCard(parchmentTone = true) {
                    Text("العبارة الأساسية", style = MaterialTheme.typography.titleMedium)
                    Text("نقرأ الأحداث، ونشاهد الأماكن، ونتتبع التاريخ، ونرجع إلى المصادر.", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("المنهج العلمي والضوابط:", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "• كل معلومة تاريخية تُربط بمرجعها الأصلي أو تُوسم بأنها غير متوفرة/تحتاج مراجعة.\n" +
                            "• فصل تام بين نص الرواية والتحليل والشرح.\n" +
                            "• لا تُصدر أحكام حديثية آليًا دون مرجع علمي.\n" +
                            "• التزام كامل بعدم تجسيد النبي ﷺ أو ادعاء صور للصحابة.\n" +
                            "• عمل محلي كامل دون إنترنت وحماية تامة للكتب المستوردة.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
