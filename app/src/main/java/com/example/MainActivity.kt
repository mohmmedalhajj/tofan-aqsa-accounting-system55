package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.example.data.AppDatabase
import com.example.data.QatRepository
import com.example.ui.AppViewModel
import com.example.ui.AppViewModelFactory
import com.example.ui.Screen
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Databases, Repositories, and ViewModel
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = QatRepository(database)
        val viewModelFactory = AppViewModelFactory(application, repository)
        val viewModel = ViewModelProvider(this, viewModelFactory)[AppViewModel::class.java]

        enableEdgeToEdge()
        
        setContent {
            val isDark by viewModel.isDarkMode.collectAsState()
            MyApplicationTheme(darkTheme = isDark) {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val currentScreen by viewModel.currentScreen.collectAsState()
                    
                    if (currentScreen != Screen.Dashboard && currentScreen != Screen.Splash && currentScreen != Screen.Login) {
                        BackHandler {
                            viewModel.navigateToDashboard()
                        }
                    }
                    
                    Crossfade(
                        targetState = currentScreen,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        label = "screen_navigation"
                    ) { screen ->
                        when (screen) {
                            is Screen.Splash -> SplashScreen(viewModel)
                            is Screen.Login -> LoginScreen(viewModel)
                            is Screen.Dashboard -> DashboardScreen(viewModel)
                            is Screen.Inventory -> InventoryScreen(viewModel)
                            is Screen.Sales -> SalesScreen(viewModel)
                            is Screen.Accounts -> AccountsScreen(viewModel)
                            is Screen.Customers -> CustomerScreen(viewModel)
                            is Screen.Suppliers -> SupplierScreen(viewModel)
                            is Screen.Expenses -> ExpensesScreen(viewModel)
                            is Screen.Transfers -> TransfersScreen(viewModel)
                            is Screen.Reports -> ReportsScreen(viewModel)
                            is Screen.Archive -> ArchiveScreen(viewModel)
                            is Screen.Settings -> SettingsScreen(viewModel)
                        }
                    }
                }
            }
        }
    }
}
