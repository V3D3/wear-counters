package io.github.v3d3.counters

import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.util.Log // For logging to Logcat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable // For long press detection
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove // For deleting the last counter
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
// Removed Gson and TypeToken imports
// import com.google.gson.Gson
// import com.google.gson.reflect.TypeToken
// Removed UUID as it's no longer needed for unique IDs

// --- Data Model (No longer a separate data class, directly use Int) ---
// The Counter data class is removed. We will store List<Int> directly.

// --- MainActivity ---
class MainActivity : ComponentActivity() {

    private val TAG = "MultiCounterApp" // Updated TAG for consistency
    private val PREFS_NAME = "multi_counter_prefs"
    private val COUNTERS_KEY = "counters_list" // This key will now store a delimited string of Ints

    // State holders that MainActivity controls and passes to Composables
    // Now a MutableList<Int>
    private var countersState: MutableState<MutableList<Int>> = mutableStateOf(mutableListOf())
    private var currentPageIndexState: MutableState<Int> = mutableStateOf(0)

    // Removed Gson instance: private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "MainActivity onCreate: Loading counters.")
        loadCounters() // Load counters when activity is created

        setContent {
            WearApp(
                counters = countersState.value, // Pass the current list of Ints
                currentPageIndex = currentPageIndexState.value, // Pass the current page index
                onCountersChanged = { newCounters ->
                    // Callback to update the counters state in MainActivity
                    countersState.value = newCounters.toMutableList()
                    saveCounters() // Save immediately when counters change
                },
                onPageIndexChanged = { newIndex ->
                    // Callback to update the current page index in MainActivity
                    currentPageIndexState.value = newIndex
                },
                onDeleteLastCounter = {
                    val currentList = countersState.value.toMutableList()
                    if (currentList.isNotEmpty()) {
                        currentList.removeLast() // Remove the last counter (Int)
                        countersState.value = currentList
                        saveCounters() // Save after deletion
                        Log.i(TAG, "Last counter deleted. Remaining: ${currentList.size}")

                        // Adjust current page if the deleted item was the currently viewed one
                        if (currentPageIndexState.value >= currentList.size && currentList.isNotEmpty()) {
                            currentPageIndexState.value = currentList.size - 1
                        } else if (currentList.isEmpty()) {
                            // If all counters are deleted, go to the add new counter page (index 0)
                            currentPageIndexState.value = 0
                        }
                    }
                }
            )
        }
    }

    override fun onStop() {
        super.onStop()
        // Save counters when the activity is no longer visible.
        Log.d(TAG, "MainActivity onStop: Saving counters.")
        saveCounters()
    }

    // Load counters from SharedPreferences using a delimited string
    private fun loadCounters() {
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        // Get the string, default to empty string if not found
        val countersString = sharedPrefs.getString(COUNTERS_KEY, "")

        // Parse the string back into a MutableList<Int>
        val loadedList = if (countersString.isNullOrEmpty()) {
            mutableListOf() // Start with an empty list if no string is found
        } else {
            // Split by comma, trim whitespace, convert to Int, filter out any non-numeric results
            countersString.split(",").mapNotNull { it.trim().toIntOrNull() }.toMutableList()
        }

        // If no counters are loaded (either empty string or parsing failed), initialize with a default one (an Int with value 0)
        countersState.value = if (loadedList.isEmpty()) mutableListOf(0) else loadedList
        Log.d(TAG, "Counters loaded: ${countersState.value.size} items from string: '$countersString'.")
    }

    // Save counters to SharedPreferences as a delimited string
    private fun saveCounters() {
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        // Convert the List<Int> to a comma-separated string
        val countersString = countersState.value.joinToString(separator = ",")
        sharedPrefs.edit().putString(COUNTERS_KEY, countersString).apply()
        Log.d(TAG, "Counters saved as string: '$countersString'.")
    }

    /**
     * Handles physical button presses.
     * KeyEvent.KEYCODE_STEM_1 for incrementing, KeyEvent.KEYCODE_STEM_2 for decrementing.
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        Log.d(TAG, "onKeyDown: KeyCode = $keyCode")

        // Get the current list of counters (Ints) and the current page index
        val currentCounters = countersState.value
        val currentPage = currentPageIndexState.value

        // Check if we are on a valid counter page (not the "add new counter" page)
        // And if there are any counters to modify
        if (currentPage < currentCounters.size && currentCounters.isNotEmpty()) {
            val currentCount = currentCounters[currentPage] // Get the Int count directly
            var handled = false
            var newCountValue: Int = currentCount

            when (keyCode) {
                // STEM 1 (often the top physical button) for Increment
                KeyEvent.KEYCODE_STEM_1 -> {
                    newCountValue = currentCount + 1
                    Log.i(TAG, "Counter at index $currentPage incremented to $newCountValue")
                    handled = true
                }
                // STEM 2 (often the bottom physical button) for Decrement
                KeyEvent.KEYCODE_STEM_2 -> {
                    newCountValue = currentCount - 1
                    Log.i(TAG, "Counter at index $currentPage decremented to $newCountValue")
                    handled = true
                }
            }

            if (handled) {
                // Update the state in MainActivity directly for immediate UI feedback
                val updatedList = currentCounters.toMutableList()
                updatedList[currentPage] = newCountValue // Update the Int at the specific index
                countersState.value = updatedList

                saveCounters() // Save after each physical button press that modifies data
                return true // Event handled by our logic
            }
        }

        // Let the system handle other key events if not specifically handled by our logic
        return super.onKeyDown(keyCode, event)
    }
}

// --- Composable Functions ---

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WearApp(
    counters: List<Int>, // Receives the current list of Ints
    currentPageIndex: Int, // Receives the current page index
    onCountersChanged: (List<Int>) -> Unit, // Callback to request counter list updates (List<Int>)
    onPageIndexChanged: (Int) -> Unit, // Callback to request page index updates
    onDeleteLastCounter: () -> Unit // Callback to delete the last counter
) {
    // Remember the pager state based on the provided current page index
    val pagerState = rememberPagerState(initialPage = currentPageIndex) {
        // Number of pages will be the number of counters + 1 (for the add new counter screen)
        counters.size + 1
    }

    // Effect to keep currentPageIndexState in MainActivity in sync with pagerState
    LaunchedEffect(pagerState.currentPage) {
        if (currentPageIndex != pagerState.currentPage) {
            onPageIndexChanged(pagerState.currentPage)
        }
    }

    // Effect to adjust pager position if counters list changes (e.g., a counter is deleted/added)
    LaunchedEffect(counters.size) {
        // Ensure the current page index is valid after a list change
        val newPageIndex = pagerState.currentPage.coerceIn(0, (counters.size).coerceAtLeast(0))
        if (newPageIndex != pagerState.currentPage) {
            pagerState.animateScrollToPage(newPageIndex)
        }
        // If we were on the last page (Add New Counter) and all counters were deleted,
        // ensure we stay on the Add New Counter page (which is page 0 if no other counters exist).
        if (counters.isEmpty() && pagerState.currentPage != 0) {
            pagerState.animateScrollToPage(0)
        }
    }


    Scaffold(
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) }
    ) {
        HorizontalPager(state = pagerState) { page ->
            if (page < counters.size) {
                // Display individual counter page
                val count = counters[page] // Get the Int count directly
                CounterPage(
                    count = count, // Pass the Int count
                    onReset = {
                        val updatedList = counters.toMutableList()
                        // Reset by index directly
                        updatedList[page] = 0 // Set count at this index to 0
                        onCountersChanged(updatedList) // Request update via callback
                    }
                )
            } else {
                // Display "Add New Counter" page
                AddCounterPage(
                    hasCounters = counters.isNotEmpty(), // Pass if any counters exist
                    onAddCounter = {
                        // Create a new Counter (Int) instance
                        val newCounterValue = 0 // New counters start at 0
                        val updatedList = counters.toMutableList().apply { add(newCounterValue) }
                        onCountersChanged(updatedList) // Request update via callback
                    },
                    onDeleteLastCounter = onDeleteLastCounter // Pass the callback from MainActivity
                )
            }
        }
    }
}

/**
 * Composable for displaying a single counter.
 * Reset is now done via long press on the counter itself.
 * Increment/Decrement are handled by physical buttons.
 * Counter name and Delete button are removed.
 */
