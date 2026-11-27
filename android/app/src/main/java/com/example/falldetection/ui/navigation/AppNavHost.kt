package com.example.falldetection.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.falldetection.di.AppContainer
import com.example.falldetection.ui.screens.ContactDetailScreen
import com.example.falldetection.ui.screens.ContactFormScreen
import com.example.falldetection.ui.screens.ContactsScreen
import com.example.falldetection.ui.screens.FallDetailScreen
import com.example.falldetection.ui.screens.HistoryScreen
import com.example.falldetection.ui.screens.HomeScreen
import com.example.falldetection.ui.screens.OnboardingScreen
import com.example.falldetection.ui.screens.ProfileScreen
import kotlinx.coroutines.runBlocking

private data class TabItem(val route: String, val label: String, val icon: ImageVector)

private val TABS = listOf(
    TabItem("beranda", "Beranda", Icons.Default.Home),
    TabItem("riwayat", "Riwayat", Icons.Default.History),
    TabItem("kontak", "Kontak", Icons.Default.Phone),
    TabItem("profil", "Profil", Icons.Default.Person),
)

@Composable
private fun TabsScaffold(
    navController: NavHostController,
    current: String,
    content: @Composable () -> Unit,
) {
    Scaffold(
        bottomBar = {
            NavigationBar {
                TABS.forEach { tab ->
                    NavigationBarItem(
                        selected = current == tab.route,
                        onClick = {
                            if (current != tab.route) {
                                navController.navigate(tab.route) {
                                    launchSingleTop = true
                                    restoreState = true
                                    popUpTo("beranda") { saveState = true }
                                }
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(tab.label) },
                        alwaysShowLabel = true,
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding)) { content() }
    }
}

@Composable
fun AppNavHost(container: AppContainer) {
    val navController = rememberNavController()
    val startDestination = remember {
        if (runBlocking { container.preferences.isOnboardingDone() }) "beranda" else "onboarding"
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable("onboarding") {
            OnboardingScreen(container) {
                navController.navigate("beranda") {
                    popUpTo("onboarding") { inclusive = true }
                }
            }
        }

        composable("beranda") {
            TabsScaffold(navController, "beranda") {
                HomeScreen(
                    container = container,
                    onOpenHistory = {
                        navController.navigate("riwayat") {
                            launchSingleTop = true
                            restoreState = true
                            popUpTo("beranda") { saveState = true }
                        }
                    },
                    onOpenContacts = {
                        navController.navigate("kontak") {
                            launchSingleTop = true
                            restoreState = true
                            popUpTo("beranda") { saveState = true }
                        }
                    },
                )
            }
        }

        composable("riwayat") {
            TabsScaffold(navController, "riwayat") {
                HistoryScreen(container) { id -> navController.navigate("riwayat/$id") }
            }
        }

        composable(
            "riwayat/{id}",
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString("id").orEmpty()
            FallDetailScreen(container, id) { navController.popBackStack() }
        }

        composable("kontak") {
            TabsScaffold(navController, "kontak") {
                ContactsScreen(
                    container = container,
                    onOpenDetail = { id -> navController.navigate("kontak/$id") },
                    onAdd = { navController.navigate("kontak/form/new") },
                )
            }
        }

        composable(
            "kontak/{id}",
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString("id").orEmpty()
            ContactDetailScreen(
                container = container,
                id = id,
                onEdit = { contactId -> navController.navigate("kontak/form/$contactId") },
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            "kontak/form/{id}",
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString("id") ?: "new"
            ContactFormScreen(
                container = container,
                id = id,
                onDone = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
            )
        }

        composable("profil") {
            TabsScaffold(navController, "profil") { ProfileScreen(container) }
        }
    }
}
