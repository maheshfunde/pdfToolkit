package com.yashodatech.pdftoolkit

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.yashodatech.pdftoolkit.data.PreferencesManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Home : Screen("home")
    object ImageToPdf : Screen("image_to_pdf")
    object MergePdf : Screen("merge_pdf")
    object SplitPdf : Screen("split_pdf")
    object CompressPdf : Screen("compress_pdf")
    object ViewPdf : Screen("view_pdf")
    object Settings : Screen("settings")

    object WordToPdf : Screen("word_to_pdf")

    object OrganizePages : Screen("organize_pages")
    object WatermarkPdf : Screen("watermark_pdf")
    object ExportImages : Screen("export_images")
    object ExportWord : Screen("export_word")
    object Ocr : Screen("ocr")
    object UnlockPdf : Screen("unlock_pdf")

    // Opens a specific PDF file in the in-app viewer.
    object ViewPdfFile : Screen("view_pdf_file/{uri}") {
        fun createRoute(uri: String): String =
            "view_pdf_file/${android.net.Uri.encode(uri)}"
    }
}

@Composable
fun AppNavigation() {

    val context = LocalContext.current
    val prefsManager = remember { PreferencesManager(context) }
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    // Check onboarding status
    var startDestination by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val completed = prefsManager.onboardingCompleted.first()
        startDestination = if (completed) Screen.Home.route
        else Screen.Onboarding.route

        prefsManager.incrementAppOpenCount()
    }

    if (startDestination == null) return  // Wait for check

    NavHost(
        navController = navController,
        startDestination = startDestination!!,
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300))
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { -it / 3 },
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { -it / 3 },
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300))
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        }
    ) {

        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onComplete = {
                    scope.launch {
                        prefsManager.setOnboardingCompleted()
                    }
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                },
                onThemeSelect = { mode ->
                    scope.launch {
                        prefsManager.setThemeMode(mode)
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onToolClick = { route ->
                    scope.launch {
                        prefsManager.setLastUsedTool(route)
                    }
                    navController.navigate(route) {
                        launchSingleTop = true
                    }
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route) {
                        launchSingleTop = true
                    }
                },
                onOpenRecentDoc = { uri ->
                    navController.navigate(Screen.ViewPdfFile.createRoute(uri)) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Screen.ImageToPdf.route) {
            ImageToPdfScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.MergePdf.route) {
            MergePdfScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.SplitPdf.route) {
            SplitPdfScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.CompressPdf.route) {
            CompressPdfScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.ViewPdf.route) {
            ViewPdfScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.WordToPdf.route) {
            WordToPdfScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.OrganizePages.route) {
            OrganizePagesScreen(
                onBack = { navController.popBackStack() },
                onOpenDocument = { uri ->
                    navController.navigate(Screen.ViewPdfFile.createRoute(uri)) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Screen.WatermarkPdf.route) {
            WatermarkPdfScreen(
                onBack = { navController.popBackStack() },
                onOpenDocument = { uri ->
                    navController.navigate(Screen.ViewPdfFile.createRoute(uri)) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Screen.ExportImages.route) {
            ExportImagesScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.ExportWord.route) {
            ExportWordScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Ocr.route) {
            OcrScreen(
                onBack = { navController.popBackStack() },
                onOpenDocument = { uri ->
                    navController.navigate(Screen.ViewPdfFile.createRoute(uri)) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Screen.UnlockPdf.route) {
            UnlockPdfScreen(
                onBack = { navController.popBackStack() },
                onOpenDocument = { uri ->
                    navController.navigate(Screen.ViewPdfFile.createRoute(uri)) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Screen.ViewPdfFile.route) { backStackEntry ->
            val encoded = backStackEntry.arguments?.getString("uri")
            ViewPdfScreen(
                onBack = { navController.popBackStack() },
                // Navigation Compose already URL-decodes path args, so parse the
                // decoded string directly back into a Uri.
                initialUri = encoded?.let { android.net.Uri.parse(it) }
            )
        }
    }
}