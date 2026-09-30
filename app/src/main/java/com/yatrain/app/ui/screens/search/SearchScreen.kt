package com.yatrain.app.ui.screens.search

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yatrain.app.models.Passenger
import com.yatrain.app.models.PNRStatus
import com.yatrain.app.models.Station
import com.yatrain.app.models.Train
import com.yatrain.app.ui.screens.track.LiveTrainEntry
import com.yatrain.app.ui.screens.track.StationInfo
import com.yatrain.app.ui.screens.track.stationNameFor
import com.yatrain.app.i18n.LocalStrings
import com.yatrain.app.i18n.localTrainName
import com.yatrain.app.ui.components.HeroSection
import com.yatrain.app.ui.components.ProBannerCard
import com.yatrain.app.ui.screens.subscription.ProPaywallSheet
import com.yatrain.app.ui.screens.subscription.SubscriptionViewModel
import com.yatrain.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
//  Screen
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel = viewModel(),
    onNavigateToTrack: (String, PNRStatus?) -> Unit = { _, _ -> },
    onViewAll: () -> Unit = {}
) {
    val query              by viewModel.query.collectAsStateWithLifecycle()
    val results            by viewModel.results.collectAsStateWithLifecycle()
    val pnrResult          by viewModel.pnrResult.collectAsStateWithLifecycle()
    val isLoading          by viewModel.isLoading.collectAsStateWithLifecycle()
    val error              by viewModel.error.collectAsStateWithLifecycle()
    val selectedTrain      by viewModel.selectedTrain.collectAsStateWithLifecycle()
    val schedule           by viewModel.schedule.collectAsStateWithLifecycle()
    val scheduleLoading    by viewModel.scheduleLoading.collectAsStateWithLifecycle()
    val currentStop        by viewModel.currentStopIndex.collectAsStateWithLifecycle()
    val coachPnrInput      by viewModel.coachPnrInput.collectAsStateWithLifecycle()
    val coachPnrResult     by viewModel.coachPnrResult.collectAsStateWithLifecycle()
    val coachPnrLoading    by viewModel.coachPnrLoading.collectAsStateWithLifecycle()
    val alerted            by viewModel.alertedStations.collectAsStateWithLifecycle()
    val alertPickerStation by viewModel.alertPickerStation.collectAsStateWithLifecycle()
    val snackMsg           by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val isTicketSaved      by viewModel.isTicketSaved.collectAsStateWithLifecycle()
    val popularTrains      by viewModel.popularTrains.collectAsStateWithLifecycle()
    val popularLoading     by viewModel.popularLoading.collectAsStateWithLifecycle()
    val stationSuggestions    by viewModel.stationSuggestions.collectAsStateWithLifecycle()
    val selectedStation       by viewModel.selectedStation.collectAsStateWithLifecycle()
    val stationTrains         by viewModel.stationTrains.collectAsStateWithLifecycle()
    val stationTrainsLoading  by viewModel.stationTrainsLoading.collectAsStateWithLifecycle()

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager       = LocalFocusManager.current
    val snackbarHostState  = remember { SnackbarHostState() }
    val scope              = rememberCoroutineScope()
    var showPaywall        by remember { mutableStateOf(false) }
    val subViewModel: SubscriptionViewModel = viewModel()
    val isPro              by subViewModel.isPro.collectAsStateWithLifecycle()

    LaunchedEffect(snackMsg) {
        snackMsg?.let {
            scope.launch {
                snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
                viewModel.clearSnackbar()
            }
        }
    }

    // ── Route detail overlay ──────────────────────────────────────────────────
    if (selectedTrain != null) {
        Box(Modifier.fillMaxSize()) {
            RouteDetailView(
                train        = selectedTrain!!,
                schedule     = schedule,
                isLoading    = scheduleLoading,
                currentIndex = currentStop,
                alerted      = alerted,
                onAlertTap   = { station ->
                    if (station.code in alerted) viewModel.cancelStationAlert(station)
                    else if (!isPro && alerted.size >= 2) showPaywall = true
                    else viewModel.showAlertPicker(station)
                },
                onBack = { viewModel.clearSelection() }
            )

            // Alert picker sheet shown on top
            if (alertPickerStation != null) {
                AlertPickerSheet(
                    station     = alertPickerStation!!,
                    trainNumber = selectedTrain!!.number,
                    onConfirm   = { mins -> viewModel.setStationAlert(alertPickerStation!!, mins) },
                    onDismiss   = { viewModel.hideAlertPicker() }
                )
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier  = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
            )
        }
        return
    }

    // ── Default search layout ─────────────────────────────────────────────────
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier       = Modifier.fillMaxSize().background(AppBackground),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                val s = LocalStrings.current
                HeroSection(eyebrow = s.homeEyebrow, title = s.homeTitle, titleAccent = s.homeAccent)
            }

            item {
                SearchPill(
                    query         = query,
                    onQueryChange = { viewModel.onQueryChange(it) },
                    onSearch      = { keyboardController?.hide(); focusManager.clearFocus(); viewModel.search() },
                    modifier      = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp)
                )
            }

            // Station name typeahead suggestions
            if (stationSuggestions.isNotEmpty()) {
                item {
                    StationSuggestionsCard(
                        suggestions = stationSuggestions,
                        onSelect    = { station ->
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            viewModel.onStationSelected(station)
                        },
                        modifier = Modifier.padding(horizontal = 18.dp).offset(y = (-6).dp)
                    )
                }
            }

            if (error != null) {
                item {
                    Row(Modifier.padding(horizontal = 20.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("⚠", color = AppOrange)
                        Spacer(Modifier.width(6.dp))
                        Text(error!!, fontSize = 13.sp, color = AppTitle)
                    }
                }
            }

            if (isLoading) {
                item {
                    Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AppOrange, modifier = Modifier.size(32.dp))
                    }
                }
            } else if (pnrResult != null) {
                item {
                    PopularRightNowSection(
                        trains    = popularTrains,
                        isLoading = popularLoading,
                        onViewAll = onViewAll,
                        onTrainTap = { popularTrain ->
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            onNavigateToTrack(popularTrain.number, null)
                        }
                    )
                }
                item {
                    Box(Modifier.padding(horizontal = 18.dp, vertical = 4.dp)) {
                        if (isPro) {
                            PNRBoardingCard(
                                pnr     = pnrResult!!,
                                isSaved = isTicketSaved,
                                onSave  = { viewModel.saveTicket() },
                                onTrack = { onNavigateToTrack(pnrResult!!.trainNumber, pnrResult) }
                            )
                        } else {
                            PNRBasicCard(
                                pnr     = pnrResult!!,
                                onTrack = { onNavigateToTrack(pnrResult!!.trainNumber, pnrResult) },
                                onUpgrade = { showPaywall = true }
                            )
                        }
                    }
                }
                item {
                    if (isPro) {
                        CoachMapCard(
                            coachPnrInput     = coachPnrInput,
                            coachPnrResult    = coachPnrResult,
                            isLoading         = coachPnrLoading,
                            onPnrChange       = { viewModel.onCoachPnrChange(it) },
                            onSearch          = { viewModel.searchCoachPnr() },
                            externalPnrResult = pnrResult
                        )
                    } else {
                        ProLockedCard(
                            title     = LocalStrings.current.proCoachFinder,
                            onUpgrade = { showPaywall = true },
                            modifier  = Modifier.padding(horizontal = 18.dp, vertical = 4.dp)
                        )
                    }
                }
                item { QuickServicesSection() }
            } else if (selectedStation != null) {
                item {
                    StationTrainsSection(
                        station    = selectedStation!!,
                        trains     = stationTrains,
                        isLoading  = stationTrainsLoading,
                        onTrainTap = { train ->
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            onNavigateToTrack(train.number, null)
                        },
                        onClear = { viewModel.clearStationSelection() }
                    )
                }
            } else if (results.isNotEmpty()) {
                items(results) { train ->
                    Box(Modifier.padding(horizontal = 18.dp, vertical = 5.dp)) {
                        TrainResultCard(
                            train = train,
                            onTap = { keyboardController?.hide(); focusManager.clearFocus(); viewModel.onTrainCardTapped(train) }
                        )
                    }
                }
            } else if (query.length >= 3 && pnrResult == null && error == null
                       && stationSuggestions.isEmpty() && selectedStation == null) {
                item {
                    val s = LocalStrings.current
                    Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                        Text(s.noTrainsFound, color = Zinc500)
                    }
                }
            } else if (stationSuggestions.isEmpty() && selectedStation == null) {
                item {
                    ProBannerCard(
                        viewModel = subViewModel,
                        onTap     = { showPaywall = true },
                        modifier  = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                    )
                }
                item {
                    PopularRightNowSection(
                        trains    = popularTrains,
                        isLoading = popularLoading,
                        onViewAll = onViewAll,
                        onTrainTap = { popularTrain ->
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            // Navigate to Track tab — TrackViewModel is pre-loaded there
                            onNavigateToTrack(popularTrain.number, null)
                        }
                    )
                }
                item { YourTicketCard() }
                item {
                    if (isPro) {
                        CoachMapCard(
                            coachPnrInput  = coachPnrInput,
                            coachPnrResult = coachPnrResult,
                            isLoading      = coachPnrLoading,
                            onPnrChange    = { viewModel.onCoachPnrChange(it) },
                            onSearch       = { viewModel.searchCoachPnr() }
                        )
                    } else {
                        ProLockedCard(
                            title     = LocalStrings.current.proCoachFinder,
                            onUpgrade = { showPaywall = true },
                            modifier  = Modifier.padding(horizontal = 18.dp, vertical = 4.dp)
                        )
                    }
                }
                item { AffiliateBannersSection() }
                item { QuickServicesSection() }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier  = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
        )
    }

    if (showPaywall) {
        ProPaywallSheet(viewModel = subViewModel, onDismiss = { showPaywall = false })
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Search pill
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SearchPill(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val navGradient = Brush.linearGradient(colors = listOf(Color(0xFF173573), Color(0xFF13264D)))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .shadow(16.dp, RoundedCornerShape(50.dp))
            .clip(RoundedCornerShape(50.dp))
            .background(Color.White)
    ) {
        Icon(Icons.Filled.Search, null, tint = Color(0xFF9CA3AF), modifier = Modifier.padding(start = 18.dp).size(20.dp))
        BasicTextField(
            value           = query,
            onValueChange   = onQueryChange,
            singleLine      = true,
            textStyle       = TextStyle(fontSize = 14.sp, color = Color(0xFF1F2937)),
            cursorBrush     = SolidColor(AppNavy),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            modifier        = Modifier.weight(1f).padding(horizontal = 10.dp, vertical = 17.dp),
            decorationBox   = { inner ->
                Box {
                    if (query.isEmpty()) {
                        Text(LocalStrings.current.searchHint, fontSize = 14.sp, color = Color(0xFF9CA3AF), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    inner()
                }
            }
        )
        if (query.isNotEmpty()) {
            IconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.Clear, null, tint = Color(0xFFD1D5DB), modifier = Modifier.size(18.dp))
            }
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .padding(end = 5.dp, top = 5.dp, bottom = 5.dp)
                .clip(RoundedCornerShape(50.dp))
                .background(navGradient)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onSearch)
                .padding(horizontal = 20.dp, vertical = 13.dp)
        ) {
            Text(LocalStrings.current.findTrain, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Popular Right Now
// ─────────────────────────────────────────────────────────────────────────────

data class PopularTrain(
    val number: String,
    val name: String,
    val from: String,
    val to: String,
    val departure: String = "",
    val arrival: String = "",
    val delayMinutes: Int = 0
)

@Composable
private fun PopularRightNowSection(
    trains: List<PopularTrain>,
    isLoading: Boolean,
    onViewAll: () -> Unit = {},
    onTrainTap: (PopularTrain) -> Unit
) {
    val s = LocalStrings.current
    Column {
        Row(
            modifier              = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(s.popularRightNow, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = AppMuted)
            Text(
                text     = s.viewAll,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color    = AppTitle,
                modifier = Modifier.clickable(onClick = onViewAll)
            )
        }
        if (isLoading && trains.isEmpty()) {
            LazyRow(
                contentPadding        = PaddingValues(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(5) { PopularTrainShimmer() }
            }
        } else {
            LazyRow(
                contentPadding        = PaddingValues(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(trains) { train ->
                    PopularTrainCard(train = train, onClick = { onTrainTap(train) })
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun PopularTrainShimmer() {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue  = 0.3f,
        targetValue   = 0.7f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label         = "shimmerAlpha"
    )
    Box(
        modifier = Modifier
            .width(220.dp)
            .height(72.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(AppCard.copy(alpha = alpha))
    )
}

@Composable
private fun PopularTrainCard(train: PopularTrain, onClick: () -> Unit) {
    Card(
        modifier  = Modifier.width(220.dp),
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = AppCard),
        border    = BorderStroke(1.dp, AppBorder),
        elevation = CardDefaults.cardElevation(2.dp),
        onClick   = onClick
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier         = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(AppNavy.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.DirectionsRailway, null, tint = AppNavy, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(train.number, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AppTitle)
                    if (train.delayMinutes > 0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(AppOrange)
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text("+${train.delayMinutes}m", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
                Text(train.name.localTrainName(), fontSize = 12.sp, color = AppBody, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${train.from} → ${train.to}", fontSize = 11.sp, color = Zinc500, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Outlined.ChevronRight, null, tint = AppOrange, modifier = Modifier.size(18.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Your Ticket card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun YourTicketCard() {
    Card(
        modifier  = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = AppCard),
        border    = BorderStroke(1.5.dp, Zinc200),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier         = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(AppNavy.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.ConfirmationNumber, null, tint = AppNavy, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(LocalStrings.current.yourTicket, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = AppNavy)
                Text(LocalStrings.current.yourTicketSub, fontSize = 12.sp, color = Zinc500)
            }
            Icon(Icons.Filled.Search, null, tint = Zinc400, modifier = Modifier.size(20.dp))
        }
    }
    Spacer(Modifier.height(12.dp))
}

// ─────────────────────────────────────────────────────────────────────────────
//  Coach map card — enabled only when a valid PNR is entered
// ─────────────────────────────────────────────────────────────────────────────

private enum class CoachKind { LOCO, POWER, GENERAL, SLEEPER, AC3T, AC2T, AC1T }
private data class CoachInfo(
    val code: String, val typeName: String, val kind: CoachKind, val highlighted: Boolean = false
)

private fun composeCoachList(trainName: String): List<CoachInfo> {
    val n = trainName.lowercase()
    val isRajdhani = n.contains("rajdhani") || n.contains("duronto")
    val isShatabdi = n.contains("shatabdi") || n.contains("vande bharat") || n.contains("vande")
    return when {
        isRajdhani -> listOf(
            CoachInfo("ENG",  "Loco",   CoachKind.LOCO),
            CoachInfo("EOG1", "Power",  CoachKind.POWER),
            CoachInfo("H1",   "1A",     CoachKind.AC1T),
            CoachInfo("H2",   "1A",     CoachKind.AC1T),
            CoachInfo("A1",   "2A",     CoachKind.AC2T),
            CoachInfo("A2",   "2A",     CoachKind.AC2T),
            CoachInfo("A3",   "2A",     CoachKind.AC2T),
            CoachInfo("PAN",  "Pantry", CoachKind.POWER),
            CoachInfo("B1",   "3A",     CoachKind.AC3T),
            CoachInfo("B2",   "3A",     CoachKind.AC3T),
            CoachInfo("B3",   "3A",     CoachKind.AC3T),
            CoachInfo("B4",   "3A",     CoachKind.AC3T),
            CoachInfo("B5",   "3A",     CoachKind.AC3T),
            CoachInfo("B6",   "3A",     CoachKind.AC3T),
            CoachInfo("B7",   "3A",     CoachKind.AC3T),
            CoachInfo("B8",   "3A",     CoachKind.AC3T),
            CoachInfo("EOG2", "Power",  CoachKind.POWER),
            CoachInfo("GRD",  "Guard",  CoachKind.GENERAL),
        )
        isShatabdi -> listOf(
            CoachInfo("ENG",  "Loco",   CoachKind.LOCO),
            CoachInfo("EOG1", "Power",  CoachKind.POWER),
            CoachInfo("EC1",  "EC",     CoachKind.AC1T),
            CoachInfo("EC2",  "EC",     CoachKind.AC1T),
            CoachInfo("C1",   "CC",     CoachKind.AC2T),
            CoachInfo("C2",   "CC",     CoachKind.AC2T),
            CoachInfo("C3",   "CC",     CoachKind.AC2T),
            CoachInfo("PAN",  "Pantry", CoachKind.POWER),
            CoachInfo("C4",   "CC",     CoachKind.AC2T),
            CoachInfo("C5",   "CC",     CoachKind.AC2T),
            CoachInfo("C6",   "CC",     CoachKind.AC2T),
            CoachInfo("C7",   "CC",     CoachKind.AC2T),
            CoachInfo("C8",   "CC",     CoachKind.AC2T),
            CoachInfo("EOG2", "Power",  CoachKind.POWER),
            CoachInfo("GRD",  "Guard",  CoachKind.GENERAL),
        )
        else -> listOf(
            CoachInfo("ENG",  "Loco",   CoachKind.LOCO),
            CoachInfo("D1",   "GEN",    CoachKind.GENERAL),
            CoachInfo("D2",   "GEN",    CoachKind.GENERAL),
            CoachInfo("H1",   "1A",     CoachKind.AC1T),
            CoachInfo("A1",   "2A",     CoachKind.AC2T),
            CoachInfo("A2",   "2A",     CoachKind.AC2T),
            CoachInfo("PAN",  "Pantry", CoachKind.POWER),
            CoachInfo("B1",   "3A",     CoachKind.AC3T),
            CoachInfo("B2",   "3A",     CoachKind.AC3T),
            CoachInfo("B3",   "3A",     CoachKind.AC3T),
            CoachInfo("B4",   "3A",     CoachKind.AC3T),
            CoachInfo("B5",   "3A",     CoachKind.AC3T),
            CoachInfo("S1",   "SL",     CoachKind.SLEEPER),
            CoachInfo("S2",   "SL",     CoachKind.SLEEPER),
            CoachInfo("S3",   "SL",     CoachKind.SLEEPER),
            CoachInfo("S4",   "SL",     CoachKind.SLEEPER),
            CoachInfo("S5",   "SL",     CoachKind.SLEEPER),
            CoachInfo("S6",   "SL",     CoachKind.SLEEPER),
            CoachInfo("D3",   "GEN",    CoachKind.GENERAL),
            CoachInfo("GRD",  "Guard",  CoachKind.GENERAL),
        )
    }
}

private fun inferTypeName(code: String): String = when {
    code.startsWith("A", ignoreCase = true) -> "2A"
    code.startsWith("B", ignoreCase = true) -> "3A"
    code.startsWith("H", ignoreCase = true) -> "1A"
    code.startsWith("S", ignoreCase = true) -> "SL"
    code.startsWith("D", ignoreCase = true) -> "GEN"
    else -> code
}

private fun inferKind(code: String): CoachKind = when {
    code.startsWith("A", ignoreCase = true) -> CoachKind.AC2T
    code.startsWith("B", ignoreCase = true) -> CoachKind.AC3T
    code.startsWith("H", ignoreCase = true) -> CoachKind.AC1T
    code.startsWith("S", ignoreCase = true) -> CoachKind.SLEEPER
    else -> CoachKind.GENERAL
}

private fun buildCoachList(assignedCode: String?, trainName: String = ""): Pair<List<CoachInfo>, Int> {
    val base = composeCoachList(trainName)
    if (assignedCode.isNullOrBlank()) return base to -1
    val normalised = assignedCode.trim().uppercase()
    val idx = base.indexOfFirst { it.code.uppercase() == normalised }
    return if (idx >= 0) {
        base.mapIndexed { i, c -> c.copy(highlighted = i == idx) } to idx
    } else {
        // Coach not in standard list — append it as highlighted
        val extra = CoachInfo(
            assignedCode.trim(),
            inferTypeName(assignedCode.trim()),
            inferKind(assignedCode.trim()),
            highlighted = true
        )
        (base + extra) to base.size
    }
}

private fun PNRStatus.effectiveCoachId(): String {
    val p = passengers.firstOrNull() ?: return coach
    return p.currentCoachId.ifBlank { p.bookingCoachId }.ifBlank { coach }
}

private fun PNRStatus.effectiveBerthNo(): Int {
    val p = passengers.firstOrNull() ?: return 0
    return if (p.currentBerthNo > 0) p.currentBerthNo else p.bookingBerthNo
}

private fun coachPositionHint(coachCode: String, dest: String): String {
    val u = coachCode.uppercase()
    val dir = if (dest.isNotBlank()) "towards $dest" else "check platform display"
    return when {
        u.startsWith("A") || u.startsWith("H") -> "Walk towards Guard\nYour coach is near the rear"
        u.startsWith("B") || u.startsWith("C") -> "Walk towards AC section\n$dir"
        u.startsWith("S") -> "Check middle section\n$dir"
        else -> "Check platform display\n$dir"
    }
}

@Composable
private fun CoachMapCard(
    coachPnrInput:     String,
    coachPnrResult:    PNRStatus?,
    isLoading:         Boolean,
    onPnrChange:       (String) -> Unit,
    onSearch:          (() -> Unit)? = null,
    externalPnrResult: PNRStatus? = null
) {
    val effectiveResult = externalPnrResult ?: coachPnrResult
    val (coaches, highlightIdx) = remember(effectiveResult) {
        buildCoachList(effectiveResult?.effectiveCoachId(), effectiveResult?.trainName ?: "")
    }
    val listState  = rememberLazyListState()
    val infinite   = rememberInfiniteTransition(label = "coachAnim")
    val pulseScale by infinite.animateFloat(
        initialValue  = 0.85f, targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label         = "pulse"
    )
    val shimmer by infinite.animateFloat(
        initialValue  = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart),
        label         = "shimmer"
    )

    val isPnrValid     = coachPnrInput.length == 10 && coachPnrInput.all { it.isDigit() }
    val isActive       = effectiveResult != null && highlightIdx >= 0
    val isExternalMode = externalPnrResult != null

    // Auto-scroll and CENTER the highlighted coach when result arrives
    LaunchedEffect(highlightIdx) {
        if (highlightIdx >= 0) {
            // First snap to make the item visible, then measure & center
            listState.scrollToItem(highlightIdx)
            delay(60)
            val info     = listState.layoutInfo
            val itemInfo = info.visibleItemsInfo.find { it.index == highlightIdx }
            if (itemInfo != null) {
                val viewportSize = info.viewportEndOffset - info.viewportStartOffset
                val delta = (itemInfo.offset + itemInfo.size / 2) - (viewportSize / 2)
                listState.animateScrollBy(delta.toFloat())
            }
        }
    }
    // Auto-cycle only when no active highlight
    LaunchedEffect(isActive) {
        if (!isActive) {
            while (true) {
                delay(1400)
                val next = (listState.firstVisibleItemIndex + 1) % coaches.size.coerceAtLeast(1)
                listState.animateScrollToItem(next)
            }
        }
    }

    Card(
        modifier  = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
        shape     = RoundedCornerShape(18.dp),
        colors    = CardDefaults.cardColors(containerColor = AppCard),
        border    = BorderStroke(1.dp, AppBorder),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column(Modifier.padding(top = 18.dp, bottom = 14.dp)) {

            // ── Header ────────────────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                verticalAlignment     = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(LocalStrings.current.coachMap, fontSize = 12.sp, color = Zinc500)
                    Text(
                        LocalStrings.current.findYourCoach,
                        fontSize = 19.sp, fontWeight = FontWeight.Bold, color = AppNavy
                    )
                }
                if (isActive) {
                    val activeCoach = effectiveResult?.effectiveCoachId() ?: ""
                    val activeBerth = effectiveResult?.effectiveBerthNo() ?: 0
                    Row(
                        modifier = Modifier
                            .scale(pulseScale)
                            .clip(RoundedCornerShape(50.dp))
                            .background(AppGreen.copy(alpha = 0.12f))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(AppGreen))
                        Text(
                            buildString {
                                append(activeCoach)
                                if (activeBerth > 0) append(" · Seat $activeBerth")
                            },
                            fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AppGreen
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── PNR input field — hidden when external PNR provides data ─────
            if (!isExternalMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isPnrValid) AppOrange.copy(0.06f) else AppBackground)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Outlined.ConfirmationNumber, null,
                        tint     = if (isPnrValid) AppOrange else AppMuted,
                        modifier = Modifier.size(18.dp)
                    )
                    BasicTextField(
                        value           = coachPnrInput,
                        onValueChange   = { if (it.length <= 10) onPnrChange(it) },
                        singleLine      = true,
                        textStyle       = TextStyle(fontSize = 14.sp, color = AppNavy, fontWeight = FontWeight.SemiBold),
                        cursorBrush     = SolidColor(AppOrange),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { onSearch?.invoke() }),
                        modifier        = Modifier.weight(1f),
                        decorationBox   = { inner ->
                            Box {
                                if (coachPnrInput.isEmpty()) {
                                    Text(LocalStrings.current.enterPnrToFindCoach, fontSize = 13.sp, color = AppMuted)
                                }
                                inner()
                            }
                        }
                    )
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = AppOrange, strokeWidth = 2.dp)
                    } else if (coachPnrInput.isNotEmpty()) {
                        IconButton(onClick = { onPnrChange("") }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Filled.Clear, null, tint = AppMuted, modifier = Modifier.size(14.dp))
                        }
                    }
                }

                // Result summary row (only for self-entered PNR)
                if (effectiveResult != null && !isExternalMode) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(Modifier.size(7.dp).clip(CircleShape).background(AppGreen))
                        val c = effectiveResult.effectiveCoachId()
                        val b = effectiveResult.effectiveBerthNo()
                        Text(
                            "${effectiveResult.trainName}  ·  Coach ${c.ifBlank { "–" }}  ·  Berth ${if (b > 0) b else "–"}",
                            fontSize = 11.sp, color = AppBody, fontWeight = FontWeight.Medium,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                } else if (isPnrValid && !isLoading) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        LocalStrings.current.coachAssignmentNotFound,
                        fontSize = 11.sp, color = AppMuted,
                        modifier = Modifier.padding(horizontal = 18.dp)
                    )
                }

                Spacer(Modifier.height(14.dp))
            }

            // ── Direction pill ────────────────────────────────────────────
            val sDir = LocalStrings.current
            Row(
                modifier = Modifier
                    .padding(horizontal = 18.dp)
                    .clip(RoundedCornerShape(50.dp))
                    .background(AppBackground)
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("←", fontSize = 15.sp, color = AppOrange, fontWeight = FontWeight.Bold)
                Text(
                    if (effectiveResult != null) "${sDir.directionTowards} ${stationNameFor(effectiveResult.to)}"
                    else sDir.directionDefault,
                    fontSize = 13.sp, color = AppBody
                )
            }

            // ── Coach position hint (external mode only) ──────────────────
            if (isExternalMode && isActive) {
                val hintCoach = effectiveResult!!.effectiveCoachId()
                val hintText  = coachPositionHint(hintCoach, effectiveResult.to)
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .padding(horizontal = 18.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(AppOrange.copy(0.08f))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier         = Modifier.size(36.dp).clip(CircleShape).background(AppOrange.copy(0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.ArrowForward, null, tint = AppOrange, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(hintText.lines().first(), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AppNavy)
                        Text(hintText.lines().getOrElse(1) { "" }, fontSize = 11.sp, color = AppMuted)
                    }
                    Box(
                        modifier         = Modifier.clip(RoundedCornerShape(8.dp)).background(AppOrange).padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(hintCoach, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            if (!isExternalMode) Spacer(Modifier.height(14.dp)) else Spacer(Modifier.height(14.dp))

            Spacer(Modifier.height(18.dp))

            // ── Animated coach strip ──────────────────────────────────────
            Box {
                LazyRow(
                    state                 = listState,
                    contentPadding        = PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(coaches) { idx, coach ->
                        CoachItem(
                            coach          = coach,
                            shimmerProgress = shimmer,
                            mirrored       = idx == coaches.lastIndex && coach.kind == CoachKind.LOCO
                        )
                    }
                }
                // Disabled overlay when no PNR entered (only in self-entry mode)
                if (!isExternalMode && !isPnrValid && !isLoading) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(Color.White.copy(alpha = 0.60f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            LocalStrings.current.enterPnrAbove,
                            fontSize  = 12.sp,
                            color     = AppMuted,
                            textAlign = TextAlign.Center,
                            modifier  = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            Text(
                "${coaches.size} ${LocalStrings.current.coachesCount}",
                fontSize = 12.sp, color = Zinc400,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                textAlign = TextAlign.End
            )
        }
    }
    Spacer(Modifier.height(12.dp))
}

// ─────────────────────────────────────────────────────────────────────────────
//  Single coach illustration — flat design
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CoachItem(coach: CoachInfo, shimmerProgress: Float, mirrored: Boolean = false) {
    // Flat colour palette: warm salmon when highlighted, cool blue-gray otherwise
    val bodyBg   = if (coach.highlighted) Color(0xFFFFF0EC) else Color(0xFFEBF0F8)
    val winBg    = if (coach.highlighted) Color(0xFFFFD5C5) else Color(0xFFCDD9EE)
    val stripe1  = if (coach.highlighted) Color(0xFFFFCCBA) else Color(0xFFBFCCE0)
    val stripe2  = if (coach.highlighted) Color(0xFFEEB8A4) else Color(0xFFB0BFCF)
    val underBg  = if (coach.highlighted) Color(0xFFE8BEB4) else Color(0xFFC4CED8)
    val wheelBg  = if (coach.highlighted) Color(0xFFD4A898) else Color(0xFFAEBCC8)
    val railC    = Color(0xFFAAB8C4)

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(105.dp)) {

        // ── YOU chip slot — always same height so all coaches baseline-align ──
        Box(Modifier.height(30.dp), contentAlignment = Alignment.BottomCenter) {
            if (coach.highlighted) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .background(AppOrange)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Outlined.AccountCircle, null, tint = Color.White, modifier = Modifier.size(12.dp))
                    Text(LocalStrings.current.youLabel, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, letterSpacing = 0.5.sp)
                }
            }
        }
        Spacer(Modifier.height(3.dp))

        // ── Coach Canvas ───────────────────────────────────────────────────────
        Canvas(modifier = Modifier.fillMaxWidth().height(88.dp)) {
            val w  = size.width
            val h  = size.height
            val cr = 6.dp.toPx()

            if (coach.kind == CoachKind.LOCO) {
                // ── VANDE BHARAT style LOCOMOTIVE ─────────────────────────
                val vbSilver = Color(0xFFEEF0F8)
                val vbBlue   = Color(0xFF003B9A)
                val vbGlass  = Color(0xFF1A2633)
                val vbWheel  = Color(0xFF455A64)

                val bodyH    = h * 0.57f
                val bodyT    = h * 0.06f
                val bodyB    = bodyT + bodyH
                val noseW    = w * 0.28f
                val leftFace = !mirrored

                // Smooth bullet-nose body outline
                val bodyOutline = Path()
                if (leftFace) {
                    bodyOutline.moveTo(w - cr, bodyT)
                    bodyOutline.quadraticTo(w, bodyT, w, bodyT + cr)
                    bodyOutline.lineTo(w, bodyB - cr)
                    bodyOutline.quadraticTo(w, bodyB, w - cr, bodyB)
                    bodyOutline.lineTo(noseW, bodyB)
                    bodyOutline.cubicTo(noseW * 0.35f, bodyB, 0f, bodyB - bodyH * 0.17f, 0f, bodyT + bodyH * 0.5f)
                    bodyOutline.cubicTo(0f, bodyT + bodyH * 0.17f, noseW * 0.35f, bodyT, noseW, bodyT)
                    bodyOutline.close()
                } else {
                    bodyOutline.moveTo(cr, bodyT)
                    bodyOutline.quadraticTo(0f, bodyT, 0f, bodyT + cr)
                    bodyOutline.lineTo(0f, bodyB - cr)
                    bodyOutline.quadraticTo(0f, bodyB, cr, bodyB)
                    bodyOutline.lineTo(w - noseW, bodyB)
                    bodyOutline.cubicTo(w - noseW * 0.35f, bodyB, w, bodyB - bodyH * 0.17f, w, bodyT + bodyH * 0.5f)
                    bodyOutline.cubicTo(w, bodyT + bodyH * 0.17f, w - noseW * 0.35f, bodyT, w - noseW, bodyT)
                    bodyOutline.close()
                }

                // 1. Silver body (full nose outline)
                drawPath(bodyOutline, vbSilver)

                // 2. Blue stripe — bottom 30%, follows the nose curve on the nose end
                val stripeY   = bodyT + bodyH * 0.70f
                val noseXStripe = noseW * 0.15f   // approx nose-curve x at stripeY
                val bluePath = Path()
                if (leftFace) {
                    bluePath.moveTo(w, stripeY)
                    bluePath.lineTo(w, bodyB - cr)
                    bluePath.quadraticTo(w, bodyB, w - cr, bodyB)
                    bluePath.lineTo(noseW, bodyB)
                    bluePath.cubicTo(noseW * 0.30f, bodyB, noseW * 0.05f, bodyT + bodyH * 0.78f, noseXStripe, stripeY)
                    bluePath.close()
                } else {
                    bluePath.moveTo(0f, stripeY)
                    bluePath.lineTo(0f, bodyB - cr)
                    bluePath.quadraticTo(0f, bodyB, cr, bodyB)
                    bluePath.lineTo(w - noseW, bodyB)
                    bluePath.cubicTo(w - noseW * 0.30f, bodyB, w - noseW * 0.05f, bodyT + bodyH * 0.78f, w - noseXStripe, stripeY)
                    bluePath.close()
                }
                drawPath(bluePath, vbBlue)

                // Angled windshield glass at nose
                val gpLeft = Path().apply {
                    moveTo(noseW * 0.78f, bodyT + bodyH * 0.06f)
                    lineTo(noseW * 0.10f, bodyT + bodyH * 0.22f)
                    lineTo(noseW * 0.08f, bodyT + bodyH * 0.58f)
                    lineTo(noseW * 0.76f, bodyT + bodyH * 0.58f)
                    close()
                }
                val gpRight = Path().apply {
                    moveTo(w - noseW * 0.78f, bodyT + bodyH * 0.06f)
                    lineTo(w - noseW * 0.10f, bodyT + bodyH * 0.22f)
                    lineTo(w - noseW * 0.08f, bodyT + bodyH * 0.58f)
                    lineTo(w - noseW * 0.76f, bodyT + bodyH * 0.58f)
                    close()
                }
                drawPath(if (leftFace) gpLeft else gpRight, vbGlass)

                // Headlight in blue stripe zone
                val hlX = if (leftFace) w * 0.03f else w - w * 0.03f
                val hlY = bodyT + bodyH * 0.81f
                drawCircle(Color(0xFFFFF9C4), w * 0.050f, Offset(hlX, hlY))
                drawCircle(Color.White,       w * 0.028f, Offset(hlX, hlY))

                // Undercarriage
                val ucT = bodyB
                val ucH = h * 0.08f
                val ucB = ucT + ucH
                drawRect(Color(0xFF37474F), Offset(0f, ucT), Size(w, ucH))

                // 4 wheels
                val wR    = h * 0.092f
                val wCY   = ucB + wR * 0.52f
                val railY = wCY + wR * 0.88f
                val wXs   = listOf(w * 0.14f, w * 0.38f, w * 0.62f, w * 0.86f)
                for (wx in wXs) {
                    drawCircle(vbWheel,           wR,         Offset(wx, wCY))
                    drawCircle(Color(0xFF78909C), wR * 0.40f, Offset(wx, wCY))
                    drawCircle(vbWheel, wR, Offset(wx, wCY), style = Stroke(1.2f.dp.toPx()))
                }
                drawLine(Color(0xFF37474F), Offset(wXs[0], wCY), Offset(wXs.last(), wCY), 2.dp.toPx())
                drawLine(railC, Offset(0f, railY), Offset(w, railY), 1.5f.dp.toPx())

            } else {
                // ── Regular coach drawing ──────────────────────────────────
                val bodyH  = h * 0.62f
                val bodyT  = 0f
                val bodyB  = bodyT + bodyH

                val nWin   = if (coach.kind == CoachKind.POWER) 3 else 4
                val winPad = 7.dp.toPx()
                val winGap = 4.dp.toPx()
                val winW   = (w - 2 * winPad - (nWin - 1) * winGap) / nWin
                val winH   = bodyH * 0.30f
                val winY   = bodyT + bodyH * 0.10f
                val winCR  = 2.5f.dp.toPx()

                val ucH    = h * 0.085f
                val ucT    = bodyB
                val ucB    = ucT + ucH
                val wR     = h * 0.088f
                val wCY    = ucB + wR * 0.55f
                val railY  = wCY + wR * 0.88f

                drawRoundRect(bodyBg, Offset(0f, bodyT), Size(w, bodyH), CornerRadius(cr))

                for (i in 0 until nWin) {
                    val wx = winPad + i * (winW + winGap)
                    drawRoundRect(winBg, Offset(wx, winY), Size(winW, winH), CornerRadius(winCR))
                }

                val s1Y = bodyT + bodyH * 0.52f
                val s2Y = bodyT + bodyH * 0.60f
                val sH2 = bodyH * 0.065f
                drawRect(stripe1, Offset(cr * 0.5f, s1Y), Size(w - cr, sH2))
                drawRect(stripe2, Offset(cr * 0.5f, s2Y), Size(w - cr, sH2))

                if (coach.highlighted) {
                    drawRoundRect(
                        color        = AppOrange,
                        topLeft      = Offset(0f, bodyT),
                        size         = Size(w, bodyH),
                        cornerRadius = CornerRadius(cr),
                        style        = Stroke(2.2f.dp.toPx())
                    )
                }

                drawRect(underBg, Offset(cr * 0.5f, ucT), Size(w - cr, ucH))

                val wxL = w * 0.24f
                val wxR = w * 0.76f
                for (wxPos in listOf(wxL, wxR)) {
                    drawCircle(wheelBg,             wR,         Offset(wxPos, wCY))
                    drawCircle(wheelBg.copy(0.55f), wR * 0.40f, Offset(wxPos, wCY))
                    drawCircle(wheelBg.copy(0.85f), wR,         Offset(wxPos, wCY), style = Stroke(0.8f.dp.toPx()))
                }
                drawLine(underBg, Offset(wxL + wR * 0.4f, wCY), Offset(wxR - wR * 0.4f, wCY), 2.dp.toPx())
                drawLine(railC, Offset(0f, railY), Offset(w, railY), 1.5f.dp.toPx())
            }
        }

        Spacer(Modifier.height(5.dp))
        Text(
            coach.code,
            fontSize   = 12.sp,
            fontWeight = if (coach.highlighted) FontWeight.ExtraBold else FontWeight.Medium,
            color      = if (coach.highlighted) AppOrange else Zinc500
        )
        Text(
            coach.typeName,
            fontSize   = 10.sp,
            fontWeight = FontWeight.Medium,
            color      = if (coach.highlighted) AppOrange.copy(0.70f) else Zinc400
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Affiliate banners
// ─────────────────────────────────────────────────────────────────────────────

private data class AffiliateBanner(
    val brand: String, val headline: String, val subtext: String, val cta: String,
    val bgLight: Color, val bgDeep: Color, val accent: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val playStoreQuery: String
)

private val affiliateBanners = listOf(
    AffiliateBanner("IRCTC",      "Book Train Tickets",  "Tatkal, general & waiting list",  "Book Now →", Color(0xFFDEECFA), Color(0xFFC4D9F4), Color(0xFF1565C0), Icons.Outlined.DirectionsRailway, "irctc rail connect"),
    AffiliateBanner("OLA CABS",   "₹150 Off First Ride", "AC cab at your doorstep",          "Ride Now →", Color(0xFFDEF5E7), Color(0xFFC2E8CF), Color(0xFF1E7B40), Icons.Outlined.LocalTaxi,         "ola cabs"),
    AffiliateBanner("GOIBIBO",    "Hotels – 60% Off",    "Stay deals across India",          "Explore →",  Color(0xFFFDE8EF), Color(0xFFF9CDDB), Color(0xFFD81B60), Icons.Outlined.Hotel,              "goibibo hotels"),
    AffiliateBanner("PHONEPE",    "Instant Recharge",    "All networks · No extra charge",   "Recharge →", Color(0xFFEDE8FD), Color(0xFFD9CEFA), Color(0xFF6930C3), Icons.Outlined.PhoneAndroid,       "phonepe"),
    AffiliateBanner("MAKEMYTRIP", "Flights from ₹1,499", "Compare 500+ airlines instantly", "Search →",   Color(0xFFFEF0E4), Color(0xFFFDDAC2), Color(0xFFD64C15), Icons.Outlined.Flight,             "makemytrip flights"),
)

@Composable
private fun AffiliateBannersSection() {
    val listState   = rememberLazyListState()
    val currentPage by remember { derivedStateOf { listState.firstVisibleItemIndex } }

    LaunchedEffect(Unit) {
        while (true) { delay(3000); listState.animateScrollToItem((listState.firstVisibleItemIndex + 1) % affiliateBanners.size) }
    }

    val s = LocalStrings.current
    Column {
        Row(
            modifier              = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(s.partnerOffers, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = AppMuted)
            Box(modifier = Modifier.clip(RoundedCornerShape(50.dp)).background(Zinc100).padding(horizontal = 10.dp, vertical = 4.dp)) {
                Text(s.ads, fontSize = 10.sp, color = Zinc500, fontWeight = FontWeight.Medium)
            }
        }
        LazyRow(state = listState, contentPadding = PaddingValues(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(affiliateBanners) { AffiliateBannerCard(it) }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            affiliateBanners.indices.forEach { i ->
                val isActive = i == currentPage
                val dotWidth by animateFloatAsState(targetValue = if (isActive) 20f else 6f, animationSpec = tween(250), label = "dot_$i")
                Box(modifier = Modifier.padding(horizontal = 3.dp).size(width = dotWidth.dp, height = 6.dp).clip(RoundedCornerShape(50.dp))
                    .background(if (isActive) affiliateBanners[currentPage].accent else Zinc200))
            }
        }
        Spacer(Modifier.height(6.dp))
    }
}

@Composable
private fun AffiliateBannerCard(banner: AffiliateBanner) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .width(272.dp)
            .height(110.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(listOf(banner.bgLight, banner.bgDeep)))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null
            ) {
                val uri = Uri.parse("market://search?q=${Uri.encode(banner.playStoreQuery)}")
                try { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                catch (_: Exception) {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://play.google.com/store/search?q=${Uri.encode(banner.playStoreQuery)}"))
                    )
                }
            }
    ) {
        Box(modifier = Modifier.width(4.dp).fillMaxHeight().background(banner.accent.copy(alpha = 0.65f)).align(Alignment.CenterStart))
        Icon(banner.icon, null, tint = banner.accent.copy(alpha = 0.10f), modifier = Modifier.size(96.dp).align(Alignment.CenterEnd).padding(end = 10.dp))
        Row(modifier = Modifier.fillMaxSize().padding(start = 18.dp, end = 14.dp, top = 16.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(banner.brand, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.8.sp, color = banner.accent.copy(alpha = 0.75f))
                Spacer(Modifier.height(3.dp))
                Text(banner.headline, fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color(0xFF1A1A2E), maxLines = 1)
                Text(banner.subtext, fontSize = 11.sp, color = Color(0xFF5A607A), maxLines = 1)
                Spacer(Modifier.height(10.dp))
                Box(modifier = Modifier.clip(RoundedCornerShape(50.dp)).background(banner.accent).padding(horizontal = 13.dp, vertical = 5.dp)) {
                    Text(banner.cta, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Quick Services
// ─────────────────────────────────────────────────────────────────────────────

private data class QuickService(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val bgColor: Color,
    val iconColor: Color,
    val playStoreQuery: String
)

private val quickServices = listOf(
    QuickService("Recharge", Icons.Outlined.PhoneAndroid, Color(0xFF5C35CC).copy(0.12f), Color(0xFF5C35CC), "mobile recharge india"),
    QuickService("Hotels",   Icons.Outlined.Hotel,        Color(0xFFC0182E).copy(0.12f), Color(0xFFC0182E), "hotel booking india"),
    QuickService("Cab",      Icons.Outlined.LocalTaxi,    Color(0xFF1A7A40).copy(0.12f), Color(0xFF1A7A40), "ola cabs"),
    QuickService("Flights",  Icons.Outlined.Flight,       Color(0xFF0277BD).copy(0.12f), Color(0xFF0277BD), "flight booking india"),
    QuickService("Bus",      Icons.Outlined.DirectionsBus, Color(0xFFD84A12).copy(0.12f), Color(0xFFD84A12), "redbus india"),
)

@Composable
private fun QuickServicesSection() {
    val s = LocalStrings.current
    Column(modifier = Modifier.padding(horizontal = 18.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(s.quickServices, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = AppMuted)
            Text(s.viewAll, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = AppTitle)
        }
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = AppCard),
            border = BorderStroke(1.dp, AppBorder), elevation = CardDefaults.cardElevation(2.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp, horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                quickServices.forEach { QuickServiceItem(it) }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun QuickServiceItem(service: QuickService) {
    val context = LocalContext.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null
            ) {
                val uri = Uri.parse("market://search?q=${Uri.encode(service.playStoreQuery)}")
                try { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                catch (_: Exception) {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://play.google.com/store/search?q=${Uri.encode(service.playStoreQuery)}"))
                    )
                }
            }
            .padding(horizontal = 4.dp)
    ) {
        Box(modifier = Modifier.size(50.dp).clip(RoundedCornerShape(14.dp)).background(service.bgColor), contentAlignment = Alignment.Center) {
            Icon(service.icon, service.label, tint = service.iconColor, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.height(7.dp))
        Text(service.label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = AppTitle)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  PNR Boarding-pass card  (home screen, shown when PNR result arrives)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PNRBoardingCard(
    pnr:     PNRStatus,
    isSaved: Boolean       = false,
    onSave:  () -> Unit    = {},
    onTrack: () -> Unit    = {}
) {
    var expanded by remember { mutableStateOf(true) }

    val firstPax = pnr.passengers.firstOrNull()
    val coachId  = firstPax?.let { it.currentCoachId.ifBlank { it.bookingCoachId } } ?: ""
    val berthNo  = firstPax?.let { if (it.currentBerthNo > 0) it.currentBerthNo else it.bookingBerthNo } ?: 0
    val berthCd  = firstPax?.let { it.currentBerthCode.ifBlank { it.bookingBerthCode } } ?: ""
    val paxStatus = firstPax?.let { it.currentStatus.ifBlank { it.bookingStatus } } ?: pnr.currentStatus

    val seatDisplay = buildString {
        val st = paxStatus.substringBefore("/").trim().take(3)
        if (st.isNotBlank()) append(st)
        if (coachId.isNotBlank()) append("/$coachId")
        if (berthNo > 0) append("/$berthNo")
        if (berthCd.isNotBlank()) append("/$berthCd")
    }.ifBlank { "–" }

    val isConfirmed = paxStatus.contains("CNF", ignoreCase = true)
    val isWaiting   = paxStatus.contains("WL",  ignoreCase = true)
    val statusColor = when { isConfirmed -> AppGreen; isWaiting -> AppOrange; else -> AppMuted }

    // Barcode seed — split PNR digits into groups for visual display
    val pnrDigits   = pnr.pnrNumber.filter { it.isDigit() }
    val barcodeLeft = if (pnrDigits.length >= 5) "${pnrDigits.take(1)}${pnrDigits.drop(1).take(4).chunked(2).joinToString("")}" else pnrDigits
    val barcodeRight = if (pnrDigits.length >= 9) pnrDigits.takeLast(4) else ""

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column {

            // ── Header row ────────────────────────────────────────────────
            Row(
                modifier          = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier         = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(AppNavy),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.DirectionsRailway, null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("PNR ${pnr.pnrNumber}", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = AppNavy)
                    Text(
                        buildString {
                            append(pnr.trainName)
                            if (pnr.trainNumber.isNotBlank()) append(" · ${pnr.trainNumber}")
                        },
                        fontSize = 11.sp, color = AppMuted, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                        null, tint = AppMuted, modifier = Modifier.size(20.dp)
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFF0F0F0))

            // ── Route section ─────────────────────────────────────────────
            Row(
                modifier          = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Source
                Column {
                    Text(pnr.from, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = AppNavy)
                    Text(
                        stationNameFor(pnr.from).let { if (it.length > 14) it.take(14) + "…" else it },
                        fontSize = 11.sp, color = AppMuted
                    )
                }

                // Connector
                Row(
                    modifier          = Modifier.weight(1f).padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(AppNavy.copy(0.30f)))
                    Canvas(Modifier.weight(1f).height(1.dp)) {
                        val dotW = 6.dp.toPx(); val gap = 5.dp.toPx()
                        var x = 0f
                        while (x < size.width - dotW) {
                            drawLine(Color(0xFFD1D5DB), Offset(x, 0f), Offset(x + dotW, 0f), 1.5f.dp.toPx())
                            x += dotW + gap
                        }
                    }
                    Box(
                        modifier         = Modifier.size(26.dp).clip(CircleShape).background(AppNavy),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.DirectionsRailway, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                    Canvas(Modifier.weight(1f).height(1.dp)) {
                        val dotW = 6.dp.toPx(); val gap = 5.dp.toPx()
                        var x = 0f
                        while (x < size.width - dotW) {
                            drawLine(AppOrange.copy(0.50f), Offset(x, 0f), Offset(x + dotW, 0f), 1.5f.dp.toPx())
                            x += dotW + gap
                        }
                    }
                    Box(Modifier.size(8.dp).clip(CircleShape).background(AppOrange))
                }

                // Destination
                Column(horizontalAlignment = Alignment.End) {
                    Text(pnr.to, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = AppOrange)
                    Text(
                        stationNameFor(pnr.to).let { if (it.length > 14) it.take(14) + "…" else it },
                        fontSize = 11.sp, color = AppOrange.copy(0.75f), textAlign = TextAlign.End
                    )
                }
            }

            // ── Perforated divider ─────────────────────────────────────────
            Canvas(Modifier.fillMaxWidth().height(20.dp)) {
                val cR = 10.dp.toPx()
                drawCircle(Color(0xFFF5F5F5), cR, Offset(-cR * 0.5f, cR))
                drawCircle(Color(0xFFF5F5F5), cR, Offset(size.width + cR * 0.5f, cR))
                val dotW = 8.dp.toPx(); val gap = 6.dp.toPx(); var x = cR * 1.5f
                while (x < size.width - cR) {
                    drawLine(Color(0xFFE5E7EB), Offset(x, cR), Offset(x + dotW, cR), 1.5f.dp.toPx())
                    x += dotW + gap
                }
            }

            // ── Journey date row ───────────────────────────────────────────
            if (pnr.dateOfJourney.isNotBlank() || pnr.numberOfPassenger > 0) {
                val sBc = LocalStrings.current
                Row(
                    modifier              = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Column {
                        Text(sBc.departure, fontSize = 9.sp, color = AppMuted, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                        Spacer(Modifier.height(3.dp))
                        Text(pnr.dateOfJourney.ifBlank { "–" }, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AppNavy)
                    }
                    if (pnr.numberOfPassenger > 0) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(sBc.passengers, fontSize = 9.sp, color = AppMuted, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                            Spacer(Modifier.height(3.dp))
                            Text("${pnr.numberOfPassenger}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AppNavy, textAlign = TextAlign.End)
                        }
                    }
                }
                HorizontalDivider(color = Color(0xFFF0F0F0), modifier = Modifier.padding(horizontal = 20.dp))
            }

            // ── Expandable detail section ──────────────────────────────────
            if (expanded) {

                // 2-column label+value grid
                val sGrid = LocalStrings.current
                Column(Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
                    PNRGridRow(
                        label1 = sGrid.classLabel,  value1 = pnr.journeyClass.ifBlank { "–" },
                        label2 = sGrid.seatLabel,   value2 = seatDisplay
                    )
                    Spacer(Modifier.height(14.dp))
                    PNRGridRow(
                        label1 = sGrid.chartLabel,  value1 = pnr.chartStatus.ifBlank { "Not Prepared" },
                        label2 = sGrid.pnrLabel,    value2 = pnr.pnrNumber
                    )
                    Spacer(Modifier.height(14.dp))
                    PNRGridRow(
                        label1 = sGrid.trainLabel,  value1 = "${pnr.trainName}${if (pnr.trainNumber.isNotBlank()) " · ${pnr.trainNumber}" else ""}",
                        label2 = sGrid.statusLabel, value2 = paxStatus.ifBlank { "–" },
                        value2Color = statusColor
                    )
                    if (pnr.quota.isNotBlank() || pnr.ticketFare > 0) {
                        Spacer(Modifier.height(14.dp))
                        PNRGridRow(
                            label1 = sGrid.quotaLabel, value1 = pnr.quota.ifBlank { "–" },
                            label2 = sGrid.fareLabel,  value2 = if (pnr.ticketFare > 0) "₹${pnr.ticketFare}" else "–"
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFFF0F0F0), modifier = Modifier.padding(horizontal = 20.dp))

                // Decorative barcode
                Canvas(
                    modifier = Modifier.fillMaxWidth().height(72.dp).padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    val seed = pnr.pnrNumber.hashCode().toLong()
                    val totalW = size.width; val barH = size.height * 0.75f
                    var x = 0f; var i = 0
                    while (x < totalW) {
                        val barW = ((((seed * 1103515245L + 12345L + i * 6364136223846793005L) ushr 33) and 0xF) + 2f).toFloat().coerceIn(2f, 8f).dp.toPx()
                        val isOrange = (i % 7 == 3 || i % 11 == 5)
                        drawRect(
                            color   = if (isOrange) AppOrange else AppNavy,
                            topLeft = Offset(x, (size.height - barH) / 2f),
                            size    = Size(barW, barH)
                        )
                        x += barW + 1.5f.dp.toPx()
                        i++
                    }
                }

                // Barcode text label
                val bcLabel = buildString {
                    if (pnrDigits.length >= 5) {
                        append(pnrDigits.take(1))
                        append(pnrDigits.drop(1).take(4).chunked(2).joinToString(""))
                        append(" ")
                        append(pnrDigits.drop(5).take(3))
                        if (pnrDigits.length > 8) { append(" · "); append("B"); append(pnrDigits.drop(8)) }
                    } else append(pnrDigits)
                }
                Text(bcLabel, fontSize = 10.sp, color = AppMuted, letterSpacing = 1.sp,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), textAlign = TextAlign.Center)

                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = Color(0xFFF0F0F0))

                // ── Action buttons ────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    val sAct = LocalStrings.current
                    // Hide / Show ticket toggle
                    OutlinedButton(
                        onClick  = { expanded = false },
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(12.dp),
                        colors   = ButtonDefaults.outlinedButtonColors(contentColor = AppNavy),
                        border   = BorderStroke(1.dp, AppNavy.copy(0.35f))
                    ) {
                        Icon(Icons.Outlined.ConfirmationNumber, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(sAct.hideTicket, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }

                    // Track button
                    Button(
                        onClick  = onTrack,
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(12.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = AppOrange)
                    ) {
                        Icon(Icons.Outlined.TrackChanges, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(sAct.trackArrow, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }

                    // Bookmark
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSaved) AppGreen.copy(0.12f) else Color(0xFFF5F5F5))
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onSave),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isSaved) Icons.Outlined.BookmarkAdded else Icons.Outlined.BookmarkBorder,
                            null, tint = if (isSaved) AppGreen else AppMuted, modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PNRGridRow(
    label1: String, value1: String,
    label2: String, value2: String,
    value2Color: Color = AppNavy
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f)) {
            Text(label1, fontSize = 10.sp, color = AppMuted, fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp)
            Spacer(Modifier.height(2.dp))
            Text(value1, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AppNavy, maxLines = 2)
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
            Text(label2, fontSize = 10.sp, color = AppMuted, fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp)
            Spacer(Modifier.height(2.dp))
            Text(value2, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = value2Color, textAlign = TextAlign.End, maxLines = 2)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  PNR Status card — full detail
// ─────────────────────────────────────────────────────────────────────────────

private fun berthFullName(code: String) = when (code.uppercase()) {
    "LB"  -> "Lower Berth"
    "MB"  -> "Middle Berth"
    "UB"  -> "Upper Berth"
    "SL"  -> "Side Lower"
    "SU"  -> "Side Upper"
    "WS"  -> "Window Seat"
    "AN"  -> "Aisle (No Pref)"
    else  -> code
}

@Composable
fun PNRStatusCard(
    pnr:          PNRStatus,
    isSaved:      Boolean     = false,
    onSaveTicket: (() -> Unit)? = null,
    onViewRoute:  (() -> Unit)? = null
) {
    // Derive overall status from passenger list (first passenger is representative)
    val firstPax     = pnr.passengers.firstOrNull()
    val overallStatus = firstPax?.currentStatus?.ifBlank { firstPax.bookingStatus }
        ?: pnr.currentStatus
    val isConfirmed  = overallStatus.contains("CNF", ignoreCase = true)
    val isWaiting    = overallStatus.contains("WL",  ignoreCase = true)
    val statusColor  = when { isConfirmed -> AppGreen; isWaiting -> AppOrange; else -> AppMuted }

    val chartReady   = pnr.chartStatus.contains("Prepared", ignoreCase = true)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

        // ── Header card (navy) ────────────────────────────────────────────────
        Card(
            modifier  = Modifier.fillMaxWidth(),
            shape     = RoundedCornerShape(20.dp),
            colors    = CardDefaults.cardColors(containerColor = AppNavy),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(Modifier.padding(20.dp)) {

                // PNR row
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.Top
                ) {
                    Column {
                        Text(LocalStrings.current.pnrStatus, fontSize = 9.sp, fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp, color = Color.White.copy(0.50f))
                        Spacer(Modifier.height(2.dp))
                        Text(pnr.pnrNumber, fontSize = 26.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(50.dp))
                                .background(statusColor.copy(0.20f))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                overallStatus.ifBlank { "UNKNOWN" },
                                fontSize = 12.sp, fontWeight = FontWeight.Bold, color = statusColor
                            )
                        }
                        if (pnr.chartStatus.isNotBlank()) {
                            Box(
                                modifier = Modifier.clip(RoundedCornerShape(50.dp))
                                    .background(if (chartReady) AppGreen.copy(0.15f) else Color.White.copy(0.10f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    pnr.chartStatus,
                                    fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                                    color = if (chartReady) AppGreen else Color.White.copy(0.65f)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Train name + meta
                Text(pnr.trainName.localTrainName(), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier              = Modifier.padding(top = 2.dp)
                ) {
                    Text(pnr.trainNumber, fontSize = 12.sp, color = Color.White.copy(0.55f))
                    if (pnr.journeyClass.isNotBlank()) {
                        Box(Modifier.size(3.dp).clip(CircleShape).background(Color.White.copy(0.30f)))
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(4.dp))
                                .background(AppOrange.copy(0.25f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(pnr.journeyClass, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AppOrange)
                        }
                    }
                    if (pnr.quota.isNotBlank()) {
                        Box(Modifier.size(3.dp).clip(CircleShape).background(Color.White.copy(0.30f)))
                        Text(pnr.quota, fontSize = 11.sp, color = Color.White.copy(0.55f))
                    }
                }

                Spacer(Modifier.height(18.dp))

                // Route row
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    val sFromTo = LocalStrings.current
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(pnr.from, fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color.White)
                        Text(sFromTo.fromLabel, fontSize = 9.sp, color = Color.White.copy(0.45f), letterSpacing = 1.sp)
                    }
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Canvas(Modifier.fillMaxWidth().height(1.dp)) {
                            drawLine(Color.White.copy(0.20f), Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx())
                        }
                        Icon(Icons.Outlined.DirectionsRailway, null, tint = AppOrange,
                            modifier = Modifier.clip(CircleShape).background(AppNavy).padding(5.dp).size(20.dp))
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(pnr.to, fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color.White)
                        Text(sFromTo.toLabel, fontSize = 9.sp, color = Color.White.copy(0.45f), letterSpacing = 1.sp)
                    }
                }

                // Boarding point (if different from source)
                if (pnr.boardingPoint.isNotBlank() && pnr.boardingPoint != pnr.from) {
                    Spacer(Modifier.height(10.dp))
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Outlined.MyLocation, null, tint = AppOrange, modifier = Modifier.size(13.dp))
                        Text(
                            "Boarding at ${pnr.boardingPoint}",
                            fontSize = 12.sp, color = AppOrange, fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Date of journey
                if (pnr.dateOfJourney.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Outlined.CalendarToday, null, tint = Color.White.copy(0.50f), modifier = Modifier.size(13.dp))
                        Text(
                            pnr.dateOfJourney.substringBefore(" "),
                            fontSize = 12.sp, color = Color.White.copy(0.65f)
                        )
                        if (pnr.distance > 0) {
                            Box(Modifier.size(3.dp).clip(CircleShape).background(Color.White.copy(0.25f)))
                            Icon(Icons.Outlined.Straighten, null, tint = Color.White.copy(0.40f), modifier = Modifier.size(13.dp))
                            Text("${pnr.distance} km", fontSize = 12.sp, color = Color.White.copy(0.55f))
                        }
                    }
                }
            }
        }

        // ── Passenger details ─────────────────────────────────────────────────
        if (pnr.passengers.isNotEmpty()) {
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(16.dp),
                colors    = CardDefaults.cardColors(containerColor = AppCard),
                border    = BorderStroke(1.dp, AppBorder),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
                    Text(
                        "PASSENGERS  ·  ${pnr.passengers.size}",
                        fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp, color = AppMuted
                    )
                    Spacer(Modifier.height(12.dp))
                    pnr.passengers.forEachIndexed { index, p ->
                        if (index > 0) HorizontalDivider(color = AppBorder, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 10.dp))
                        PassengerRow(p)
                    }
                }
            }
        }

        // ── Journey meta row ──────────────────────────────────────────────────
        val showMeta = pnr.bookingFare > 0 || pnr.ticketFare > 0 || pnr.reservationUpto.isNotBlank()
        if (showMeta) {
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(16.dp),
                colors    = CardDefaults.cardColors(containerColor = AppCard),
                border    = BorderStroke(1.dp, AppBorder),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Row(
                    modifier              = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    if (pnr.ticketFare > 0)
                        PNRMetaItem("FARE", "₹${pnr.ticketFare}")
                    if (pnr.reservationUpto.isNotBlank())
                        PNRMetaItem("RESERVED UPTO", pnr.reservationUpto)
                    if (pnr.boardingPoint.isNotBlank())
                        PNRMetaItem("BOARDING", pnr.boardingPoint)
                }
            }
        }

        // ── Info messages ─────────────────────────────────────────────────────
        if (pnr.informationMessage.isNotEmpty()) {
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(14.dp),
                colors    = CardDefaults.cardColors(containerColor = Color(0xFFFFF7ED)),
                border    = BorderStroke(1.dp, AppOrange.copy(0.20f)),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    pnr.informationMessage.forEach { msg ->
                        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Outlined.Info, null, tint = AppOrange, modifier = Modifier.size(14.dp).padding(top = 2.dp))
                            Text(msg, fontSize = 12.sp, color = Color(0xFF92400E), lineHeight = 18.sp)
                        }
                    }
                }
            }
        }

        // ── Action buttons ────────────────────────────────────────────────────
        if (onViewRoute != null) {
            val sPnrAct = LocalStrings.current
            Button(
                onClick  = onViewRoute,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = AppOrange)
            ) {
                Icon(Icons.Outlined.Map, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(sPnrAct.viewRouteSetAlerts, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (onSaveTicket != null) {
            val sSave = LocalStrings.current
            OutlinedButton(
                onClick  = onSaveTicket,
                enabled  = !isSaved,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.outlinedButtonColors(
                    contentColor         = if (isSaved) AppGreen else AppNavy,
                    disabledContentColor = AppGreen
                ),
                border = BorderStroke(1.dp, if (isSaved) AppGreen.copy(0.4f) else AppBorder)
            ) {
                Icon(
                    if (isSaved) Icons.Outlined.BookmarkAdded else Icons.Outlined.BookmarkAdd,
                    null, modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (isSaved) sSave.ticketSaved else sSave.saveTicket,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ── Per-passenger row ─────────────────────────────────────────────────────────

@Composable
private fun PassengerRow(p: Passenger) {
    val coach  = p.currentCoachId.ifBlank { p.bookingCoachId }
    val berth  = if (p.currentBerthNo > 0) p.currentBerthNo else p.bookingBerthNo
    val bCode  = p.currentBerthCode.ifBlank { p.bookingBerthCode }
    val status = p.currentStatus.ifBlank { p.bookingStatus }

    val isConf = status.contains("CNF", ignoreCase = true)
    val isWL   = status.contains("WL",  ignoreCase = true)
    val pColor = when { isConf -> AppGreen; isWL -> AppOrange; else -> AppMuted }

    Row(
        modifier          = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left: passenger number + berth type label
        Column {
            Text("Passenger ${p.serialNumber}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AppTitle)
            if (bCode.isNotBlank()) {
                Text(berthFullName(bCode), fontSize = 12.sp, color = AppMuted)
            }
        }

        // Center: Coach + Berth number pills
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            if (coach.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(AppNavy.copy(0.08f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(coach, fontSize = 16.sp, fontWeight = FontWeight.Black, color = AppNavy)
                        Text(LocalStrings.current.coachLabel, fontSize = 8.sp, color = AppMuted, letterSpacing = 0.8.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            if (berth > 0) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(AppOrange.copy(0.10f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$berth", fontSize = 16.sp, fontWeight = FontWeight.Black, color = AppOrange)
                        Text(LocalStrings.current.berthLabel, fontSize = 8.sp, color = AppOrange.copy(0.70f), letterSpacing = 0.8.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Right: status chip
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(pColor.copy(0.12f))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(status.ifBlank { "–" }, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = pColor)
        }
    }
}

@Composable
private fun PNRMetaItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Black, color = AppNavy)
        Text(label, fontSize = 9.sp, color = AppMuted, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Train result card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun TrainResultCard(train: Train, onTap: () -> Unit = {}) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppCard), border = BorderStroke(1.dp, AppBorder),
        elevation = CardDefaults.cardElevation(2.dp), onClick = onTap) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(AppOrange.copy(alpha = 0.10f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.DirectionsRailway, null, tint = AppOrange, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(train.name.localTrainName(), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF1565C0), maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (train.source.isNotEmpty() && train.destination.isNotEmpty()) {
                    Text("${train.source} → ${train.destination}", fontSize = 12.sp, color = Zinc500)
                }
                Text(train.number, fontSize = 12.sp, color = AppMuted, fontWeight = FontWeight.Medium)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(LocalStrings.current.routeLabel, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = AppOrange)
                Icon(Icons.Outlined.ChevronRight, null, tint = AppOrange, modifier = Modifier.size(16.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Route detail view — light background, alert buttons on future stops
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun RouteDetailView(
    train:        Train,
    schedule:     List<Station>,
    isLoading:    Boolean,
    currentIndex: Int,
    alerted:      Set<String>,
    onAlertTap:   (Station) -> Unit,
    onBack:       () -> Unit
) {
    LazyColumn(
        modifier       = Modifier.fillMaxSize().background(AppBackground),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            Row(
                Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "Back", tint = AppNavy)
                }
                Spacer(Modifier.width(4.dp))
                Column(Modifier.weight(1f)) {
                    Text(train.name.localTrainName(), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = AppNavy, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${train.number}  ·  ${train.source.ifBlank { "–" }} → ${train.destination.ifBlank { "–" }}", fontSize = 12.sp, color = AppMuted)
                }
            }
            HorizontalDivider(color = AppBorder, thickness = 0.5.dp)
        }

        if (isLoading) {
            item {
                Box(Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AppOrange, modifier = Modifier.size(36.dp))
                }
            }
            return@LazyColumn
        }

        item {
            val sStream = LocalStrings.current
            Spacer(Modifier.height(8.dp))
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp)) {
                Text(sStream.theStream, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp, color = Color(0xFF005BA2))
                Text(sStream.stationsAlongRoute, fontSize = 22.sp, fontWeight = FontWeight.Black, color = AppNavy)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    val sStr = LocalStrings.current
                    Text("${schedule.size} ${sStr.stopsLabel}", fontSize = 13.sp, color = AppMuted)
                    Text("·", fontSize = 13.sp, color = AppMuted)
                    Text("${sStr.tapSetAlertPre} ", fontSize = 13.sp, color = AppMuted)
                    Icon(Icons.Outlined.NotificationsNone, null, tint = AppMuted, modifier = Modifier.size(13.dp))
                    Text(" ${sStr.tapSetAlertPost}", fontSize = 13.sp, color = AppMuted)
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        if (schedule.isEmpty() && !isLoading) {
            item {
                Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    Text("Schedule not available for this train.", fontSize = 14.sp, color = AppMuted, fontWeight = FontWeight.Medium)
                }
            }
            return@LazyColumn
        }

        itemsIndexed(schedule) { index, station ->
            SearchStationRow(
                station      = station,
                index        = index,
                currentIndex = currentIndex,
                isLast       = index == schedule.lastIndex,
                isAlerted    = station.code in alerted,
                onAlertTap   = { onAlertTap(station) }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Station row (with alert bell for future stops)
// ─────────────────────────────────────────────────────────────────────────────

private val SearchPastColor    = Color(0xFF38BDF8)
private val SearchCurrentColor = AppOrange
private val SearchFutureColor  = Color(0xFFCDD5E0)

@Composable
private fun SearchStationRow(
    station:      Station,
    index:        Int,
    currentIndex: Int,
    isLast:       Boolean,
    isAlerted:    Boolean = false,
    onAlertTap:   (() -> Unit)? = null
) {
    val isPast    = index < currentIndex
    val isCurrent = index == currentIndex
    val isFuture  = index > currentIndex

    val nameColor = if (isPast) Color(0xFFA0AEC0) else AppNavy
    val metaColor = if (isPast) Color(0xFFCDD5E0) else AppMuted
    val timeColor = if (isCurrent) AppOrange else if (isPast) Color(0xFFA0AEC0) else AppNavy
    val lineColor = if (index < currentIndex) SearchPastColor else Color(0xFFE2E8F0)

    val alertColor by animateColorAsState(
        targetValue = if (isAlerted) AppOrange else AppMuted,
        animationSpec = tween(250), label = "alertColor"
    )

    val rawTime     = if (index == 0) station.departure else station.arrival
    val displayTime = rawTime.replace(".", ":").trim()
    val dayNum      = station.day.trim().toIntOrNull() ?: 1
    val daySuffix   = if (dayNum > 1) "+${dayNum - 1}" else ""

    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(28.dp)) {
            when {
                isCurrent -> Box(
                    Modifier.size(22.dp).clip(CircleShape).background(SearchCurrentColor.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) { Box(Modifier.size(14.dp).clip(CircleShape).background(SearchCurrentColor)) }
                isFuture  -> Box(
                    Modifier.size(14.dp).clip(CircleShape).background(Color.White).drawBehind {
                        drawCircle(SearchFutureColor, size.minDimension / 2f, style = Stroke(width = 2.dp.toPx()))
                    }
                )
                else      -> Box(Modifier.size(14.dp).clip(CircleShape).background(SearchPastColor))
            }
            if (!isLast) Box(Modifier.width(2.dp).height(72.dp).background(lineColor))
        }

        Spacer(Modifier.width(14.dp))

        Row(
            Modifier.weight(1f).padding(bottom = if (isLast) 4.dp else 0.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.Top
        ) {
            Column(Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    station.name,
                    fontSize   = if (isCurrent) 17.sp else 16.sp,
                    fontWeight = if (isCurrent || isLast || index == 0) FontWeight.Bold else FontWeight.Normal,
                    color      = nameColor, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                    Text(station.code, fontSize = 12.sp, color = metaColor, fontWeight = FontWeight.Medium)
                    if (station.halt > 0) { Text("·", fontSize = 11.sp, color = metaColor); Text("${station.halt}m halt", fontSize = 12.sp, color = metaColor) }
                    if (station.distance.isNotBlank() && station.distance != "0") { Text("·", fontSize = 11.sp, color = metaColor); Text("${station.distance} km", fontSize = 12.sp, color = metaColor) }
                }
                if (isCurrent) {
                    val sCurr = LocalStrings.current
                    Spacer(Modifier.height(5.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(AppOrange.copy(alpha = 0.12f)).padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Box(Modifier.size(7.dp).clip(CircleShape).background(AppOrange))
                        Text(sCurr.trainIsHereNow, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = AppOrange)
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                // Alert bell — future stops only
                if (isFuture && onAlertTap != null) {
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(50.dp))
                            .background(if (isAlerted) AppOrange.copy(0.10f) else Color(0xFFF1F5F9))
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onAlertTap)
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            if (isAlerted) Icons.Filled.Notifications else Icons.Outlined.NotificationsNone,
                            null, tint = alertColor, modifier = Modifier.size(14.dp)
                        )
                        Text(LocalStrings.current.alertLabel, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = alertColor)
                    }
                    Spacer(Modifier.height(4.dp))
                }

                if (displayTime.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(displayTime, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = timeColor)
                        if (daySuffix.isNotBlank()) Text(daySuffix, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6366F1))
                    }
                }
            }
        }
    }
    if (!isLast) Spacer(Modifier.height(4.dp))
}

// ─────────────────────────────────────────────────────────────────────────────
//  Alert Picker Sheet — light / white theme
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlertPickerSheet(
    station:     Station,
    trainNumber: String,
    onConfirm:   (Int) -> Unit,
    onDismiss:   () -> Unit
) {
    val sheetState  = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedMin by remember { mutableIntStateOf(15) }
    val options     = listOf(15, 30, 45, 60)

    val etaDisplay = station.arrival
        .ifBlank { station.departure }
        .replace(".", ":")
        .trim()
        .ifBlank { "–" }

    val sAp = LocalStrings.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = Color.White,
        dragHandle       = { BottomSheetDefaults.DragHandle(color = Zinc300) }
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Bell icon — orange on light background
            Box(
                Modifier.size(56.dp).clip(CircleShape).background(AppOrange.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Notifications, null, tint = AppOrange, modifier = Modifier.size(28.dp))
            }

            Spacer(Modifier.height(12.dp))
            Text(sAp.arrivalAlert, fontSize = 22.sp, fontWeight = FontWeight.Black, color = AppNavy)
            Text(station.name, fontSize = 14.sp, color = AppBody, textAlign = TextAlign.Center)

            if (trainNumber.isNotBlank() || etaDisplay != "–") {
                Text(
                    buildString {
                        if (trainNumber.isNotBlank()) append("Train $trainNumber")
                        if (etaDisplay != "–") { if (trainNumber.isNotBlank()) append(" · "); append("ETA $etaDisplay") }
                    },
                    fontSize = 13.sp, color = AppOrange, textAlign = TextAlign.Center,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(24.dp))
            Text(sAp.wakeMeBeforeArrival, fontSize = 10.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.Bold, color = AppMuted)
            Spacer(Modifier.height(12.dp))

            // Minute pills
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { mins ->
                    val isSelected = mins == selectedMin
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) AppNavy else Zinc100)
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { selectedMin = mins }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "$mins",
                                fontSize   = 20.sp,
                                fontWeight = FontWeight.Black,
                                color      = if (isSelected) Color.White else AppNavy
                            )
                            Text(
                                sAp.minLabel,
                                fontSize = 11.sp,
                                color    = if (isSelected) Color.White.copy(alpha = 0.75f) else AppMuted
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Set Alert button
            Button(
                onClick  = { onConfirm(selectedMin) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(16.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = AppOrange)
            ) {
                Icon(Icons.Filled.Notifications, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("${sAp.setAlertMin} $selectedMin ${sAp.minLabel}", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(8.dp))

            // Cancel
            TextButton(
                onClick  = onDismiss,
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape    = RoundedCornerShape(16.dp)
            ) {
                Text(sAp.cancel, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AppMuted)
            }
        }
    }
}

// ── PNR basic card (Free tier) ────────────────────────────────────────────────

@Composable
private fun PNRBasicCard(
    pnr: com.yatrain.app.models.PNRStatus,
    onTrack: () -> Unit,
    onUpgrade: () -> Unit
) {
    val s = LocalStrings.current
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(18.dp),
        colors    = CardDefaults.cardColors(containerColor = AppCard),
        border    = BorderStroke(1.dp, AppBorder),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            // Train name + PNR badge
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(pnr.trainName.ifBlank { pnr.trainNumber }.localTrainName(), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AppTitle)
                    Text("${s.pnrLabel} ${pnr.pnrNumber}", fontSize = 11.sp, color = AppMuted)
                }
                val statusColor = when {
                    pnr.currentStatus.startsWith("CNF", true) -> Color(0xFF22C55E)
                    pnr.currentStatus.startsWith("WL",  true) -> Color(0xFFEF4444)
                    pnr.currentStatus.startsWith("RAC", true) -> Color(0xFFF59E0B)
                    else -> AppMuted
                }
                Box(
                    Modifier.clip(RoundedCornerShape(8.dp)).background(statusColor.copy(0.12f)).padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(pnr.currentStatus.ifBlank { pnr.bookingStatus }, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = statusColor)
                }
            }

            Spacer(Modifier.height(12.dp))

            // From → To
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(pnr.from, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AppNavy)
                }
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    androidx.compose.material3.HorizontalDivider(color = AppBorder, thickness = 1.dp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(pnr.to, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AppNavy)
                }
            }

            if (pnr.dateOfJourney.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(pnr.dateOfJourney, fontSize = 11.sp, color = AppMuted)
            }

            Spacer(Modifier.height(14.dp))

            // Locked details strip
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(12.dp),
                colors    = CardDefaults.cardColors(containerColor = AppNavy.copy(0.06f)),
                border    = BorderStroke(1.dp, AppNavy.copy(0.12f))
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Outlined.Lock, null, tint = AppNavy, modifier = Modifier.size(18.dp))
                    Column(Modifier.weight(1f)) {
                        Text(s.proLockedFeature, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AppNavy)
                        Text(s.freePnrHint, fontSize = 11.sp, color = AppMuted, lineHeight = 15.sp)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Action row
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick  = onTrack,
                    modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(14.dp),
                    border   = BorderStroke(1.dp, AppBorder)
                ) {
                    Icon(Icons.Outlined.Map, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(s.trackArrow, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick  = onUpgrade,
                    modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = AppOrange)
                ) {
                    Icon(Icons.Outlined.Star, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(s.upgradeToPro, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

// ── Generic Pro locked feature card ──────────────────────────────────────────

@Composable
private fun ProLockedCard(
    title: String,
    onUpgrade: () -> Unit,
    modifier: Modifier = Modifier
) {
    val s = LocalStrings.current
    Card(
        modifier  = modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(18.dp),
        colors    = CardDefaults.cardColors(containerColor = AppCard),
        border    = BorderStroke(1.dp, AppNavy.copy(0.15f)),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(AppNavy.copy(0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Lock, null, tint = AppNavy, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AppTitle)
                Text(s.proLockedFeature, fontSize = 11.sp, color = AppMuted)
            }
            Button(
                onClick  = onUpgrade,
                shape    = RoundedCornerShape(10.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = AppOrange),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(s.upgradeToPro, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Station typeahead suggestions dropdown
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun StationSuggestionsCard(
    suggestions: List<StationInfo>,
    onSelect:    (StationInfo) -> Unit,
    modifier:    Modifier = Modifier
) {
    Card(
        modifier  = modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        border    = BorderStroke(1.dp, AppBorder),
        elevation = CardDefaults.cardElevation(12.dp)
    ) {
        Column {
            suggestions.forEachIndexed { i, station ->
                if (i > 0) HorizontalDivider(color = AppBorder, thickness = 0.5.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onSelect(station) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier         = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(AppNavy.copy(0.08f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.LocationOn, null, tint = AppNavy, modifier = Modifier.size(18.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(station.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AppNavy, maxLines = 1)
                        Text("${station.code}  ·  ${station.city}", fontSize = 12.sp, color = AppMuted)
                    }
                    Icon(Icons.Outlined.ChevronRight, null, tint = AppOrange, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Station trains section — shown after selecting a station from typeahead
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StationTrainsSection(
    station:    StationInfo,
    trains:     List<LiveTrainEntry>,
    isLoading:  Boolean = false,
    onTrainTap: (LiveTrainEntry) -> Unit,
    onClear:    () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 18.dp)) {
        Row(
            modifier              = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "TRAINS AT STATION",
                    fontSize = 10.sp, fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp, color = AppMuted
                )
                Text(station.name, fontSize = 20.sp, fontWeight = FontWeight.Black, color = AppNavy)
                val subtitle = listOfNotNull(
                    station.code.takeIf { it.isNotBlank() },
                    station.city.takeIf { it.isNotBlank() }
                ).joinToString("  ·  ")
                if (subtitle.isNotBlank())
                    Text(subtitle, fontSize = 12.sp, color = AppMuted)
            }
            IconButton(onClick = onClear) {
                Icon(Icons.Filled.Clear, null, tint = AppMuted)
            }
        }

        if (isLoading) {
            Box(
                Modifier.fillMaxWidth().height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(32.dp), color = AppNavy, strokeWidth = 2.5.dp)
            }
        } else if (trains.isEmpty()) {
            Box(
                Modifier.fillMaxWidth().height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No scheduled trains found for this station.", fontSize = 13.sp, color = AppMuted)
            }
        } else {
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(18.dp),
                colors    = CardDefaults.cardColors(containerColor = AppCard),
                border    = BorderStroke(1.dp, AppBorder),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column {
                    trains.forEachIndexed { i, train ->
                        if (i > 0) HorizontalDivider(
                            color = AppBorder, thickness = 0.5.dp,
                            modifier = Modifier.padding(horizontal = 14.dp)
                        )
                        StationTrainRow(train = train, onTap = { onTrainTap(train) })
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun StationTrainRow(train: LiveTrainEntry, onTap: () -> Unit) {
    val isDelayed   = train.delayMin > 0
    val delayLabel  = if (isDelayed) "+${train.delayMin}m" else "On time"
    val delayColor  = if (isDelayed) Color(0xFFE53935) else Color(0xFF22C55E)
    val depDisplay  = if (train.departure == "Last") train.arrival else train.departure

    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clickable(onClick = onTap)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier         = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(AppOrange.copy(0.10f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.DirectionsRailway, null, tint = AppOrange, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    train.name.localTrainName(),
                    fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1565C0),
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Text(delayLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = delayColor)
            }
            Text(
                "${train.number}  ·  ${train.fromName} → ${train.toName}",
                fontSize = 11.sp, color = AppMuted,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                depDisplay.replace(".", ":"),
                fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AppTitle
            )
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text("Track", fontSize = 11.sp, color = AppOrange, fontWeight = FontWeight.Medium)
                Icon(Icons.Outlined.ChevronRight, null, tint = AppOrange, modifier = Modifier.size(14.dp))
            }
        }
    }
}
