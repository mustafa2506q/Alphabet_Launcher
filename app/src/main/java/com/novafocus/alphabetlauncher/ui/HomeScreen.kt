package com.novafocus.alphabetlauncher.ui

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.novafocus.alphabetlauncher.data.AppInfo
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val SWIPE_THRESHOLD = 25f

// Top-level screen: normal home (clock + favourites, or letter-filtered
// list while dragging) plus a full-screen search you get to by swiping up.
@Composable
fun LauncherScreen(
    state: LauncherUiState,
    onLetterSelected: (Char?) -> Unit,
    onPressChanged: (Boolean) -> Unit,
    onLaunchApp: (AppInfo) -> Unit,
    onToggleFavourite: (AppInfo) -> Unit,
    recentApps: List<AppInfo>,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var touchYPx by remember { mutableStateOf<Float?>(null) }
    var isSearchOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(state.selectedLetter) {
        if (state.selectedLetter != null) triggerHapticTick(context)
    }

    val background = if (isDarkTheme) {
        Modifier.background(Color.Black)
    } else {
        Modifier.background(Brush.verticalGradient(listOf(Color(0xFFEFEFF7), Color(0xFFDCE0EC))))
    }

    Box(modifier = modifier.fillMaxSize().then(background)) {
        if (isSearchOpen) {
            SearchScreen(
                searchQuery = searchQuery,
                onSearchQueryChanged = { searchQuery = it },
                recentApps = recentApps,
                allApps = state.allApps,
                onLaunchApp = onLaunchApp,
                onCloseSearch = { isSearchOpen = false; searchQuery = "" },
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { _, dragAmount ->
                            if (dragAmount > SWIPE_THRESHOLD) { // swipe down closes it
                                isSearchOpen = false
                                searchQuery = ""
                            }
                        }
                    },
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { _, dragAmount ->
                            if (dragAmount < -SWIPE_THRESHOLD) { // swipe up opens it
                                isSearchOpen = true
                                searchQuery = ""
                            }
                        }
                    },
            ) {
                AnimatedContent(
                    targetState = state.isDragging,
                    label = "homeVsLetterContent",
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                ) { dragging ->
                    val content = Modifier.padding(end = 48.dp)
                    if (dragging) {
                        LetterFilteredContent(state.selectedLetter, state.appsForSelectedLetter, onLaunchApp, onToggleFavourite, content)
                    } else {
                        RestingHomeContent(state.favourites, onLaunchApp, onToggleFavourite, content)
                    }
                }
            }
        }

        // Alphabet bar stays outside the swipe-detecting box above, so its
        // own vertical dragging doesn't get eaten by the swipe gesture.
        if (!isSearchOpen) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(top = 72.dp, bottom = 20.dp),
            ) {
                AlphabetBar(
                    onLetterSelected = onLetterSelected,
                    onPressChanged = onPressChanged,
                    onTouchYChanged = { touchYPx = it },
                    lettersWithApps = state.letterHasApps,
                    modifier = Modifier.align(Alignment.CenterEnd),
                )

                if (state.isDragging && state.selectedLetter != null && touchYPx != null) {
                    val density = LocalDensity.current
                    val bubbleY = with(density) { (touchYPx!! - 28.dp.toPx()).toDp() }
                    LetterBubble(
                        letter = state.selectedLetter,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(end = 56.dp, top = bubbleY),
                    )
                }
            }
        }

        if (!isSearchOpen) {
            ThemeToggleButton(
                isDarkTheme = isDarkTheme,
                onToggle = onToggleTheme,
                modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().padding(16.dp),
            )
        }
    }
}

