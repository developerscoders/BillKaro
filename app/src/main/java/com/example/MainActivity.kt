package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.data.local.AppDatabase
import com.example.data.repository.BillKaroRepository
import com.example.ui.navigation.BillKaroApp
import com.example.ui.theme.BillKaroTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(applicationContext)
        val mongoSettings = com.example.data.remote.mongo.MongoSettingsManager(applicationContext)
        val mongoSyncManager = com.example.data.remote.mongo.MongoSyncManager(database, mongoSettings)
        val repository = BillKaroRepository(db = database, mongoSyncManager = mongoSyncManager)

        setContent {
            BillKaroTheme {
                BillKaroApp(repository = repository)
            }
        }
    }
}
