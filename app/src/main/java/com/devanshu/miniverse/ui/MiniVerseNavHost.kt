package com.devanshu.miniverse.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.devanshu.miniverse.data.GameRepository
import com.devanshu.miniverse.feature.achievements.AchievementsScreen
import com.devanshu.miniverse.feature.extras.DailySpinScreen
import com.devanshu.miniverse.feature.extras.GameDetailsScreen
import com.devanshu.miniverse.feature.extras.ShopScreen
import com.devanshu.miniverse.feature.extras.StatsScreen
import com.devanshu.miniverse.feature.gameplay.GameplayScreen
import com.devanshu.miniverse.feature.history.HistoryScreen
import com.devanshu.miniverse.feature.home.HomeScreen
import com.devanshu.miniverse.feature.library.LibraryScreen
import com.devanshu.miniverse.feature.onboarding.OnboardingScreen
import com.devanshu.miniverse.feature.profile.ProfileScreen
import com.devanshu.miniverse.feature.result.ResultScreen
import com.devanshu.miniverse.feature.settings.SettingsScreen
import com.devanshu.miniverse.feature.setup.SetupScreen
import com.devanshu.miniverse.feature.splash.SplashScreen
import com.devanshu.miniverse.feature.wallet.WalletScreen

object Routes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val LIBRARY = "library"
    const val ACHIEVEMENTS = "achievements"
    const val PROFILE = "profile"
    const val WALLET = "wallet"
    const val SETTINGS = "settings"
    const val HISTORY = "history"
    const val SPIN = "spin"
    const val SHOP = "shop"
    const val STATS = "stats"
    fun details(gameId: String) = "details/$gameId"
    fun setup(gameId: String) = "setup/$gameId"
    fun play(matchId: String) = "play/$matchId"
    fun result(matchId: String) = "result/$matchId"
}

private data class Tab(val route: String, val emoji: String, val label: String)

private val tabs = listOf(
    Tab(Routes.HOME, "🏠", "Home"),
    Tab(Routes.LIBRARY, "🎮", "Games"),
    Tab(Routes.ACHIEVEMENTS, "🏆", "Awards"),
    Tab(Routes.PROFILE, "🦊", "Me"),
)

@Composable
fun MiniVerseNavHost(repository: GameRepository) {
    val nav = rememberNavController()
    val player by repository.player.collectAsState(initial = null)
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (currentRoute in tabs.map { it.route }) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                if (currentRoute != tab.route) {
                                    nav.navigate(tab.route) {
                                        popUpTo(Routes.HOME) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Text(tab.emoji, fontSize = 20.sp) },
                            label = { Text(tab.label, fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Routes.SPLASH,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.SPLASH) {
                SplashScreen(
                    hasPlayer = { repository },
                    onReady = { exists ->
                        nav.navigate(if (exists) Routes.HOME else Routes.ONBOARDING) {
                            popUpTo(Routes.SPLASH) { inclusive = true }
                        }
                    },
                )
            }
            composable(Routes.ONBOARDING) {
                OnboardingScreen(repository) {
                    nav.navigate(Routes.HOME) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
                }
            }
            composable(Routes.HOME) {
                HomeScreen(
                    repository = repository,
                    onOpenLibrary = { nav.navigate(Routes.LIBRARY) },
                    onOpenWallet = { nav.navigate(Routes.WALLET) },
                    onPickGame = { nav.navigate(Routes.details(it)) },
                    onOpenSpin = { nav.navigate(Routes.SPIN) },
                    onOpenShop = { nav.navigate(Routes.SHOP) },
                    onOpenStats = { nav.navigate(Routes.STATS) },
                    onResumeMatch = { nav.navigate(Routes.play(it)) },
                    onRestartGame = { nav.navigate(Routes.setup(it)) },
                )
            }
            composable(Routes.LIBRARY) {
                LibraryScreen(onPickGame = { nav.navigate(Routes.details(it)) })
            }
            composable(Routes.SPIN) { DailySpinScreen(repository, onBack = { nav.popBackStack() }) }
            composable(Routes.SHOP) { ShopScreen(repository, onBack = { nav.popBackStack() }) }
            composable(Routes.STATS) { StatsScreen(repository, onBack = { nav.popBackStack() }) }
            composable(
                "details/{gameId}",
                arguments = listOf(navArgument("gameId") { type = NavType.StringType }),
            ) { entry ->
                val gameId = entry.arguments?.getString("gameId").orEmpty()
                GameDetailsScreen(
                    repository = repository,
                    gameId = gameId,
                    onBack = { nav.popBackStack() },
                    onPlay = { nav.navigate(Routes.setup(gameId)) },
                )
            }
            composable(Routes.ACHIEVEMENTS) { AchievementsScreen(repository) }
            composable(Routes.PROFILE) {
                ProfileScreen(
                    repository = repository,
                    onOpenSettings = { nav.navigate(Routes.SETTINGS) },
                    onOpenHistory = { nav.navigate(Routes.HISTORY) },
                )
            }
            composable(Routes.WALLET) {
                WalletScreen(repository, player?.playerId, onBack = { nav.popBackStack() })
            }
            composable(Routes.SETTINGS) { SettingsScreen(onBack = { nav.popBackStack() }) }
            composable(Routes.HISTORY) { HistoryScreen(repository, onBack = { nav.popBackStack() }) }
            composable(
                "setup/{gameId}",
                arguments = listOf(navArgument("gameId") { type = NavType.StringType }),
            ) { entry ->
                SetupScreen(
                    repository = repository,
                    gameId = entry.arguments?.getString("gameId").orEmpty(),
                    onBack = { nav.popBackStack() },
                    onMatchCreated = { matchId ->
                        nav.navigate(Routes.play(matchId)) { popUpTo(Routes.HOME) }
                    },
                )
            }
            composable(
                "play/{matchId}",
                arguments = listOf(navArgument("matchId") { type = NavType.StringType }),
            ) { entry ->
                GameplayScreen(
                    repository = repository,
                    matchId = entry.arguments?.getString("matchId").orEmpty(),
                    onFinished = { matchId ->
                        nav.navigate(Routes.result(matchId)) { popUpTo(Routes.HOME) }
                    },
                    onQuit = { nav.popBackStack(Routes.HOME, inclusive = false) },
                )
            }
            composable(
                "result/{matchId}",
                arguments = listOf(navArgument("matchId") { type = NavType.StringType }),
            ) { entry ->
                ResultScreen(
                    repository = repository,
                    matchId = entry.arguments?.getString("matchId").orEmpty(),
                    onHome = { nav.popBackStack(Routes.HOME, inclusive = false) },
                    onNextRound = { nextId -> nav.navigate(Routes.play(nextId)) { popUpTo(Routes.HOME) } },
                    onPlayAgain = { id -> nav.navigate(Routes.setup(id)) { popUpTo(Routes.HOME) } },
                )
            }
        }
    }
}
