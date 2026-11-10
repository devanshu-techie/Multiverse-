package com.devanshu.miniverse.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.devanshu.miniverse.data.GameRepository
import com.devanshu.miniverse.feature.gameplay.GameplayScreen
import com.devanshu.miniverse.feature.home.HomeScreen
import com.devanshu.miniverse.feature.library.LibraryScreen
import com.devanshu.miniverse.feature.onboarding.OnboardingScreen
import com.devanshu.miniverse.feature.result.ResultScreen
import com.devanshu.miniverse.feature.setup.SetupScreen
import com.devanshu.miniverse.feature.splash.SplashScreen
import com.devanshu.miniverse.feature.wallet.WalletScreen

object Routes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val LIBRARY = "library"
    const val WALLET = "wallet"
    fun setup(gameId: String) = "setup/$gameId"
    fun play(matchId: String) = "play/$matchId"
    fun result(matchId: String) = "result/$matchId"
}

@Composable
fun MiniVerseNavHost(repository: GameRepository) {
    val nav = rememberNavController()
    val player by repository.player.collectAsState(initial = null)

    NavHost(navController = nav, startDestination = Routes.SPLASH) {
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
                onPickGame = { nav.navigate(Routes.setup(it)) },
            )
        }
        composable(Routes.LIBRARY) {
            LibraryScreen(onPickGame = { nav.navigate(Routes.setup(it)) }, onBack = { nav.popBackStack() })
        }
        composable(Routes.WALLET) {
            WalletScreen(repository, player?.playerId, onBack = { nav.popBackStack() })
        }
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
            )
        }
    }
}
