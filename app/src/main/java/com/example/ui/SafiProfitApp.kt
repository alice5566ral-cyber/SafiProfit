package com.example.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.data.model.TransactionRecord
import com.example.data.model.TransactionType
import com.example.ui.components.AdExpenseModalSheet
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.IncomeModalSheet
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TransactionsScreen
import com.example.ui.viewmodel.LedgerViewModel

object Routes {
    const val HOME = "home"
    const val TRANSACTIONS = "transactions"
    const val REPORTS = "reports"
    const val SETTINGS = "settings"
}

private data class NavItem(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private sealed interface ActiveSheet {
    data object None : ActiveSheet
    data class Income(val editing: TransactionRecord? = null) : ActiveSheet
    data class AdExpense(val editing: TransactionRecord? = null) : ActiveSheet
}

@Composable
fun SafiProfitApp(
    viewModel: LedgerViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    var activeSheet by remember { mutableStateOf<ActiveSheet>(ActiveSheet.None) }
    var pendingDeleteRecord by remember { mutableStateOf<TransactionRecord?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val navItems = remember {
        listOf(
            NavItem(Routes.HOME, R.string.nav_home, Icons.Rounded.Home, Icons.Outlined.Home),
            NavItem(Routes.TRANSACTIONS, R.string.nav_transactions, Icons.Rounded.ReceiptLong, Icons.Outlined.ReceiptLong),
            NavItem(Routes.REPORTS, R.string.nav_reports, Icons.Rounded.BarChart, Icons.Outlined.BarChart),
            NavItem(Routes.SETTINGS, R.string.nav_settings, Icons.Rounded.Settings, Icons.Outlined.Settings)
        )
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val navigateToTab: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            contentWindowInsets = if (useRail) {
                WindowInsets.safeDrawing
            } else {
                WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
            },
            bottomBar = {
                if (!useRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 0.dp
                    ) {
                        navItems.forEach { item ->
                            val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTab(item.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = stringResource(item.labelRes)
                                    )
                                },
                                label = { Text(stringResource(item.labelRes)) },
                                modifier = Modifier.testTag("nav_${item.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        navItems.forEach { item ->
                            val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(item.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = stringResource(item.labelRes)
                                    )
                                },
                                label = { Text(stringResource(item.labelRes)) },
                                modifier = Modifier.testTag("nav_${item.route}")
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = Routes.HOME,
                    modifier = Modifier.weight(1f)
                ) {
                    composable(Routes.HOME) {
                        HomeScreen(
                            uiState = uiState,
                            onAddIncomeClick = { activeSheet = ActiveSheet.Income() },
                            onAddAdExpenseClick = { activeSheet = ActiveSheet.AdExpense() },
                            onMarkAsReceived = { id -> viewModel.markAsReceived(id) },
                            onEditTransaction = { record ->
                                activeSheet = if (record.type == TransactionType.INCOME) {
                                    ActiveSheet.Income(record)
                                } else {
                                    ActiveSheet.AdExpense(record)
                                }
                            },
                            onNavigateToReports = { navigateToTab(Routes.REPORTS) }
                        )
                    }

                    composable(Routes.TRANSACTIONS) {
                        TransactionsScreen(
                            uiState = uiState,
                            onSearchQueryChange = viewModel::updateSearchQuery,
                            onFilterSelect = viewModel::updateTransactionFilter,
                            onAddIncomeClick = { activeSheet = ActiveSheet.Income() },
                            onEditTransaction = { record ->
                                activeSheet = if (record.type == TransactionType.INCOME) {
                                    ActiveSheet.Income(record)
                                } else {
                                    ActiveSheet.AdExpense(record)
                                }
                            },
                            onDeleteTransactionRequest = { record -> pendingDeleteRecord = record },
                            onMarkAsReceived = { id -> viewModel.markAsReceived(id) }
                        )
                    }

                    composable(Routes.REPORTS) {
                        ReportsScreen(
                            uiState = uiState,
                            onSelectPeriod = viewModel::selectReportPeriod,
                            onCustomRangeChange = viewModel::updateCustomDateRange,
                            onSelectGranularity = viewModel::selectBreakdownGranularity
                        )
                    }

                    composable(Routes.SETTINGS) {
                        SettingsScreen(
                            uiState = uiState,
                            onSaveExchangeRates = viewModel::updateExchangeRates,
                            onToggleAlwaysShowDecimals = viewModel::setAlwaysShowDecimals,
                            onToggleWeekStartsMonday = viewModel::setWeekStartsOnMonday,
                            onGenerateBackupJson = viewModel::generateBackupJson,
                            onRestoreBackupJson = viewModel::restoreFromBackupJson,
                            onClearAllTransactions = viewModel::clearAllTransactions,
                            onNotifyMessage = viewModel::notifyMessage
                        )
                    }
                }
            }
        }
    }

    when (val sheet = activeSheet) {
        ActiveSheet.None -> Unit
        is ActiveSheet.Income -> {
            IncomeModalSheet(
                existingRecord = sheet.editing,
                onDismiss = { activeSheet = ActiveSheet.None },
                onSave = { amountMad, description, dateString, status ->
                    val existing = sheet.editing
                    if (existing == null) {
                        viewModel.addIncome(amountMad, description, dateString, status)
                    } else {
                        viewModel.updateTransaction(
                            existing.copy(
                                amountOriginal = amountMad,
                                amountMad = amountMad,
                                description = description,
                                dateString = dateString,
                                status = status
                            )
                        )
                    }
                    activeSheet = ActiveSheet.None
                },
                onDelete = sheet.editing?.let { existing ->
                    {
                        activeSheet = ActiveSheet.None
                        pendingDeleteRecord = existing
                    }
                }
            )
        }
        is ActiveSheet.AdExpense -> {
            AdExpenseModalSheet(
                settings = uiState.settings,
                existingRecord = sheet.editing,
                onDismiss = { activeSheet = ActiveSheet.None },
                onSave = { amountOriginal, currency, exchangeRate, description, dateString ->
                    val existing = sheet.editing
                    if (existing == null) {
                        viewModel.addAdExpense(amountOriginal, currency, exchangeRate, description, dateString)
                    } else {
                        viewModel.updateTransaction(
                            existing.copy(
                                amountOriginal = amountOriginal,
                                currency = currency,
                                exchangeRate = exchangeRate,
                                description = description,
                                dateString = dateString
                            )
                        )
                    }
                    activeSheet = ActiveSheet.None
                },
                onDelete = sheet.editing?.let { existing ->
                    {
                        activeSheet = ActiveSheet.None
                        pendingDeleteRecord = existing
                    }
                }
            )
        }
    }

    pendingDeleteRecord?.let { target ->
        ConfirmDeleteDialog(
            onConfirm = {
                viewModel.deleteTransaction(target.id)
                pendingDeleteRecord = null
            },
            onDismiss = { pendingDeleteRecord = null }
        )
    }
}
