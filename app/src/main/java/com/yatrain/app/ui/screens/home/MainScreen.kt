package com.yatrain.app.ui.screens.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.outlined.DirectionsRailway
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yatrain.app.i18n.LocalStrings
import com.yatrain.app.models.PNRStatus
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yatrain.app.ui.screens.alerts.AlertsScreen
import com.yatrain.app.ui.screens.profile.ProfileScreen
import com.yatrain.app.ui.screens.search.SearchScreen
import com.yatrain.app.ui.screens.search.SearchViewModel
import com.yatrain.app.ui.screens.track.LiveTrainEntry
import com.yatrain.app.ui.screens.track.TrackScreen
import com.yatrain.app.ui.screens.track.TrackViewModel
import com.yatrain.app.ui.theme.AppBackground

enum class YatrainTab(
    val icon: ImageVector,
    val iconFilled: ImageVector
) {
    STATION(Icons.Outlined.DirectionsRailway, Icons.Filled.DirectionsRailway),
    TRACK(  Icons.Outlined.Map,               Icons.Filled.Map),
    SLEEP(  Icons.Outlined.Notifications,     Icons.Filled.Notifications),
    YOU(    Icons.Outlined.AccountCircle,     Icons.Filled.AccountCircle),
}

@Composable
fun MainScreen() {
    var selectedTab  by remember { mutableStateOf(YatrainTab.STATION) }
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val tabBarHeight  = 80.dp + navBarPadding + 16.dp

    // Shared ViewModels — same instances used by their screens and navigation callbacks
    val trackViewModel: TrackViewModel = viewModel()
    val searchViewModel: SearchViewModel = viewModel()
    val popularTrains by searchViewModel.popularTrains.collectAsStateWithLifecycle()

    val navigateToTrack: (String, PNRStatus?) -> Unit = { trainNumber, pnr ->
        trackViewModel.setPNRContext(pnr)
        trackViewModel.onQueryChange(trainNumber)
        trackViewModel.search()
        selectedTab = YatrainTab.TRACK
    }

    val navigateToSearchWithPnr: (String) -> Unit = { pnrNumber ->
        searchViewModel.onQueryChange(pnrNumber)
        searchViewModel.search()
        selectedTab = YatrainTab.STATION
    }

    val navigateToTrackList: () -> Unit = {
        trackViewModel.loadPopularTrains(popularTrains.map { t ->
            LiveTrainEntry(
                number    = t.number,
                name      = t.name,
                fromCode  = "",
                fromName  = t.from,
                toCode    = "",
                toName    = t.to,
                arrival   = "First",
                departure = t.departure,
                platform  = "–",
                delayMin  = t.delayMinutes
            )
        })
        selectedTab = YatrainTab.TRACK
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        // Content area — padded so nothing hides behind the floating tab bar
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = tabBarHeight)
        ) {
            when (selectedTab) {
                YatrainTab.STATION -> SearchScreen(
                    viewModel         = searchViewModel,
                    onNavigateToTrack = { n, p -> navigateToTrack(n, p) },
                    onViewAll         = navigateToTrackList
                )
                YatrainTab.TRACK   -> TrackScreen(viewModel = trackViewModel)
                YatrainTab.SLEEP   -> AlertsScreen(
                    onTrackTrain = { number -> navigateToTrack(number, null) },
                    onSearchPnr  = { pnr -> navigateToSearchWithPnr(pnr) }
                )
                YatrainTab.YOU     -> ProfileScreen(
                    onNavigateToSleep = { selectedTab = YatrainTab.SLEEP }
                )
            }
        }

        // Floating capsule tab bar
        YatrainTabBar(
            selectedTab   = selectedTab,
            onTabSelected = { selectedTab = it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 16.dp, start = 20.dp, end = 20.dp)
        )
    }
}

@Composable
fun YatrainTabBar(
    selectedTab: YatrainTab,
    onTabSelected: (YatrainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val gradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF173573), Color(0xFF132547), Color(0xFF13264D))
    )
    val borderBrush = Brush.verticalGradient(
        colors = listOf(Color.White.copy(alpha = 0.9f), Color.White.copy(alpha = 0.25f))
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(50.dp))
            .clip(RoundedCornerShape(50.dp))
            .background(gradient)
            .border(1.dp, borderBrush, RoundedCornerShape(50.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        YatrainTab.entries.forEach { tab ->
            TabItem(
                tab      = tab,
                selected = selectedTab == tab,
                onClick  = { onTabSelected(tab) }
            )
        }
    }
}

@Composable
private fun RowScope.TabItem(
    tab: YatrainTab,
    selected: Boolean,
    onClick: () -> Unit
) {
    val strings = LocalStrings.current
    val label = when (tab) {
        YatrainTab.STATION -> strings.tabStation
        YatrainTab.TRACK   -> strings.tabTrack
        YatrainTab.SLEEP   -> strings.tabSleep
        YatrainTab.YOU     -> strings.tabYou
    }
    val scale by animateFloatAsState(
        targetValue   = if (selected) 1.15f else 1.0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f),
        label         = "tabScale_${tab.name}"
    )
    val alpha by animateFloatAsState(
        targetValue = if (selected) 1.0f else 0.55f,
        label       = "tabAlpha_${tab.name}"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .weight(1f)
            .scale(scale)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,
                onClick           = onClick
            )
            .padding(vertical = 6.dp)
    ) {
        Icon(
            imageVector        = if (selected) tab.iconFilled else tab.icon,
            contentDescription = label,
            tint               = Color.White.copy(alpha = alpha),
            modifier           = Modifier.size(24.dp)
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text       = label,
            fontSize   = 11.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color      = Color.White.copy(alpha = alpha)
        )
    }
}
