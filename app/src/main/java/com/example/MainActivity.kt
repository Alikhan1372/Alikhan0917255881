package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.data.AuthRepository
import com.example.data.EquipmentRepository
import com.example.model.Equipment
import com.example.model.UserProfile
import com.example.ui.components.AppBottomNavigation
import com.example.ui.components.AppHeader
import com.example.ui.components.EditDisplayNameDialog
import com.example.ui.components.NavigationTab
import com.example.ui.screens.DailyServiceScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.EquipmentDetailScreen
import com.example.ui.screens.InventoryScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.MonthlyScreen
import com.example.ui.screens.OilChangeScreen
import com.example.ui.theme.IndustrialPrimary
import com.example.ui.theme.MyApplicationTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.persistentCacheSettings
import kotlinx.coroutines.launch

private const val TAG = "MainActivity"

sealed class Screen {
    object Dashboard : Screen()
    data class EquipmentDetail(val equipment: Equipment) : Screen()
    data class DailyService(val equipment: Equipment) : Screen()
    data class OilChange(val equipment: Equipment) : Screen()
}

class MainActivity : ComponentActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var authRepository: AuthRepository
    private lateinit var equipmentRepository: EquipmentRepository
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        FirebaseApp.initializeApp(this)

        val databaseId = getString(R.string.firestore_database_id)
        db = FirebaseFirestore.getInstance(databaseId)
        db.firestoreSettings = firestoreSettings {
            setLocalCacheSettings(persistentCacheSettings { })
        }

        authRepository = AuthRepository(applicationContext, db)
        equipmentRepository = EquipmentRepository(db)

        setContent {
            MyApplicationTheme {
                MainAppRoot(
                    authRepository = authRepository,
                    equipmentRepository = equipmentRepository
                )
            }
        }
    }
}

@Composable
fun MainAppRoot(
    authRepository: AuthRepository,
    equipmentRepository: EquipmentRepository
) {
    val currentUser by authRepository.currentUser.collectAsState()

    if (currentUser == null) {
        LoginScreen(
            authRepository = authRepository,
            onLoginSuccess = { }
        )
    } else {
        androidx.compose.runtime.key(currentUser!!.userId) {
            AuthenticatedWorkspace(
                profile = currentUser!!,
                authRepository = authRepository,
                equipmentRepository = equipmentRepository
            )
        }
    }
}

@Composable
fun AuthenticatedWorkspace(
    profile: UserProfile,
    authRepository: AuthRepository,
    equipmentRepository: EquipmentRepository
) {
    val coroutineScope = rememberCoroutineScope()
    var currentTab by remember { mutableStateOf(NavigationTab.EQUIPMENT) }
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Dashboard) }
    var showEditDisplayNameDialog by remember { mutableStateOf(false) }

    // Seed initial equipment and inventory ONLY once an authorized manager/representative is authenticated
    LaunchedEffect(profile.userId) {
        if (profile.roleEnum == com.example.model.UserRole.REPRESENTATIVE || 
            profile.roleEnum == com.example.model.UserRole.TECHNICAL_MANAGER) {
            try {
                equipmentRepository.seedInitialDataIfEmpty()
            } catch (_: Exception) { }
        }
    }

    // Live Data Flows — Auth-gated: only collected when user is logged in
    val equipmentList by equipmentRepository.observeEquipmentList()
        .collectAsState(initial = emptyList())
    val inventoryList by equipmentRepository.observeInventory()
        .collectAsState(initial = emptyList())
    val transactions by equipmentRepository.observeOilTransactions()
        .collectAsState(initial = emptyList())
    val monthlyReports by equipmentRepository.observeMonthlyReports()
        .collectAsState(initial = emptyList())

    // BackHandler for secondary screens
    BackHandler(enabled = currentScreen !is Screen.Dashboard) {
        when (currentScreen) {
            is Screen.DailyService -> {
                val eq = (currentScreen as Screen.DailyService).equipment
                currentScreen = Screen.EquipmentDetail(eq)
            }
            is Screen.OilChange -> {
                val eq = (currentScreen as Screen.OilChange).equipment
                currentScreen = Screen.EquipmentDetail(eq)
            }
            is Screen.EquipmentDetail -> {
                currentScreen = Screen.Dashboard
            }
            else -> {
                currentScreen = Screen.Dashboard
            }
        }
    }

    Scaffold(
        topBar = {
            AppHeader(
                currentUser = profile,
                onEditDisplayName = { showEditDisplayNameDialog = true },
                onLogout = { authRepository.logout() }
            )
        },
        bottomBar = {
            if (currentScreen is Screen.Dashboard) {
                AppBottomNavigation(
                    currentTab = currentTab,
                    onTabSelected = { currentTab = it }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val screen = currentScreen) {
                is Screen.EquipmentDetail -> {
                    EquipmentDetailScreen(
                        equipment = screen.equipment,
                        currentUser = profile,
                        equipmentRepository = equipmentRepository,
                        onNavigateBack = { currentScreen = Screen.Dashboard },
                        onNavigateToDailyService = { currentScreen = Screen.DailyService(screen.equipment) },
                        onNavigateToOilChange = { currentScreen = Screen.OilChange(screen.equipment) }
                    )
                }

                is Screen.DailyService -> {
                    DailyServiceScreen(
                        equipment = screen.equipment,
                        currentUser = profile,
                        equipmentRepository = equipmentRepository,
                        onNavigateBack = { currentScreen = Screen.EquipmentDetail(screen.equipment) }
                    )
                }

                is Screen.OilChange -> {
                    OilChangeScreen(
                        equipment = screen.equipment,
                        currentUser = profile,
                        equipmentRepository = equipmentRepository,
                        onNavigateBack = { currentScreen = Screen.EquipmentDetail(screen.equipment) }
                    )
                }

                is Screen.Dashboard -> {
                    when (currentTab) {
                        NavigationTab.EQUIPMENT -> {
                            DashboardScreen(
                                currentUser = profile,
                                equipmentList = equipmentList,
                                inventoryList = inventoryList,
                                equipmentRepository = equipmentRepository,
                                onEquipmentClick = { equip ->
                                    currentScreen = Screen.EquipmentDetail(equip)
                                }
                            )
                        }

                        NavigationTab.INVENTORY -> {
                            InventoryScreen(
                                currentUser = profile,
                                inventoryList = inventoryList,
                                transactions = transactions,
                                equipmentRepository = equipmentRepository
                            )
                        }

                        NavigationTab.MONTHLY -> {
                            MonthlyScreen(
                                currentUser = profile,
                                reports = monthlyReports,
                                equipmentRepository = equipmentRepository
                            )
                        }
                    }
                }
            }
        }
    }

    if (showEditDisplayNameDialog) {
        EditDisplayNameDialog(
            currentDisplayName = profile.displayName,
            onDismiss = { showEditDisplayNameDialog = false },
            onConfirm = { newName ->
                coroutineScope.launch {
                    authRepository.updateDisplayName(newName)
                    showEditDisplayNameDialog = false
                }
            }
        )
    }
}
