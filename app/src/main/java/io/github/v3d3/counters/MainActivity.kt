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
import androidx.compose.runtime.snapshots.SnapshotStateList // Explicit import for clarity
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

// --- MainActivity ---
class MainActivity : ComponentActivity() {

    private val TAG = "MultiCounterApp"
    private val PREFS_NAME = "multi_counter_prefs"
    private val COUNTERS_KEY = "counters_list" // This key stores a delimited string of Ints

    // OPTIMIZATION: Use mutableStateListOf for more efficient list state management in Compose.
    // This allows Compose to track changes to individual elements within the list,
    // potentially leading to more granular recompositions.
    private val countersState: SnapshotStateList<Int> = mutableStateListOf()
    private var currentPageIndexState: MutableState<Int> = mutableStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "MainActivity onCreate: Loading counters.")
        loadCounters() // Load counters when activity is created

        setContent {
            WearApp(
                counters = countersState, // Pass the SnapshotStateList directly
                currentPageIndex = currentPageIndexState.value,
                onCountersChanged = { newCounters ->
                    // OPTIMIZATION: Update SnapshotStateList directly for efficiency.
                    // Clear existing and add all new elements.
                    countersState.clear()
                    countersState.addAll(newCounters)
                    saveCounters() // Save immediately when counters change
                },
                onPageIndexChanged = { newIndex ->
                    currentPageIndexState.value = newIndex
                },
                onDeleteLastCounter = {
                    // OPTIMIZATION: Directly modify the SnapshotStateList
                    if (countersState.isNotEmpty()) {
                        countersState.removeLast() // Remove the last counter (Int)
                        saveCounters() // Save after deletion
                        Log.i(TAG, "Last counter deleted. Remaining: ${countersState.size}")

                        // Adjust current page if the deleted item was the currently viewed one
                        if (currentPageIndexState.value >= countersState.size && countersState.isNotEmpty()) {
                            currentPageIndexState.value = countersState.size - 1
                        } else if (countersState.isEmpty()) {
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
        // This ensures data is saved even if the app process is killed.
        Log.d(TAG, "MainActivity onStop: Saving counters.")
        saveCounters()
    }

    // Load counters from SharedPreferences using a delimited string
    private fun loadCounters() {
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val countersString = sharedPrefs.getString(COUNTERS_KEY, "")

        val loadedList = if (countersString.isNullOrEmpty()) {
            mutableListOf() // Start with an empty list if no string is found
        } else {
            countersString.split(",").mapNotNull { it.trim().toIntOrNull() }.toMutableList()
        }

        // OPTIMIZATION: Initialize SnapshotStateList directly.
        // If no counters are loaded, initialize with a default one (an Int with value 0)
        countersState.clear()
        countersState.addAll(if (loadedList.isEmpty()) listOf(0) else loadedList)
        Log.d(TAG, "Counters loaded: ${countersState.size} items from string: '$countersString'.")
    }

    // Save counters to SharedPreferences as a delimited string
    private fun saveCounters() {
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        // Convert the List<Int> to a comma-separated string
        val countersString = countersState.joinToString(separator = ",")
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
        // OPTIMIZATION: Work directly with the observable list.
        val currentCounters = countersState
        val currentPage = currentPageIndexState.value

        // Check if we are on a valid counter page (not the "add new counter" page)
        if (currentPage < currentCounters.size && currentCounters.isNotEmpty()) {
            var handled = false
            val currentCount = currentCounters[currentPage] // Get the Int count directly
            var newCountValue: Int = currentCount

            when (keyCode) {
                KeyEvent.KEYCODE_STEM_1 -> { // Increment
                    newCountValue = currentCount + 1
                    Log.i(TAG, "Counter at index $currentPage incremented to $newCountValue")
                    handled = true
                }
                KeyEvent.KEYCODE_STEM_2 -> { // Decrement
                    newCountValue = currentCount - 1
                    Log.i(TAG, "Counter at index $currentPage decremented to $newCountValue")
                    handled = true
                }
            }

            if (handled) {
                // OPTIMIZATION: Directly update the element in the SnapshotStateList.
                // This triggers more efficient recomposition for only the affected item.
                countersState[currentPage] = newCountValue

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
    counters: List<Int>, // Receives the current list of Ints (can be SnapshotStateList too)
    currentPageIndex: Int,
    onCountersChanged: (List<Int>) -> Unit,
    onPageIndexChanged: (Int) -> Unit,
    onDeleteLastCounter: () -> Unit
) {
    val pagerState = rememberPagerState(initialPage = currentPageIndex) {
        counters.size + 1
    }

    LaunchedEffect(pagerState.currentPage) {
        if (currentPageIndex != pagerState.currentPage) {
            onPageIndexChanged(pagerState.currentPage)
        }
    }

    LaunchedEffect(counters.size) {
        val newPageIndex = pagerState.currentPage.coerceIn(0, (counters.size).coerceAtLeast(0))
        if (newPageIndex != pagerState.currentPage) {
            pagerState.animateScrollToPage(newPageIndex)
        }
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
                val count = counters[page]
                CounterPage(
                    count = count,
                    onReset = {
                        // OPTIMIZATION: Create a copy of the list and modify the specific element,
                        // then pass it to onCountersChanged to update the main state list efficiently.
                        val updatedList = counters.toMutableList()
                        updatedList[page] = 0 // Set count at this index to 0
                        onCountersChanged(updatedList) // Request update via callback
                    }
                )
            } else {
                AddCounterPage(
                    hasCounters = counters.isNotEmpty(),
                    onAddCounter = {
                        // OPTIMIZATION: Add new item to a copy and update the main state list.
                        val newCounterValue = 0
                        val updatedList = counters.toMutableList().apply { add(newCounterValue) }
                        onCountersChanged(updatedList)
                    },
                    onDeleteLastCounter = onDeleteLastCounter
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CounterPage(
    count: Int,
    onReset: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            // Change background to pitch black
            .background(Color.Black)
            .padding(8.dp)
            .combinedClickable(
                onClick = { /* No action on short click, as increment/decrement are physical buttons */ },
                onLongClick = onReset
            ),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "$count",
            fontSize = 80.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            // Change font to Monospace for rendering efficiency and digital aesthetic
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun AddCounterPage(
    hasCounters: Boolean,
    onAddCounter: () -> Unit,
    onDeleteLastCounter: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            // Change background to pitch black
            .background(Color.Black)
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = onAddCounter,
            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF007AFF)),
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .aspectRatio(1f)
                .padding(bottom = 8.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add New Counter", modifier = Modifier.size(80.dp), tint = Color.White)
        }
        Text(
            text = "Add New Counter",
            fontSize = 18.sp,
            color = Color.White,
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
        )

        Button(
            onClick = onDeleteLastCounter,
            enabled = hasCounters,
            colors = ButtonDefaults.buttonColors(
                backgroundColor = if (hasCounters) Color(0xFFD32F2F) else Color(0x88D32F2F)
            ),
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .aspectRatio(1f)
        ) {
            Icon(Icons.Default.Remove, contentDescription = "Delete Last Counter", modifier = Modifier.size(40.dp), tint = Color.White)
        }
        Text(
            text = "Delete Last",
            fontSize = 16.sp,
            color = if (hasCounters) Color.White else Color.Gray,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun WearAppPreviewRound() {
    WearApp(
        counters = mutableStateListOf(42),
        currentPageIndex = 0,
        onCountersChanged = {},
        onPageIndexChanged = {},
        onDeleteLastCounter = {}
    )
}

@Preview(device = WearDevices.RECT, showSystemUi = true)
@Composable
fun WearAppPreviewRect() {
    WearApp(
        counters = mutableStateListOf(42),
        currentPageIndex = 0,
        onPageIndexChanged = {},
        onCountersChanged = {},
        onDeleteLastCounter = {}
    )
}
