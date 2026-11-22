package com.seerah.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.seerah.core.navigation.SeerahRoutes
import com.seerah.core.theme.SeerahEmerald
import com.seerah.core.theme.SeerahTheme
import com.seerah.domain.model.SearchContentFilter
import com.seerah.domain.model.UserPreferencesState
import com.seerah.data.updater.AppReleaseInfo
import com.seerah.data.updater.UpdateDownloadProgress
import com.seerah.feature.atlas.*
import com.seerah.feature.battles.*
import com.seerah.feature.hadith.*
import com.seerah.feature.home.*
import com.seerah.feature.library.*
import com.seerah.feature.people.*
import com.seerah.feature.places.*
import com.seerah.feature.quran.*
import com.seerah.feature.search.*
import com.seerah.feature.settings.*
import com.seerah.feature.timeline.*
import kotlinx.coroutines.launch
import java.net.URLDecoder

private data class BottomTab(val route: String, val label: String, val icon: ImageVector)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = (application as SeerahApplication).repository

        setContent {
            val prefs by repository.preferences.state.collectAsState(initial = UserPreferencesState())
            val scope = rememberCoroutineScope()
            var pendingUpdateRelease by remember { mutableStateOf<AppReleaseInfo?>(null) }
            var autoUpdateProgress by remember { mutableStateOf<UpdateDownloadProgress>(UpdateDownloadProgress.Idle) }
            var autoUpdateError by remember { mutableStateOf("") }

            LaunchedEffect(Unit) {
                repository.appUpdater.checkLatestRelease().onSuccess { info ->
                    if (info.isUpdateAvailable) {
                        pendingUpdateRelease = info
                    }
                }
            }

            pendingUpdateRelease?.let { release ->
                AlertDialog(
                    onDismissRequest = {
                        if (autoUpdateProgress !is UpdateDownloadProgress.Downloading) {
                            pendingUpdateRelease = null
                        }
                    },
                    title = { Text("يتوفر تحديث جديد لتطبيق سِيرَة (${release.latestVersion})") },
                    text = {
                        androidx.compose.foundation.layout.Column(
                            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                        ) {
                            Text(release.releaseNotes.ifBlank { "يتضمن هذا التحديث تحسينات ومحتوى جديدًا." })
                            when (val prog = autoUpdateProgress) {
                                is UpdateDownloadProgress.Downloading -> {
                                    LinearProgressIndicator(progress = { prog.percent / 100f })
                                    Text("جارٍ التحميل... ${prog.percent}%")
                                }
                                is UpdateDownloadProgress.Error -> {
                                    Text(prog.message, color = MaterialTheme.colorScheme.error)
                                }
                                else -> {}
                            }
                            if (autoUpdateError.isNotBlank()) {
                                Text(autoUpdateError, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            enabled = autoUpdateProgress !is UpdateDownloadProgress.Downloading,
                            onClick = {
                                val ready = autoUpdateProgress as? UpdateDownloadProgress.ReadyToInstall
                                if (ready != null) {
                                    repository.appUpdater.installApk(ready.apkFile).onFailure {
                                        autoUpdateError = it.message ?: "تعذر بدء التثبيت"
                                    }
                                } else {
                                    scope.launch {
                                        repository.appUpdater.downloadUpdateApk(release.apkDownloadUrl, release.sha256DownloadUrl).collect { st ->
                                            autoUpdateProgress = st
                                            if (st is UpdateDownloadProgress.ReadyToInstall) {
                                                repository.appUpdater.installApk(st.apkFile).onFailure {
                                                    autoUpdateError = it.message ?: "يرجى السماح بالتثبيت ثم الضغط مجددًا"
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        ) {
                            Text(
                                if (autoUpdateProgress is UpdateDownloadProgress.ReadyToInstall) "تثبيت الآن"
                                else "تحميل وتثبيت التحديث"
                            )
                        }
                    },
                    dismissButton = {
                        if (autoUpdateProgress !is UpdateDownloadProgress.Downloading) {
                            TextButton(onClick = { pendingUpdateRelease = null }) {
                                Text("لاحقًا")
                            }
                        }
                    }
                )
            }

            SeerahTheme(darkTheme = prefs.darkMode, fontScale = prefs.fontScale) {
                val navController = rememberNavController()
                val backStack by navController.currentBackStackEntryAsState()
                val currentRoute = backStack?.destination?.route ?: SeerahRoutes.SPLASH

                // شريط التنقل السفلي: الرئيسية، السيرة، الأطلس، الغزوات، المزيد
                val bottomTabs = remember {
                    listOf(
                        BottomTab(SeerahRoutes.HOME, "الرئيسية", Icons.Outlined.Home),
                        BottomTab(SeerahRoutes.TIMELINE, "السيرة", Icons.Outlined.Timeline),
                        BottomTab(SeerahRoutes.historicalAtlas(), "الأطلس", Icons.Outlined.Map),
                        BottomTab(SeerahRoutes.BATTLE_ENCYCLOPEDIA, "الغزوات", Icons.Outlined.Shield),
                        BottomTab(SeerahRoutes.MORE, "المزيد", Icons.Outlined.Menu)
                    )
                }

                val showBottomBar = currentRoute in setOf(
                    SeerahRoutes.HOME,
                    SeerahRoutes.TIMELINE,
                    SeerahRoutes.HISTORICAL_ATLAS,
                    SeerahRoutes.BATTLE_ENCYCLOPEDIA,
                    SeerahRoutes.MORE
                )

                Scaffold(
                    bottomBar = {
                        if (showBottomBar) {
                            NavigationBar {
                                bottomTabs.forEach { tab ->
                                    val selected = currentRoute.startsWith(tab.route.substringBefore("?"))
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = {
                                            navController.navigate(tab.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                                        label = { Text(tab.label) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = SeerahEmerald,
                                            selectedTextColor = SeerahEmerald
                                        )
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = SeerahRoutes.SPLASH,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        // 1. Splash
                        composable(SeerahRoutes.SPLASH) {
                            SplashScreen(
                                onboardingCompleted = prefs.onboardingCompleted,
                                onNavigateNext = { done ->
                                    val target = if (done) SeerahRoutes.HOME else SeerahRoutes.ONBOARDING
                                    navController.navigate(target) {
                                        popUpTo(SeerahRoutes.SPLASH) { inclusive = true }
                                    }
                                }
                            )
                        }
                        // 2. Onboarding
                        composable(SeerahRoutes.ONBOARDING) {
                            OnboardingScreen(
                                onFinish = {
                                    scope.launch { repository.preferences.setOnboardingCompleted(true) }
                                    navController.navigate(SeerahRoutes.HOME) {
                                        popUpTo(SeerahRoutes.ONBOARDING) { inclusive = true }
                                    }
                                }
                            )
                        }
                        // 3. Home
                        composable(SeerahRoutes.HOME) {
                            HomeScreen(
                                repository = repository,
                                lastReadTitle = prefs.lastReadTitle,
                                lastReadRoute = prefs.lastReadRoute,
                                onOpenRoute = { route -> navController.navigate(route) },
                                onOpenTimeline = { navController.navigate(SeerahRoutes.TIMELINE) },
                                onOpenAtlas = { mapId -> navController.navigate(SeerahRoutes.historicalAtlas(mapId)) },
                                onOpenBattles = { navController.navigate(SeerahRoutes.BATTLE_ENCYCLOPEDIA) },
                                onOpenPeople = { navController.navigate(SeerahRoutes.PEOPLE_ENCYCLOPEDIA) },
                                onOpenPlaces = { navController.navigate(SeerahRoutes.HISTORICAL_PLACES) },
                                onOpenLibrary = { navController.navigate(SeerahRoutes.SOURCES_LIBRARY) },
                                onOpenSearch = { navController.navigate(SeerahRoutes.GLOBAL_SEARCH) },
                                onOpenHijra = { navController.navigate(SeerahRoutes.HIJRA_JOURNEY) },
                                onOpenEvent = { id -> navController.navigate(SeerahRoutes.eventDetails(id)) }
                            )
                        }
                        // 4. Timeline
                        composable(SeerahRoutes.TIMELINE) {
                            TimelineScreen(
                                repository = repository,
                                onOpenEvent = { id -> navController.navigate(SeerahRoutes.eventDetails(id)) },
                                onOpenMap = { mapId -> navController.navigate(SeerahRoutes.historicalAtlas(mapId)) },
                                onOpenImport = { navController.navigate(SeerahRoutes.IMPORT_BOOKS) }
                            )
                        }
                        // 5. Event Details
                        composable(
                            route = SeerahRoutes.EVENT_DETAILS,
                            arguments = listOf(navArgument("eventId") { type = NavType.LongType })
                        ) { entry ->
                            val id = entry.arguments?.getLong("eventId") ?: -1L
                            EventDetailsScreen(
                                eventId = id,
                                repository = repository,
                                onBack = { navController.popBackStack() },
                                onOpenMap = { mapId -> navController.navigate(SeerahRoutes.historicalAtlas(mapId)) },
                                onOpenNarrationCompare = { evId -> navController.navigate(SeerahRoutes.narrationComparison(evId)) },
                                onOpenEvent = { evId -> navController.navigate(SeerahRoutes.eventDetails(evId)) }
                            )
                        }
                        // 6. Historical Atlas
                        composable(
                            route = SeerahRoutes.HISTORICAL_ATLAS,
                            arguments = listOf(
                                navArgument("mapId") { type = NavType.StringType; defaultValue = "arabia" },
                                navArgument("placeId") { type = NavType.LongType; defaultValue = -1L }
                            )
                        ) { entry ->
                            val mapId = entry.arguments?.getString("mapId") ?: "arabia"
                            HistoricalAtlasScreen(
                                initialMapId = mapId,
                                repository = repository,
                                onOpenLayers = { navController.navigate(SeerahRoutes.MAP_LAYERS) },
                                onOpenLegend = { navController.navigate(SeerahRoutes.MAP_LEGEND) },
                                onOpenBattleMap = { bMapId -> navController.navigate(SeerahRoutes.battleMap(bMapId)) },
                                onOpenPlaceDetails = { pId -> navController.navigate(SeerahRoutes.placeDetails(pId)) },
                                onOpenImport = { navController.navigate(SeerahRoutes.IMPORT_BOOKS) }
                            )
                        }
                        // 7. Battle Map
                        composable(
                            route = SeerahRoutes.BATTLE_MAP,
                            arguments = listOf(navArgument("battleId") { type = NavType.StringType })
                        ) { entry ->
                            val battleMapId = entry.arguments?.getString("battleId") ?: "badr"
                            BattleMapScreen(
                                battleMapId = battleMapId,
                                repository = repository,
                                onBack = { navController.popBackStack() },
                                onOpenLayers = { navController.navigate(SeerahRoutes.MAP_LAYERS) },
                                onOpenLegend = { navController.navigate(SeerahRoutes.MAP_LEGEND) },
                                onOpenImport = { navController.navigate(SeerahRoutes.IMPORT_BOOKS) }
                            )
                        }
                        // 8. Map Layers
                        composable(SeerahRoutes.MAP_LAYERS) {
                            MapLayersScreen(repository = repository, onBack = { navController.popBackStack() })
                        }
                        // 9. Map Legend
                        composable(SeerahRoutes.MAP_LEGEND) {
                            MapLegendScreen(onBack = { navController.popBackStack() })
                        }
                        // 10. Battle Encyclopedia
                        composable(SeerahRoutes.BATTLE_ENCYCLOPEDIA) {
                            BattleEncyclopediaScreen(
                                repository = repository,
                                onOpenBattleDetails = { id -> navController.navigate(SeerahRoutes.battleDetails(id)) },
                                onOpenBattleMap = { mapId -> navController.navigate(SeerahRoutes.battleMap(mapId)) },
                                onOpenExpeditions = { navController.navigate(SeerahRoutes.EXPEDITIONS) },
                                onOpenImport = { navController.navigate(SeerahRoutes.IMPORT_BOOKS) }
                            )
                        }
                        // 11. Battle Details
                        composable(
                            route = SeerahRoutes.BATTLE_DETAILS,
                            arguments = listOf(navArgument("battleId") { type = NavType.LongType })
                        ) { entry ->
                            val id = entry.arguments?.getLong("battleId") ?: -1L
                            BattleDetailsScreen(
                                battleId = id,
                                repository = repository,
                                onBack = { navController.popBackStack() },
                                onOpenBattleMap = { mapId -> navController.navigate(SeerahRoutes.battleMap(mapId)) },
                                onOpenNarrationCompare = { evId -> navController.navigate(SeerahRoutes.narrationComparison(evId)) }
                            )
                        }
                        // 12. Expeditions
                        composable(SeerahRoutes.EXPEDITIONS) {
                            ExpeditionsScreen(
                                repository = repository,
                                onBack = { navController.popBackStack() },
                                onOpenDetails = { id -> navController.navigate(SeerahRoutes.eventDetails(id)) },
                                onOpenImport = { navController.navigate(SeerahRoutes.IMPORT_BOOKS) }
                            )
                        }
                        // 13. People Encyclopedia
                        composable(SeerahRoutes.PEOPLE_ENCYCLOPEDIA) {
                            PeopleEncyclopediaScreen(
                                repository = repository,
                                onBack = { navController.popBackStack() },
                                onOpenPerson = { id -> navController.navigate(SeerahRoutes.personDetails(id)) },
                                onOpenImport = { navController.navigate(SeerahRoutes.IMPORT_BOOKS) }
                            )
                        }
                        // 14. Person Details
                        composable(
                            route = SeerahRoutes.PERSON_DETAILS,
                            arguments = listOf(navArgument("personId") { type = NavType.LongType })
                        ) { entry ->
                            val id = entry.arguments?.getLong("personId") ?: -1L
                            PersonDetailsScreen(
                                personId = id,
                                repository = repository,
                                onBack = { navController.popBackStack() },
                                onOpenEvent = { evId -> navController.navigate(SeerahRoutes.eventDetails(evId)) }
                            )
                        }
                        // 15. Historical Places
                        composable(SeerahRoutes.HISTORICAL_PLACES) {
                            HistoricalPlacesScreen(
                                repository = repository,
                                onBack = { navController.popBackStack() },
                                onOpenPlace = { id -> navController.navigate(SeerahRoutes.placeDetails(id)) },
                                onOpenAtlas = { mapId -> navController.navigate(SeerahRoutes.historicalAtlas(mapId)) },
                                onOpenImport = { navController.navigate(SeerahRoutes.IMPORT_BOOKS) }
                            )
                        }
                        // 16. Place Details
                        composable(
                            route = SeerahRoutes.PLACE_DETAILS,
                            arguments = listOf(navArgument("placeId") { type = NavType.LongType })
                        ) { entry ->
                            val id = entry.arguments?.getLong("placeId") ?: -1L
                            PlaceDetailsScreen(
                                placeId = id,
                                repository = repository,
                                onBack = { navController.popBackStack() },
                                onOpenAtlas = { mapId -> navController.navigate(SeerahRoutes.historicalAtlas(mapId)) }
                            )
                        }
                        // 17. Hijra Journey
                        composable(SeerahRoutes.HIJRA_JOURNEY) {
                            HijraJourneyScreen(
                                repository = repository,
                                onBack = { navController.popBackStack() },
                                onOpenAtlas = { navController.navigate(SeerahRoutes.historicalAtlas("hijra")) }
                            )
                        }
                        // 18. Quran
                        composable(SeerahRoutes.QURAN) {
                            QuranScreen(
                                repository = repository,
                                onBack = { navController.popBackStack() },
                                onOpenTafsir = { id -> navController.navigate(SeerahRoutes.tafsir(id)) },
                                onOpenImport = { navController.navigate(SeerahRoutes.IMPORT_BOOKS) }
                            )
                        }
                        // 19. Tafsir
                        composable(
                            route = SeerahRoutes.TAFSIR,
                            arguments = listOf(navArgument("verseId") { type = NavType.LongType })
                        ) { entry ->
                            val id = entry.arguments?.getLong("verseId") ?: -1L
                            TafsirScreen(verseId = id, repository = repository, onBack = { navController.popBackStack() })
                        }
                        // 20. Hadith Library
                        composable(SeerahRoutes.HADITH_LIBRARY) {
                            HadithLibraryScreen(
                                repository = repository,
                                onBack = { navController.popBackStack() },
                                onOpenHadith = { id -> navController.navigate(SeerahRoutes.hadithDetails(id)) },
                                onOpenImport = { navController.navigate(SeerahRoutes.IMPORT_BOOKS) }
                            )
                        }
                        // 21. Hadith Details
                        composable(
                            route = SeerahRoutes.HADITH_DETAILS,
                            arguments = listOf(navArgument("hadithId") { type = NavType.LongType })
                        ) { entry ->
                            val id = entry.arguments?.getLong("hadithId") ?: -1L
                            HadithDetailsScreen(hadithId = id, repository = repository, onBack = { navController.popBackStack() })
                        }
                        // 22. Narration Comparison
                        composable(
                            route = SeerahRoutes.NARRATION_COMPARISON,
                            arguments = listOf(navArgument("eventId") { type = NavType.LongType })
                        ) { entry ->
                            val id = entry.arguments?.getLong("eventId") ?: -1L
                            NarrationComparisonScreen(eventId = id, repository = repository, onBack = { navController.popBackStack() })
                        }
                        // 23. Sources Library
                        composable(SeerahRoutes.SOURCES_LIBRARY) {
                            SourcesLibraryScreen(
                                repository = repository,
                                onBack = { navController.popBackStack() },
                                onOpenBook = { id -> navController.navigate(SeerahRoutes.bookDetails(id)) },
                                onOpenImport = { navController.navigate(SeerahRoutes.IMPORT_BOOKS) },
                                onOpenManagement = { navController.navigate(SeerahRoutes.LIBRARY_MANAGEMENT) }
                            )
                        }
                        // 24. Book Details
                        composable(
                            route = SeerahRoutes.BOOK_DETAILS,
                            arguments = listOf(navArgument("bookId") { type = NavType.LongType })
                        ) { entry ->
                            val id = entry.arguments?.getLong("bookId") ?: -1L
                            BookDetailsScreen(
                                bookId = id,
                                repository = repository,
                                onBack = { navController.popBackStack() },
                                onOpenImport = { navController.navigate(SeerahRoutes.IMPORT_BOOKS) }
                            )
                        }
                        // 25. Import Books
                        composable(SeerahRoutes.IMPORT_BOOKS) {
                            ImportBooksScreen(repository = repository, onBack = { navController.popBackStack() })
                        }
                        // 26. Library Management
                        composable(SeerahRoutes.LIBRARY_MANAGEMENT) {
                            LibraryManagementScreen(
                                repository = repository,
                                onBack = { navController.popBackStack() },
                                onOpenImport = { navController.navigate(SeerahRoutes.IMPORT_BOOKS) }
                            )
                        }
                        // 27. Global Search
                        composable(SeerahRoutes.GLOBAL_SEARCH) {
                            GlobalSearchScreen(
                                repository = repository,
                                onBack = { navController.popBackStack() },
                                onSubmitQuery = { q -> navController.navigate(SeerahRoutes.searchResults(q)) }
                            )
                        }
                        // 28. Search Results
                        composable(
                            route = SeerahRoutes.SEARCH_RESULTS,
                            arguments = listOf(navArgument("query") { type = NavType.StringType })
                        ) { entry ->
                            val raw = entry.arguments?.getString("query") ?: ""
                            val decoded = runCatching { URLDecoder.decode(raw, "UTF-8") }.getOrDefault(raw)
                            SearchResultsScreen(
                                initialQuery = decoded,
                                repository = repository,
                                onBack = { navController.popBackStack() },
                                onOpenResult = { item ->
                                    when (item.contentType) {
                                        SearchContentFilter.EVENTS, SearchContentFilter.EXPEDITIONS ->
                                            navController.navigate(SeerahRoutes.eventDetails(item.id))
                                        SearchContentFilter.BATTLES ->
                                            navController.navigate(SeerahRoutes.battleDetails(item.id))
                                        SearchContentFilter.PEOPLE ->
                                            navController.navigate(SeerahRoutes.personDetails(item.id))
                                        SearchContentFilter.PLACES ->
                                            navController.navigate(SeerahRoutes.placeDetails(item.id))
                                        SearchContentFilter.BOOKS ->
                                            navController.navigate(SeerahRoutes.bookDetails(item.id))
                                        SearchContentFilter.VERSES ->
                                            navController.navigate(SeerahRoutes.tafsir(item.id))
                                        SearchContentFilter.HADITHS ->
                                            navController.navigate(SeerahRoutes.hadithDetails(item.id))
                                        else -> {}
                                    }
                                }
                            )
                        }
                        // More Hub
                        composable(SeerahRoutes.MORE) {
                            MoreHubScreen(
                                onOpenPeople = { navController.navigate(SeerahRoutes.PEOPLE_ENCYCLOPEDIA) },
                                onOpenPlaces = { navController.navigate(SeerahRoutes.HISTORICAL_PLACES) },
                                onOpenHijra = { navController.navigate(SeerahRoutes.HIJRA_JOURNEY) },
                                onOpenQuran = { navController.navigate(SeerahRoutes.QURAN) },
                                onOpenHadith = { navController.navigate(SeerahRoutes.HADITH_LIBRARY) },
                                onOpenLibrary = { navController.navigate(SeerahRoutes.SOURCES_LIBRARY) },
                                onOpenFavorites = { navController.navigate(SeerahRoutes.FAVORITES) },
                                onOpenNotes = { navController.navigate(SeerahRoutes.NOTES) },
                                onOpenQuiz = { navController.navigate(SeerahRoutes.QUIZ) },
                                onOpenDownloads = { navController.navigate(SeerahRoutes.DOWNLOADS) },
                                onOpenSettings = { navController.navigate(SeerahRoutes.SETTINGS) },
                                onOpenAbout = { navController.navigate(SeerahRoutes.ABOUT) }
                            )
                        }
                        // 29. Favorites
                        composable(SeerahRoutes.FAVORITES) {
                            FavoritesScreen(repository = repository, onBack = { navController.popBackStack() })
                        }
                        // 30. Notes
                        composable(SeerahRoutes.NOTES) {
                            NotesScreen(repository = repository, onBack = { navController.popBackStack() })
                        }
                        // 31. Quiz
                        composable(SeerahRoutes.QUIZ) {
                            QuizScreen(
                                repository = repository,
                                onBack = { navController.popBackStack() },
                                onOpenImport = { navController.navigate(SeerahRoutes.IMPORT_BOOKS) }
                            )
                        }
                        // 32. Downloads
                        composable(SeerahRoutes.DOWNLOADS) {
                            DownloadsScreen(
                                repository = repository,
                                onBack = { navController.popBackStack() },
                                onOpenImport = { navController.navigate(SeerahRoutes.IMPORT_BOOKS) }
                            )
                        }
                        // 33. Settings
                        composable(SeerahRoutes.SETTINGS) {
                            SettingsScreen(
                                repository = repository,
                                prefsState = prefs,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        // 34. About
                        composable(SeerahRoutes.ABOUT) {
                            AboutScreen(onBack = { navController.popBackStack() })
                        }
                    }
                }
            }
        }
    }
}
