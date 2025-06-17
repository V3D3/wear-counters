/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter to find the
 * most up to date changes to the libraries and their usages.
 */

package io.github.v3d3.counters.presentation

import android.content.Context // Import Context for SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Minimize
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import androidx.wear.tooling.preview.devices.WearDevices
import androidx.compose.ui.platform.LocalContext // Import LocalContext to get the current context

// Define constants for SharedPreferences
private const val PREFS_NAME = "counter_prefs"
private const val COUNT_KEY = "current_count"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        setTheme(android.R.style.Theme_DeviceDefault)

        setContent {
            WearApp()
        }
    }
}

@Composable
fun WearApp() {
    // Get the current Android context within the Composable
    val context = LocalContext.current
    // Get an instance of SharedPreferences
    val sharedPrefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    // Load the initial count from SharedPreferences. If not found, default to 0.
    var currentCount by remember { mutableStateOf(sharedPrefs.getInt(COUNT_KEY, 0)) }

    // Scaffold provides basic Wear OS screen layout with optional TimeText and Vignette
    Scaffold(
        timeText = { TimeText() }, // Displays current time on the watch face
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) } // Adds a subtle vignette effect
    ) {
        // Main content area, centered both horizontally and vertically
        Column(
            modifier = Modifier
                .fillMaxSize() // Fills the available screen space
                .background(Color.DarkGray) // Dark background for the app
                .padding(8.dp), // Padding around the content
            verticalArrangement = Arrangement.Center, // Vertically center content
            horizontalAlignment = Alignment.CenterHorizontally // Horizontally center content
        ) {
            // Display for the current count
            Text(
                text = "$currentCount", // Display the current count
                fontSize = 80.sp, // Large font size for visibility on a small screen
                fontWeight = FontWeight.Bold, // Bold text
                color = Color.White, // White text color
                fontFamily = FontFamily.SansSerif // Sans-serif font
            )

            Spacer(modifier = Modifier.height(16.dp)) // Space between count and buttons

            // Row for Increment and Decrement buttons
            Row(
                modifier = Modifier.fillMaxWidth(), // Fill width for buttons
                horizontalArrangement = Arrangement.SpaceAround, // Distribute space evenly
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Decrement Button
                Button(
                    onClick = {
                        currentCount-- // Decrement action
                        // Save the updated count to SharedPreferences
                        sharedPrefs.edit().putInt(COUNT_KEY, currentCount).apply()
                    },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFE53935)), // Red background
                    modifier = Modifier.size(50.dp) // Fixed size for round Wear OS buttons
                ) {
                    Icon(
                        imageVector = Icons.Default.Minimize, // Minus icon
                        contentDescription = "Decrement",
                        tint = Color.White // White icon
                    )
                }

                // Spacer between buttons (optional, SpaceAround handles much of it)
                Spacer(modifier = Modifier.width(12.dp))

                // Increment Button
                Button(
                    onClick = {
                        currentCount++ // Increment action
                        // Save the updated count to SharedPreferences
                        sharedPrefs.edit().putInt(COUNT_KEY, currentCount).apply()
                    },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF4CAF50)), // Green background
                    modifier = Modifier.size(50.dp) // Fixed size for round Wear OS buttons
                ) {
                    Icon(
                        imageVector = Icons.Default.Add, // Plus icon
                        contentDescription = "Increment",
                        tint = Color.White // White icon
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp)) // Space between button row and reset button

            // Reset Button
            Button(
                onClick = {
                    currentCount = 0 // Reset action
                    // Save the updated count to SharedPreferences
                    sharedPrefs.edit().putInt(COUNT_KEY, currentCount).apply()
                },
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF616161)), // Gray background
                modifier = Modifier.fillMaxWidth(0.6f) // Fills 60% of the width
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh, // Refresh icon
                    contentDescription = "Reset",
                    tint = Color.White // White icon
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Reset", color = Color.White) // Text on the reset button
            }
        }
    }
}

// Preview Composable for different Wear OS devices
@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun WearAppPreviewRound() {
    WearApp()
}

@Preview(device = WearDevices.RECT, showSystemUi = true)
@Composable
fun WearAppPreviewRect() {
    WearApp()
}
