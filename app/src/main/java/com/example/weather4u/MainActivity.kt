package com.example.weather4u

import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.weather4u.ui.theme.Weather4uTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Weather4uTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                   MyAppNavigation()
                }

            }
        }
    }
}


@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MyAppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "screenA"
    ) {
        composable(
            "screenA",
            enterTransition = { slideInHorizontally(initialOffsetX = { 1000 }) + fadeIn(animationSpec = tween(durationMillis = 200)) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -1000 }) + fadeOut(animationSpec = tween(durationMillis = 200)) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -1000 }) + fadeIn(animationSpec = tween(durationMillis = 200)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { 1000 }) + fadeOut(animationSpec = tween(durationMillis = 200)) }
        ) {
            ScreenA(navController)
        }

        composable(
            "screenB",
            enterTransition = { slideInHorizontally(initialOffsetX = { -1000 }) + fadeIn() },
            exitTransition = { slideOutHorizontally(targetOffsetX = { 1000 }) + fadeOut() },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { 1000 }) + fadeIn() },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { -1000 }) + fadeOut() }
        ) {
            ScreenB(navController)
        }
    }
}

@Composable
fun ScreenA(navController: NavController) {
    // Your screen content here
    Button(onClick = { navController.navigate("screenB") }) {
        Text("Go to Screen B")
    }
}

@Composable
fun ScreenB(navController: NavController) {
    // Your screen content here
    Button(onClick = { navController.popBackStack() }) {
        Text("Go back to Screen A")
    }
}

