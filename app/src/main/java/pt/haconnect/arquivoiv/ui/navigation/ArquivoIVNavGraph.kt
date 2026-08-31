package pt.haconnect.arquivoiv.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import pt.haconnect.arquivoiv.R
import pt.haconnect.arquivoiv.ui.dashboard.DashboardScreen
import pt.haconnect.arquivoiv.ui.fatura.FaturaDetailScreen
import pt.haconnect.arquivoiv.ui.fatura.FaturaFormScreen
import pt.haconnect.arquivoiv.ui.fatura.FaturaListScreen
import pt.haconnect.arquivoiv.ui.fatura.FaturaSearchScreen
import pt.haconnect.arquivoiv.ui.settings.AboutScreen
import pt.haconnect.arquivoiv.ui.settings.SettingsScreen
import pt.haconnect.arquivoiv.ui.theme.Primary
import pt.haconnect.arquivoiv.ui.theme.PrimaryLight
import pt.haconnect.arquivoiv.ui.theme.Surface

sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object FaturaList : Screen("fatura_list")
    data object FaturaDetail : Screen("fatura_detail/{faturaId}") {
        fun createRoute(faturaId: Long) = "fatura_detail/$faturaId"
    }
    data object FaturaForm : Screen("fatura_form?faturaId={faturaId}") {
        fun createRoute(faturaId: Long? = null) =
            if (faturaId != null) "fatura_form?faturaId=$faturaId"
            else "fatura_form"
    }
    data object Pesquisa : Screen("pesquisa")
    data object Settings : Screen("settings")
    data object Export : Screen("export")
    data object About : Screen("about")
}

data class BottomNavItem(
    val screen: Screen,
    val labelResId: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
fun bottomNavItems() = listOf(
    BottomNavItem(
        screen = Screen.Dashboard,
        labelResId = R.string.nav_home,
        selectedIcon = Icons.Filled.Dashboard,
        unselectedIcon = Icons.Outlined.Dashboard
    ),
    BottomNavItem(
        screen = Screen.FaturaList,
        labelResId = R.string.nav_invoices,
        selectedIcon = Icons.Filled.ReceiptLong,
        unselectedIcon = Icons.Outlined.ReceiptLong
    ),
    BottomNavItem(
        screen = Screen.Pesquisa,
        labelResId = R.string.search_title,
        selectedIcon = Icons.Filled.Search,
        unselectedIcon = Icons.Outlined.Search
    ),
    BottomNavItem(
        screen = Screen.Settings,
        labelResId = R.string.nav_settings,
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )
)

@Composable
fun ArquivoIVNavGraph(
    faturaInicialId: Long? = null,
    onFaturaInicialHandled: () -> Unit = {}
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Fase 5: se a app foi aberta pela notificação de retenção, navega direto ao detalhe.
    LaunchedEffect(faturaInicialId) {
        if (faturaInicialId != null) {
            navController.navigate(Screen.FaturaDetail.createRoute(faturaInicialId)) {
                launchSingleTop = true
            }
            onFaturaInicialHandled()
        }
    }

    val items = bottomNavItems()
    val showBottomBar = currentRoute in listOf(
        Screen.Dashboard.route,
        Screen.FaturaList.route,
        Screen.Pesquisa.route,
        Screen.Settings.route
    )
    val showFab = currentRoute == Screen.FaturaList.route

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = Surface
                ) {
                    items.forEach { item ->
                        val selected = currentRoute == item.screen.route
                        val label = stringResource(item.labelResId)
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = label
                                )
                            },
                            label = { Text(text = label, maxLines = 1) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Primary,
                                selectedTextColor = Primary,
                                indicatorColor = PrimaryLight.copy(alpha = 0.2f)
                            )
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (showFab) {
                FloatingActionButton(
                    onClick = {
                        navController.navigate(Screen.FaturaForm.createRoute())
                    },
                    containerColor = Primary,
                    contentColor = androidx.compose.ui.graphics.Color.White
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(R.string.fab_new_invoice)
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    onFaturaClick = { faturaId ->
                        navController.navigate(Screen.FaturaDetail.createRoute(faturaId))
                    }
                )
            }
            composable(Screen.FaturaList.route) {
                FaturaListScreen(
                    onFaturaClick = { faturaId ->
                        navController.navigate(Screen.FaturaDetail.createRoute(faturaId))
                    },
                    onEditClick = { faturaId ->
                        navController.navigate(Screen.FaturaForm.createRoute(faturaId))
                    }
                )
            }
            composable(
                route = Screen.FaturaDetail.route,
                arguments = listOf(navArgument("faturaId") { type = NavType.StringType })
            ) { backStackEntry ->
                val faturaId = backStackEntry.arguments?.getString("faturaId")?.toLongOrNull()
                FaturaDetailScreen(
                    faturaId = faturaId ?: return@composable,
                    onNavigateBack = { navController.popBackStack() },
                    onEditClick = { id ->
                        navController.navigate(Screen.FaturaForm.createRoute(id))
                    }
                )
            }
            composable(
                route = Screen.FaturaForm.route,
                arguments = listOf(navArgument("faturaId") { type = NavType.StringType; nullable = true })
            ) { backStackEntry ->
                val faturaId = backStackEntry.arguments?.getString("faturaId")?.toLongOrNull()
                FaturaFormScreen(
                    faturaId = faturaId,
                    onNavigateBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() }
                )
            }
            composable(Screen.Pesquisa.route) {
                FaturaSearchScreen(
                    onFaturaClick = { faturaId ->
                        navController.navigate(Screen.FaturaDetail.createRoute(faturaId))
                    },
                    onEditClick = { faturaId ->
                        navController.navigate(Screen.FaturaForm.createRoute(faturaId))
                    }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateToExport = {
                        navController.navigate(Screen.Export.route)
                    },
                    onNavigateToAbout = {
                        navController.navigate(Screen.About.route)
                    }
                )
            }
            composable(Screen.Export.route) {
                pt.haconnect.arquivoiv.ui.export.ExportScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.About.route) {
                pt.haconnect.arquivoiv.ui.settings.AboutScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}









