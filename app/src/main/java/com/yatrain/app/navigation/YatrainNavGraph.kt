package com.yatrain.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.yatrain.app.ui.screens.home.MainScreen

sealed class Screen(val route: String) {
    object Main : Screen("main")
}

@Composable
fun YatrainNavGraph(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController    = navController,
        startDestination = Screen.Main.route
    ) {
        composable(Screen.Main.route) {
            // App always starts here; auth lives inside the Profile tab
            MainScreen()
        }
    }
}