@OptIn(ExperimentalFoundationApi::class) // Required for combinedClickable
@Composable
fun CounterPage(
    count: Int, // Now directly receives an Int
    onReset: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.DarkGray)
            .padding(8.dp)
            // Add combinedClickable for long press to reset
            .combinedClickable(
                onClick = { /* No action on short click, as increment/decrement are physical buttons */ },
                onLongClick = onReset
            ),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Count Display
        Text(
            text = "$count", // Display the Int count directly
            fontSize = 80.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontFamily = FontFamily.SansSerif,
            textAlign = TextAlign.Center, // Ensure text is centered
            modifier = Modifier.fillMaxWidth() // Make sure the text occupies enough width to be a good touch target
        )
    }
}

/**
 * Composable for the "Add New Counter" screen.
 * Now includes a button to delete the last counter.
 * No longer prompts for counter names.
 */
@Composable
fun AddCounterPage(
    hasCounters: Boolean, // Indicates if there are any counters to delete
    onAddCounter: () -> Unit, // Changed to no longer take a name parameter or Counter object
    onDeleteLastCounter: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.DarkGray)
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // "Add New Counter" Button (Plus Icon)
        Button(
            onClick = onAddCounter, // Call the simplified onAddCounter
            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF007AFF)), // Bright blue for add
            modifier = Modifier
                .fillMaxWidth(0.8f) // Fill most of the width
                .aspectRatio(1f) // Make it square
                .padding(bottom = 8.dp) // Add padding below the add button
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add New Counter", modifier = Modifier.size(80.dp), tint = Color.White) // Large plus icon
        }
        Text(
            text = "Add New Counter",
            fontSize = 18.sp,
            color = Color.White,
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
        )

        // "Delete Last Counter" Button (Minus Icon)
        Button(
            onClick = onDeleteLastCounter,
            // Enable only if there are counters to delete
            enabled = hasCounters,
            colors = ButtonDefaults.buttonColors(
                backgroundColor = if (hasCounters) Color(0xFFD32F2F) else Color(0x88D32F2F) // Darker red, dimmed if disabled
            ),
            modifier = Modifier
                .fillMaxWidth(0.6f) // Smaller than add button
                .aspectRatio(1f) // Make it square
        ) {
            Icon(Icons.Default.Remove, contentDescription = "Delete Last Counter", modifier = Modifier.size(40.dp), tint = Color.White) // Minus icon
        }
        Text(
            text = "Delete Last",
            fontSize = 16.sp,
            color = if (hasCounters) Color.White else Color.Gray,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

// --- Preview Composables ---
@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun WearAppPreviewRound() {
    // Provide dummy data for preview (List<Int>)
    WearApp(
        counters = mutableStateListOf(42), // No names, just a count
        currentPageIndex = 0,
        onCountersChanged = {},
        onPageIndexChanged = {},
        onDeleteLastCounter = {}
    )
}

@Preview(device = WearDevices.RECT, showSystemUi = true)
@Composable
fun WearAppPreviewRect() {
    // Provide dummy data for preview (List<Int>)
    WearApp(
        counters = mutableStateListOf(42), // No names, just a count
        currentPageIndex = 0,
        onPageIndexChanged = {},
        onCountersChanged = {},
        onDeleteLastCounter = {}
    )
}
