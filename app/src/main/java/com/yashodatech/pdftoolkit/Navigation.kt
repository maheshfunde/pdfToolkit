package com.yashodatech.pdftoolkit

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.yashodatech.pdftoolkit.data.PreferencesManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

object NavUriHelper {
    fun encode(uri: String): String {
        return android.util.Base64.encodeToString(
            uri.toByteArray(Charsets.UTF_8),
            android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP
        )
    }

    fun decode(encoded: String?): Uri? {
        if (encoded.isNullOrBlank()) return null
        return try {
            val decodedStr = String(
                android.util.Base64.decode(
                    encoded,
                    android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP
                ),
                Charsets.UTF_8
            )
            Uri.parse(decodedStr)
        } catch (_: Exception) {
            Uri.parse(encoded)
        }
    }
}

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Home : Screen("home")
    object ImageToPdf : Screen("image_to_pdf")
    object MergePdf : Screen("merge_pdf")
    object SplitPdf : Screen("split_pdf?uri={uri}") {
        fun createRoute(uri: String? = null): String =
            if (uri != null) "split_pdf?uri=${NavUriHelper.encode(uri)}" else "split_pdf"
    }
    object CompressPdf : Screen("compress_pdf?uri={uri}") {
        fun createRoute(uri: String? = null): String =
            if (uri != null) "compress_pdf?uri=${NavUriHelper.encode(uri)}" else "compress_pdf"
    }
    object ViewPdf : Screen("view_pdf")
    object Settings : Screen("settings")

    object WordToPdf : Screen("word_to_pdf")

    object OrganizePages : Screen("organize_pages?uri={uri}") {
        fun createRoute(uri: String? = null): String =
            if (uri != null) "organize_pages?uri=${NavUriHelper.encode(uri)}" else "organize_pages"
    }
    object WatermarkPdf : Screen("watermark_pdf?uri={uri}") {
        fun createRoute(uri: String? = null): String =
            if (uri != null) "watermark_pdf?uri=${NavUriHelper.encode(uri)}" else "watermark_pdf"
    }
    object ExportImages : Screen("export_images")
    object ExportWord : Screen("export_word")
    object Ocr : Screen("ocr")
    object UnlockPdf : Screen("unlock_pdf")

    // Opens a specific PDF file in the in-app viewer.
    object ViewPdfFile : Screen("view_pdf_file/{uri}") {
        fun createRoute(uri: String): String =
            "view_pdf_file/${NavUriHelper.encode(uri)}"
    }
}

@Composable
fun AppNavigation(
    initialRoute: String? = null,
    externalPdfUri: Uri? = null,
    onPdfUriConsumed: () -> Unit = {}
) {

    val context = LocalContext.current
    val prefsManager = remember { PreferencesManager(context) }
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    val hasActivePdf = externalPdfUri != null || com.yashodatech.pdftoolkit.pdf.ActivePdfHolder.activeUri != null

    // Determine initial destination synchronously if external PDF or initial route provided
    var startDestination by remember {
        mutableStateOf(
            if (hasActivePdf) {
                Screen.ViewPdf.route
            } else if (!initialRoute.isNullOrBlank()) {
                initialRoute
            } else {
                null
            }
        )
    }

    // Keep track of whether the initial external PDF URI was already used as startDestination
    val initialExternalHandled = remember { mutableStateOf(hasActivePdf) }

    LaunchedEffect(Unit) {
        if (hasActivePdf) {
            onPdfUriConsumed()
        } else if (startDestination == null) {
            val completed = prefsManager.onboardingCompleted.first()
            startDestination = if (completed) Screen.Home.route
            else Screen.Onboarding.route
        }

        prefsManager.incrementAppOpenCount()
    }

    // Handle new intent / external URI while app is already running (warm start)
    LaunchedEffect(externalPdfUri) {
        val uri = externalPdfUri ?: return@LaunchedEffect
        com.yashodatech.pdftoolkit.pdf.ActivePdfHolder.set(uri)
        if (initialExternalHandled.value) {
            // Already configured as startDestination on cold start
            initialExternalHandled.value = false
            return@LaunchedEffect
        }
        val hasGraph = runCatching { navController.graph }.isSuccess
        if (hasGraph) {
            try {
                navController.navigate(Screen.ViewPdf.route) {
                    launchSingleTop = true
                }
            } catch (e: Exception) {
                android.util.Log.e("Navigation", "Failed to navigate to external PDF", e)
            } finally {
                onPdfUriConsumed()
            }
        }
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

        composable(
            route = "split_pdf?uri={uri}",
            arguments = listOf(navArgument("uri") { nullable = true; defaultValue = null })
        ) { backStackEntry ->
            val uriArg = backStackEntry.arguments?.getString("uri")
            SplitPdfScreen(
                onBack = { navController.popBackStack() },
                initialUri = NavUriHelper.decode(uriArg)
            )
        }

        composable(
            route = "compress_pdf?uri={uri}",
            arguments = listOf(navArgument("uri") { nullable = true; defaultValue = null })
        ) { backStackEntry ->
            val uriArg = backStackEntry.arguments?.getString("uri")
            CompressPdfScreen(
                onBack = { navController.popBackStack() },
                initialUri = NavUriHelper.decode(uriArg)
            )
        }

        composable(Screen.ViewPdf.route) {
            val initialUri = remember { com.yashodatech.pdftoolkit.pdf.ActivePdfHolder.consume() }
            ViewPdfScreen(
                onBack = {
                    com.yashodatech.pdftoolkit.pdf.ActivePdfHolder.clear()
                    if (!navController.popBackStack()) {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                },
                initialUri = initialUri,
                onNavigateToTool = { route ->
                    navController.navigate(route) { launchSingleTop = true }
                }
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

        composable(
            route = "organize_pages?uri={uri}",
            arguments = listOf(navArgument("uri") { nullable = true; defaultValue = null })
        ) { backStackEntry ->
            val uriArg = backStackEntry.arguments?.getString("uri")
            OrganizePagesScreen(
                onBack = { navController.popBackStack() },
                onOpenDocument = { uri ->
                    navController.navigate(Screen.ViewPdfFile.createRoute(uri)) {
                        launchSingleTop = true
                    }
                },
                initialUri = NavUriHelper.decode(uriArg)
            )
        }

        composable(
            route = "watermark_pdf?uri={uri}",
            arguments = listOf(navArgument("uri") { nullable = true; defaultValue = null })
        ) { backStackEntry ->
            val uriArg = backStackEntry.arguments?.getString("uri")
            WatermarkPdfScreen(
                onBack = { navController.popBackStack() },
                onOpenDocument = { uri ->
                    navController.navigate(Screen.ViewPdfFile.createRoute(uri)) {
                        launchSingleTop = true
                    }
                },
                initialUri = NavUriHelper.decode(uriArg)
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
            val parsedUri = NavUriHelper.decode(encoded)
            ViewPdfScreen(
                onBack = {
                    if (!navController.popBackStack()) {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                },
                initialUri = parsedUri,
                onNavigateToTool = { route ->
                    navController.navigate(route) { launchSingleTop = true }
                }
            )
        }
    }
}