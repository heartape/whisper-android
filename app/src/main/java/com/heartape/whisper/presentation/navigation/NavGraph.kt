package com.heartape.whisper.presentation.navigation

import androidx.compose.animation.*
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.heartape.whisper.presentation.screens.*

@Composable
fun AppNavHost(navController: NavHostController, startDestination: String = "login") {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { slideInHorizontally(initialOffsetX = { 1000 }) + fadeIn() },
        exitTransition = { slideOutHorizontally(targetOffsetX = { -1000 }) + fadeOut() },
        popEnterTransition = { slideInHorizontally(initialOffsetX = { -1000 }) + fadeIn() },
        popExitTransition = { slideOutHorizontally(targetOffsetX = { 1000 }) + fadeOut() }
    ) {
        composable("login") {
            LoginScreen(
                onLoginSuccess = { navController.navigate("main") { popUpTo("login") { inclusive = true } } },
                onNavigateRegister = { navController.navigate("register") }
            )
        }
        composable("register") {
            RegisterScreen(onRegisterSuccess = { navController.popBackStack() }, onBack = { navController.popBackStack() })
        }
        composable("main") {
            MainScreen(
                onNavigateAddFriend = { navController.navigate("add_friend") },
                onNavigateCreateGroup = { navController.navigate("create_group") },
                onNavigateJoinGroup = { navController.navigate("join_group") },
                onNavigateChat = { id, type, name -> navController.navigate("chat/$id/$type/$name") },
                onNavigateSettings = { navController.navigate("settings") },
                onNavigatePendingApplies = { navController.navigate("pending_applies") }
            )
        }
        composable("pending_applies") {
            PendingAppliesScreen(onBack = { navController.popBackStack() })
        }
        composable("chat/{sessionId}/{sessionType}/{sessionName}") { backStackEntry ->
            ChatScreen(
                onBack = { navController.popBackStack() },
                onSettings = {
                    val sid = backStackEntry.arguments?.getString("sessionId")
                    navController.navigate("session_settings/$sid")
                }
            )
        }
        composable("session_settings/{sessionId}") { backStackEntry ->
            val sid = backStackEntry.arguments?.getString("sessionId") ?: "0"
            SessionSettingsScreen(
                onBack = { navController.popBackStack() },
                onNavigateAnnouncement = { navController.navigate("announcement/$sid") }
            )
        }
        composable("announcement/{sessionId}") {
            AnnouncementScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable("settings") {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onNavigateProfile = { navController.navigate("profile_settings") },
                onNavigateSecurity = { navController.navigate("security_settings") },
                onNavigateGeneral = { navController.navigate("general_settings") },
                onNavigateAbout = { navController.navigate("about_settings") },
                onNavigateTerms = { navController.navigate("terms_of_service") },
                onNavigatePrivacy = { navController.navigate("privacy_policy") },
                onLogout = { navController.navigate("login") { popUpTo("main") { inclusive = true } } }
            )
        }

        composable("profile_settings") {
            ProfileSettingsScreen(onBack = { navController.popBackStack() })
        }

        composable("security_settings") {
            SecuritySettingsScreen(onBack = { navController.popBackStack() })
        }

        composable("general_settings") {
            GeneralSettingsScreen(onBack = { navController.popBackStack() })
        }

        composable("about_settings") {
            AboutScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable("terms_of_service") {
            TermsOfServiceScreen(onBack = { navController.popBackStack() })
        }

        composable("privacy_policy") {
            PrivacyPolicyScreen(onBack = { navController.popBackStack() })
        }

        composable("create_group") {
            CreateGroupScreen(
                onBack = { navController.popBackStack() },
                onSuccess = { navController.popBackStack() }
            )
        }

        composable("add_friend") {
            AddFriendScreen(onBack = { navController.popBackStack() })
        }

        composable("join_group") {
            SearchGroupScreen(onBack = { navController.popBackStack() })
        }
    }
}