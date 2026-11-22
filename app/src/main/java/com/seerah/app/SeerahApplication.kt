package com.seerah.app

import android.app.Application
import com.seerah.data.backup.BackupRestoreManager
import com.seerah.data.importer.BookImportEngine
import com.seerah.data.prefs.SeerahPreferences
import com.seerah.data.repository.SeerahRepository
import com.seerah.data.updater.SeerahAppUpdater
import com.seerah.database.SeerahDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SeerahApplication : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var repository: SeerahRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val database = SeerahDatabase.build(this)
        val prefs = SeerahPreferences(this)
        val importer = BookImportEngine(this, database)
        val backup = BackupRestoreManager(this, database)
        val updater = SeerahAppUpdater(this)

        repository = SeerahRepository(
            context = this,
            database = database,
            preferences = prefs,
            importEngine = importer,
            backupRestoreManager = backup,
            appUpdater = updater
        )

        appScope.launch {
            repository.initializeOfflineCatalog()
        }
    }
}
