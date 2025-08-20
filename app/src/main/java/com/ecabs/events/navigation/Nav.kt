package com.ecabs.events.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ecabs.events.data.model.GitHubEvent
import com.ecabs.events.ui.EventDetailsScreen
import com.ecabs.events.ui.EventsScreen
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    val adapter = moshi.adapter(GitHubEvent::class.java)
    NavHost(navController = navController, startDestination = "list") {
        composable("list") {
            EventsScreen(onEventClick = { event ->
                val json = adapter.toJson(event)
                val encoded = java.net.URLEncoder.encode(json, "utf-8")
                navController.navigate("detail/$encoded")
            })
        }
        composable(
            route = "detail/{eventJson}",
            arguments = listOf(navArgument("eventJson") { type = NavType.StringType })
        ) { backStackEntry ->
            val encoded = backStackEntry.arguments?.getString("eventJson").orEmpty()
            val json = java.net.URLDecoder.decode(encoded, "utf-8")
            val event = runCatching { adapter.fromJson(json) }.getOrNull()
            EventDetailsScreen(event = event, onBack = { navController.popBackStack() })
        }
    }
}