// Full-screen search: shows Recent apps by default, filters allApps once
// the user starts typing.
@Composable
private fun SearchScreen(
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    recentApps: List<AppInfo>,
    allApps: List<AppInfo>,
    onLaunchApp: (AppInfo) -> Unit,
    onCloseSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val filteredApps = if (searchQuery.isBlank()) {
        allApps
    } else {
        allApps.filter { it.label.contains(searchQuery, ignoreCase = true) }
    }

    Column(
        modifier = modifier
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 12.dp),
    ) {
        SearchBar(query = searchQuery, onQueryChanged = onSearchQueryChanged, onClose = onCloseSearch)

        SectionLabel(if (searchQuery.isNotBlank()) "Apps" else "Recent")

        val listToShow = if (searchQuery.isNotBlank()) filteredApps else recentApps
        when {
            searchQuery.isNotBlank() && filteredApps.isEmpty() -> EmptyState("No apps found")
            searchQuery.isBlank() && recentApps.isEmpty() -> EmptyState("Your recent apps will appear here")
            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(listToShow, key = { it.packageName }) { app ->
                    AppRow(app = app, onClick = { onLaunchApp(app) }, iconSize = 48.dp, textSize = 15.sp)
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(top = 28.dp, bottom = 8.dp),
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
    )
}

@Composable
private fun SearchBar(query: String, onQueryChanged: (String) -> Unit, onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.ArrowBack,
            contentDescription = "Close search",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp).clickable(onClick = onClose),
        )
        Icon(
            Icons.Filled.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(start = 10.dp).size(21.dp),
        )
        Box(modifier = Modifier.weight(1f).padding(horizontal = 10.dp)) {
            if (query.isEmpty()) {
                Text(
                    text = "Search apps...",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                    fontSize = 16.sp,
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChanged,
                singleLine = true,
                textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (query.isNotEmpty()) {
            Icon(
                Icons.Filled.Clear,
                contentDescription = "Clear search",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp).clickable { onQueryChanged("") },
            )
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = message, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f), fontSize = 14.sp)
    }
}

@Composable
private fun ThemeToggleButton(isDarkTheme: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.size(44.dp).clickable(onClick = onToggle),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (isDarkTheme) Icons.Filled.DarkMode else Icons.Filled.LightMode,
            contentDescription = if (isDarkTheme) "Switch to light theme" else "Switch to dark theme",
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.size(22.dp),
        )
    }
}

// Clock, date, and a short favourites list - the default screen when nothing is pressed.
@Composable
private fun RestingHomeContent(
    favourites: List<AppInfo>,
    onLaunchApp: (AppInfo) -> Unit,
    onToggleFavourite: (AppInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = Date()
            delay(1_000)
        }
    }
    val timeText = remember(now) { SimpleDateFormat("HH:mm", Locale.getDefault()).format(now) }
    val dateText = remember(now) { SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(now) }

    Column(modifier = modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text(timeText, fontSize = 56.sp, fontWeight = FontWeight.Light, color = MaterialTheme.colorScheme.onBackground)
        Text(dateText, fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
        Column(modifier = Modifier.padding(top = 32.dp)) {
            favourites.forEach { app ->
                AppRow(app = app, onClick = { onLaunchApp(app) }, onLongPress = { onToggleFavourite(app) })
            }
        }
    }
}

// Letter header + matching apps, shown while dragging on the alphabet bar.
@Composable
private fun LetterFilteredContent(
    letter: Char?,
    apps: List<AppInfo>,
    onLaunchApp: (AppInfo) -> Unit,
    onToggleFavourite: (AppInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().padding(24.dp)) {
        Text(letter?.toString().orEmpty(), fontSize = 40.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        if (apps.isEmpty()) {
            EmptyState("No apps")
        } else {
            LazyColumn(modifier = Modifier.padding(top = 12.dp)) {
                items(apps, key = { it.packageName }) { app ->
                    AppRow(app = app, onClick = { onLaunchApp(app) }, onLongPress = { onToggleFavourite(app) })
                }
            }
        }
    }
}

// One row: app icon + name. Long-press toggles it as a favourite.
// Shared by the home list, letter list, and search results.
@Composable
private fun AppRow(
    app: AppInfo,
    onClick: () -> Unit,
    onLongPress: (() -> Unit)? = null,
    iconSize: Dp = 46.dp,
    textSize: TextUnit = 14.sp,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(0.82f)
            .pointerInput(app.packageName) {
                detectTapGestures(onTap = { onClick() }, onLongPress = { onLongPress?.invoke() })
            }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val bitmap = remember(app.packageName) { app.icon.toBitmap(width = 128, height = 128) }
        Image(
            painter = BitmapPainter(bitmap.asImageBitmap()),
            contentDescription = app.label,
            modifier = Modifier.size(iconSize).clip(CircleShape),
        )
        Text(
            text = app.label,
            modifier = Modifier.padding(start = 12.dp),
            fontSize = textSize,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

// Enlarged letter that follows the finger while dragging on the alphabet bar.
@Composable
private fun LetterBubble(letter: Char?, modifier: Modifier = Modifier) {
    if (letter == null) return
    Box(
        modifier = modifier.size(56.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(letter.toString(), color = MaterialTheme.colorScheme.onPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
    }
}

private fun triggerHapticTick(context: Context) {
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
    vibrator.vibrate(VibrationEffect.createOneShot(12, VibrationEffect.DEFAULT_AMPLITUDE))
}
