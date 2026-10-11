package com.yatrain.app.ui.screens.track

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yatrain.app.i18n.LocalStrings
import com.yatrain.app.i18n.localTrainName
import com.yatrain.app.models.PNRStatus
import com.yatrain.app.models.Station
import com.yatrain.app.models.Train
import com.yatrain.app.ui.components.HeroSection
import com.yatrain.app.ui.components.ProBannerCard
import com.yatrain.app.ui.screens.search.SearchPill
import com.yatrain.app.ui.screens.subscription.ProPaywallSheet
import com.yatrain.app.ui.screens.subscription.SubscriptionViewModel
import com.yatrain.app.ui.theme.*
import kotlinx.coroutines.launch

// ── Platform metadata ─────────────────────────────────────────────────────────

private data class PlatformInfo(val platforms: Int, val fobs: Int)

private fun platformInfo(code: String) = when (code.uppercase()) {
    "NDLS"         -> PlatformInfo(16, 5)
    "MMCT", "BCT"  -> PlatformInfo(8, 3)
    "BVI"          -> PlatformInfo(6, 2)
    "CSMT", "CST"  -> PlatformInfo(18, 6)
    "ST"           -> PlatformInfo(5, 2)
    "BRC"          -> PlatformInfo(9, 3)
    "RTM"          -> PlatformInfo(4, 2)
    "KOTA"         -> PlatformInfo(5, 2)
    "SWM"          -> PlatformInfo(3, 1)
    "NAD"          -> PlatformInfo(2, 1)
    else           -> PlatformInfo(4, 2)
}

private data class Amenity(val label: String, val location: String, val icon: ImageVector)

private val defaultAmenities = listOf(
    Amenity("Main Exit",      "Near PF 1",       Icons.Outlined.ExitToApp),
    Amenity("Waiting Room",   "Near PF 1",       Icons.Outlined.Weekend),
    Amenity("Drinking Water", "Near PF 1",       Icons.Outlined.WaterDrop),
    Amenity("Restrooms",      "Near PF 2",       Icons.Outlined.Wc),
    Amenity("Food Stalls",    "Near PF 3",       Icons.Outlined.Restaurant),
    Amenity("ATM",            "Main Concourse",  Icons.Outlined.AccountBalance),
    Amenity("Prepaid Taxi",   "Outside PF 1",    Icons.Outlined.LocalTaxi),
)

// ── Main screen ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackScreen(viewModel: TrackViewModel = viewModel()) {
    val query           by viewModel.query.collectAsStateWithLifecycle()
    val trainResult     by viewModel.trainResult.collectAsStateWithLifecycle()
    val schedule        by viewModel.schedule.collectAsStateWithLifecycle()
    val isLoading       by viewModel.isLoading.collectAsStateWithLifecycle()
    val error           by viewModel.error.collectAsStateWithLifecycle()
    val alerted             by viewModel.alertedStations.collectAsStateWithLifecycle()
    val alertPickerStation  by viewModel.alertPickerStation.collectAsStateWithLifecycle()
    val mapStation          by viewModel.mapStation.collectAsStateWithLifecycle()
    val snackMsg        by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val detectedStation      by viewModel.detectedStation.collectAsStateWithLifecycle()
    val liveTrains           by viewModel.liveTrains.collectAsStateWithLifecycle()
    val isLocating           by viewModel.isLocating.collectAsStateWithLifecycle()
    val showingPopularTrains by viewModel.showingPopularTrains.collectAsStateWithLifecycle()
    val stationSuggestions   by viewModel.stationSuggestions.collectAsStateWithLifecycle()
    val trainSuggestions     by viewModel.trainSuggestions.collectAsStateWithLifecycle()
    val currentStop     by viewModel.currentStopIndex.collectAsStateWithLifecycle()
    val isTrainSaved    by viewModel.isTrainSaved.collectAsStateWithLifecycle()
    val pnrContext      by viewModel.pnrContext.collectAsStateWithLifecycle()

    val keyboardController  = LocalSoftwareKeyboardController.current
    val focusManager        = LocalFocusManager.current
    val context             = LocalContext.current
    val snackbarHostState   = remember { SnackbarHostState() }
    val scope               = rememberCoroutineScope()
    var showPaywall         by remember { mutableStateOf(false) }
    val subViewModel: SubscriptionViewModel = viewModel()
    val isPro               by subViewModel.isPro.collectAsStateWithLifecycle()

    val hasResult = trainResult != null || schedule.isNotEmpty()

    val listState = rememberLazyListState()
    val showStatusScrim by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }
    val statusBarScrimColor by animateColorAsState(
        targetValue    = if (showStatusScrim) AppBackground else Color.Transparent,
        animationSpec  = tween(150),
        label          = "statusBarScrim"
    )

    // System back: clear result/suggestions before leaving the tab
    BackHandler(enabled = hasResult || stationSuggestions.isNotEmpty() || trainSuggestions.isNotEmpty()) {
        viewModel.clearResult()
        focusManager.clearFocus()
    }

    // Helper: get location and forward to ViewModel.
    // First tries last-known (fast), then fires requestSingleUpdate for a fresh fix —
    // critical on Samsung where getLastKnownLocation often returns null.
    @Suppress("DEPRECATION", "MissingPermission")
    fun tryGetLocation() {
        try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val cached = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                ?: lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
            if (cached != null) {
                viewModel.onLocationResult(cached.latitude, cached.longitude)
                return
            }
            // No cached fix — request a real one (common on Samsung / first boot)
            val listener = object : android.location.LocationListener {
                override fun onLocationChanged(loc: android.location.Location) {
                    try { lm.removeUpdates(this) } catch (_: Exception) {}
                    viewModel.onLocationResult(loc.latitude, loc.longitude)
                }
                @Deprecated("Deprecated") override fun onStatusChanged(p: String?, s: Int, e: android.os.Bundle?) {}
            }
            val providers = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
                .filter { lm.isProviderEnabled(it) }
            if (providers.isEmpty()) { viewModel.stopLocating(); return }
            providers.forEach { lm.requestSingleUpdate(it, listener, android.os.Looper.getMainLooper()) }
        } catch (_: SecurityException) { viewModel.stopLocating() }
    }

    // Location permission launcher
    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                      perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) tryGetLocation()
    }

    // Snackbar trigger
    LaunchedEffect(snackMsg) {
        snackMsg?.let {
            scope.launch {
                snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
                viewModel.clearSnackbar()
            }
        }
    }

    // Auto-populate station trains on first load — only if permission already granted.
    // Skips when a search result is already showing or trains were already loaded.
    LaunchedEffect(Unit) {
        if (hasResult || liveTrains.isNotEmpty()) return@LaunchedEffect
        val hasPerm = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPerm) {
            viewModel.startLocating()
            if (detectedStation != null) viewModel.refreshLiveTrains()
            else tryGetLocation()
        }
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state          = listState,
            modifier       = Modifier.fillMaxSize().background(AppBackground),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // ── Hero ────────────────────────────────────────────────────────
            item {
                val s = LocalStrings.current
                HeroSection(eyebrow = s.trackEyebrow, title = s.trackTitle, titleAccent = s.trackAccent)
            }

            // ── Search pill ──────────────────────────────────────────────────
            item {
                SearchPill(
                    query         = query,
                    onQueryChange = { viewModel.onQueryChange(it) },
                    onSearch      = {
                        keyboardController?.hide(); focusManager.clearFocus(); viewModel.search()
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp)
                )
            }

            // ── Station name suggestions ─────────────────────────────────────
            if (stationSuggestions.isNotEmpty()) {
                item {
                    com.yatrain.app.ui.screens.search.StationSuggestionsCard(
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

            // ── Train name suggestions ───────────────────────────────────────
            if (trainSuggestions.isNotEmpty()) {
                item {
                    com.yatrain.app.ui.screens.search.TrainNameSuggestionsCard(
                        suggestions = trainSuggestions,
                        onSelect    = { train ->
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            viewModel.onTrainSuggestionSelected(train)
                        },
                        modifier = Modifier.padding(horizontal = 18.dp).offset(y = (-6).dp)
                    )
                }
            }

            // ── Pro banner ───────────────────────────────────────────────────
            if (!hasResult) {
                item {
                    ProBannerCard(
                        viewModel  = subViewModel,
                        isSignedIn = true,
                        onTap      = { showPaywall = true },
                        modifier   = Modifier.padding(horizontal = 18.dp, vertical = 4.dp)
                    )
                }
            }

            // ── Error ────────────────────────────────────────────────────────
            if (error != null) {
                item {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("⚠", color = AppOrange)
                        Spacer(Modifier.width(6.dp))
                        Text(error!!, fontSize = 13.sp, color = AppTitle, modifier = Modifier.weight(1f))
                    }
                }
            }

            // ── Loading ──────────────────────────────────────────────────────
            if (isLoading) {
                item {
                    Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AppOrange, modifier = Modifier.size(32.dp))
                    }
                }
                return@LazyColumn
            }

            // ── Train found: detail view ──────────────────────────────────────
            if (hasResult) {
                // Live status card
                item {
                    LiveStatusCard(
                        train             = trainResult,
                        schedule          = schedule,
                        pnrContext        = pnrContext,
                        isSaved           = isTrainSaved,
                        isPro             = isPro,
                        currentStopIndex  = currentStop,
                        alertedStations   = alerted,
                        onAlertToggle     = { station ->
                            if (alerted.contains(station.code)) viewModel.cancelStationAlert(station)
                            else viewModel.showAlertPicker(station)
                        },
                        onSaveTrain       = {
                            if (isPro) viewModel.toggleSaveTrain()
                            else showPaywall = true
                        },
                        onClear           = { viewModel.clearResult(); focusManager.clearFocus() },
                        onShowPaywall     = { showPaywall = true }
                    )
                }

                // The Stream — route list
                item { Spacer(Modifier.height(16.dp)) }
                item {
                    RouteStreamHeader(
                        stopCount = schedule.size,
                        trainName = (trainResult?.name ?: "").localTrainName()
                    )
                }

                itemsIndexed(schedule) { index, station ->
                    StationRow(
                        station      = station,
                        index        = index,
                        currentIndex = currentStop,
                        isLast       = index == schedule.lastIndex,
                        isAlerted    = station.code in alerted,
                        onAlert      = {
                            if (station.code in alerted) viewModel.cancelStationAlert(station)
                            else if (!isPro && alerted.size >= 2) showPaywall = true
                            else viewModel.showAlertPicker(station)
                        },
                        onMapTap     = { viewModel.showMap(station) }
                    )
                }

                // Bottom action bar
                item {
                    ActionBar(
                        isSignedIn   = true,
                        onStationMap = {
                            // Show map for current (first) station
                            schedule.firstOrNull()?.let { viewModel.showMap(it) }
                        },
                        onComplaint = {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://railmadad.indianrailways.gov.in/madad/final/home.jsp"))
                            )
                        }
                    )
                }
                return@LazyColumn
            }

            // ── Default state: location-based trains ─────────────────────────
            item {
                val s = LocalStrings.current
                Row(
                    modifier              = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            s.liveTrainsToday,
                            fontSize = 11.sp, fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp, color = AppMuted
                        )
                        if (detectedStation != null) {
                            Text(
                                "${detectedStation!!.name} · ${detectedStation!!.city}",
                                fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AppOrange
                            )
                        } else if (showingPopularTrains) {
                            Text(
                                s.popularRightNow,
                                fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = AppOrange
                            )
                        } else {
                            Text(
                                if (isLocating) s.detectingStation else s.locatingStation,
                                fontSize = 11.sp, color = AppOrange
                            )
                        }
                    }
                    // Live Trains button — tapping re-detects location and refreshes
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50.dp))
                            .background(Color(0xFF22C55E))
                            .clickable(enabled = !isLocating) {
                                val hasPerm = ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.ACCESS_COARSE_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED
                                if (hasPerm) {
                                    viewModel.startLocating()
                                    tryGetLocation()
                                } else {
                                    locationLauncher.launch(
                                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                                    )
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        if (isLocating) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Outlined.DownloadForOffline, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                        Text(LocalStrings.current.liveTrainsBtn, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            // ── Train table ──────────────────────────────────────────────────
            item {
                Card(
                    modifier  = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                    shape     = RoundedCornerShape(18.dp),
                    colors    = CardDefaults.cardColors(containerColor = AppCard),
                    border    = BorderStroke(1.dp, AppBorder),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column {
                        val s = LocalStrings.current
                        Row(
                            Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Spacer(Modifier.width(54.dp))
                            Text(s.colTrain, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp, color = AppMuted, modifier = Modifier.weight(1f))
                            Text(s.colRoute, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp, color = AppMuted, modifier = Modifier.width(110.dp))
                            Text(s.colDep,   fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp, color = AppMuted, textAlign = TextAlign.End, modifier = Modifier.width(50.dp))
                        }
                        HorizontalDivider(color = AppBorder, thickness = 0.5.dp)

                        if (isLocating) {
                            Box(
                                Modifier.fillMaxWidth().height(120.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = AppOrange, modifier = Modifier.size(28.dp))
                            }
                        } else if (liveTrains.isEmpty() && detectedStation != null) {
                            Box(
                                Modifier.fillMaxWidth().height(120.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    // Show actual API error when available so the problem is immediately visible
                                    error ?: LocalStrings.current.noTrainsFound,
                                    fontSize = 13.sp,
                                    color = if (error != null) AppOrange else AppMuted,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 24.dp)
                                )
                            }
                        } else if (liveTrains.isEmpty()) {
                            Box(
                                Modifier.fillMaxWidth().height(120.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    LocalStrings.current.enableLocationToSeeTrains,
                                    fontSize = 13.sp, color = AppMuted, textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 24.dp)
                                )
                            }
                        } else {
                            liveTrains.forEachIndexed { i, t ->
                                if (i > 0) HorizontalDivider(color = AppBorder, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 14.dp))
                                LiveTrainRow(
                                    train = t,
                                    onTap = {
                                        keyboardController?.hide()
                                        focusManager.clearFocus()
                                        viewModel.onLiveTrainTapped(t)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(12.dp))
                Text(
                    LocalStrings.current.enterTrainNumberHint,
                    fontSize = 12.sp, color = Zinc400,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
                )
            }
        }

        // Status bar scrim — fades in when content scrolls behind the transparent status bar
        Box(
            Modifier
                .fillMaxWidth()
                .windowInsetsTopHeight(WindowInsets.statusBars)
                .background(statusBarScrimColor)
        )

        // Snackbar overlay
        SnackbarHost(
            hostState = snackbarHostState,
            modifier  = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
        )
    }

    // Alert picker bottom sheet
    if (alertPickerStation != null) {
        AlertPickerSheet(
            station     = alertPickerStation!!,
            trainNumber = trainResult?.number ?: "",
            onConfirm   = { mins -> viewModel.setStationAlert(alertPickerStation!!, mins) },
            onDismiss   = { viewModel.hideAlertPicker() }
        )
    }

    // Platform map bottom sheet
    if (mapStation != null) {
        StationMapSheet(station = mapStation!!, onDismiss = { viewModel.hideMap() })
    }

    // Pro paywall sheet
    if (showPaywall) {
        ProPaywallSheet(viewModel = subViewModel, onDismiss = { showPaywall = false })
    }
}

// ── Live Status Card ──────────────────────────────────────────────────────────

@Composable
private fun LiveStatusCard(
    train:            Train?,
    schedule:         List<Station>,
    pnrContext:       PNRStatus?,
    isSaved:          Boolean,
    isPro:            Boolean = true,
    currentStopIndex: Int = 0,
    alertedStations:  Set<String> = emptySet(),
    onAlertToggle:    (Station) -> Unit = {},
    onSaveTrain:      () -> Unit,
    onClear:          () -> Unit,
    onShowPaywall:    () -> Unit = {}
) {
    var showLiveMap by remember { mutableStateOf(false) }
    val currentStation = schedule.firstOrNull()
    val nextStation    = schedule.getOrNull(1)

    val pnrCoach = pnrContext?.let { p ->
        p.passengers.firstOrNull()?.let { it.currentCoachId.ifBlank { it.bookingCoachId } } ?: p.coach
    } ?: ""
    val pnrBerth = pnrContext?.let { p ->
        p.passengers.firstOrNull()?.let { if (it.currentBerthNo > 0) it.currentBerthNo else it.bookingBerthNo } ?: 0
    } ?: 0

    val sLsc = LocalStrings.current
    Card(
        modifier  = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 6.dp),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(Modifier.fillMaxWidth()) {
            // ── Train badge row ────────────────────────────────────────────
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment  = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        Modifier.size(8.dp).clip(CircleShape).background(AppOrange)
                    )
                    Text(
                        "${sLsc.trainPrefix} · ${train?.number ?: "–"}",
                        fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp, color = AppMuted
                    )
                }
                Row(
                    Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .background(Color(0xFF22C55E).copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(Modifier.size(7.dp).clip(CircleShape).background(Color(0xFF22C55E)))
                    Text(sLsc.liveLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                }
            }

            // ── Route row ─────────────────────────────────────────────────
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        train?.sourceCode?.ifBlank { "–" } ?: "–",
                        fontSize = 28.sp, fontWeight = FontWeight.Black, color = AppNavy
                    )
                    Text(
                        train?.source?.ifBlank { currentStation?.name ?: "–" } ?: "–",
                        fontSize = 11.sp, color = AppMuted, maxLines = 1
                    )
                }
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    // Animated train on a dotted track
                    Canvas(Modifier.fillMaxWidth().height(28.dp)) {
                        val y = size.height / 2
                        // Dotted line left half
                        drawLine(
                            Color(0xFFCDD5E0), Offset(0f, y), Offset(size.width * 0.35f, y),
                            strokeWidth = 2.dp.toPx(),
                            pathEffect  = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                        )
                        // Dotted line right half
                        drawLine(
                            Color(0xFFCDD5E0), Offset(size.width * 0.65f, y), Offset(size.width, y),
                            strokeWidth = 2.dp.toPx(),
                            pathEffect  = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                        )
                        // Dots
                        drawCircle(Color(0xFF555F6E), 4.dp.toPx(), Offset(0f, y))
                        drawCircle(Color(0xFF555F6E), 4.dp.toPx(), Offset(size.width, y))
                    }
                    Icon(
                        Icons.Filled.DirectionsRailway, null,
                        tint     = AppOrange,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        train?.destinationCode?.ifBlank { "–" } ?: "–",
                        fontSize = 28.sp, fontWeight = FontWeight.Black, color = AppOrange
                    )
                    Text(
                        train?.destination?.ifBlank { schedule.lastOrNull()?.name ?: "–" } ?: "–",
                        fontSize = 11.sp, color = AppMuted, maxLines = 1
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Dashed divider
            Canvas(Modifier.fillMaxWidth().height(1.dp).padding(horizontal = 20.dp)) {
                drawLine(
                    Color(0xFFCDD5E0), Offset(0f, 0f), Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect  = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── Journey time + next stop ───────────────────────────────────
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Column(Modifier.weight(1f)) {
                    Text(sLsc.journeyTimeLeft, fontSize = 9.sp, letterSpacing = 1.sp, color = AppMuted, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    val dur = train?.duration?.replace(".", ":") ?: "–"
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(dur, fontSize = 38.sp, fontWeight = FontWeight.Black, color = AppNavy)
                        Spacer(Modifier.width(4.dp))
                        Text(LocalStrings.current.hoursUnit, fontSize = 18.sp, color = AppMuted, modifier = Modifier.padding(bottom = 6.dp))
                    }
                }
                if (nextStation != null) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(sLsc.nextStop, fontSize = 10.sp, color = AppMuted)
                        Text(nextStation.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AppNavy)
                        Text(
                            "${nextStation.departure.replace(".", ":")}  ·  Plt –",
                            fontSize = 11.sp, color = AppMuted
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── Stats row ─────────────────────────────────────────────────
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatPill(sLsc.speedLabel,    "0 km/h",  Modifier.weight(1f))
                StatPill(sLsc.progressLabel, "0%",       Modifier.weight(1f))
                StatPill(sLsc.statusLabelShort, "+17m",  Modifier.weight(1f), valueColor = AppOrange)
            }

            Spacer(Modifier.height(12.dp))

            // ── PNR ticket sub-card (shown when navigated from PNR search) ─
            if (pnrContext != null) {
                val firstPax = pnrContext.passengers.firstOrNull()
                val seatDisp = buildString {
                    val st = (firstPax?.currentStatus ?: pnrContext.currentStatus).substringBefore("/").trim().take(3)
                    if (st.isNotBlank()) append(st)
                    if (pnrCoach.isNotBlank()) append("/$pnrCoach")
                    if (pnrBerth > 0) append("/$pnrBerth")
                    val bc = firstPax?.currentBerthCode?.ifBlank { firstPax.bookingBerthCode } ?: ""
                    if (bc.isNotBlank()) append("/$bc")
                }.ifBlank { "–" }

                val chartReady = pnrContext.chartStatus.contains("Prepared", ignoreCase = true)

                Card(
                    modifier  = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    shape     = RoundedCornerShape(14.dp),
                    colors    = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFB)),
                    border    = BorderStroke(1.dp, AppBorder),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                        // PNR header row
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Outlined.ConfirmationNumber, null, tint = AppNavy, modifier = Modifier.size(16.dp))
                                Text("PNR · ${pnrContext.pnrNumber}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AppNavy)
                            }
                            if (pnrContext.chartStatus.isNotBlank()) {
                                Box(
                                    Modifier.clip(RoundedCornerShape(50.dp))
                                        .background(if (chartReady) AppGreen.copy(0.12f) else AppOrange.copy(0.10f))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(pnrContext.chartStatus, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                        color = if (chartReady) AppGreen else AppOrange)
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        // From → To
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(sLsc.fromLabel, fontSize = 9.sp, color = AppMuted, letterSpacing = 0.8.sp)
                                Text(pnrContext.from, fontSize = 18.sp, fontWeight = FontWeight.Black, color = AppNavy)
                                Text(stationNameFor(pnrContext.from), fontSize = 10.sp, color = AppMuted, maxLines = 1)
                            }
                            Icon(Icons.Outlined.ArrowForward, null, tint = AppOrange, modifier = Modifier.size(18.dp))
                            Column(horizontalAlignment = Alignment.End) {
                                Text(sLsc.toLabel, fontSize = 9.sp, color = AppMuted, letterSpacing = 0.8.sp)
                                Text(pnrContext.to, fontSize = 18.sp, fontWeight = FontWeight.Black, color = AppOrange)
                                Text(stationNameFor(pnrContext.to), fontSize = 10.sp, color = AppOrange.copy(0.7f), maxLines = 1)
                            }
                        }

                        // Class + date
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (pnrContext.journeyClass.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Outlined.DirectionsRailway, null, tint = AppMuted, modifier = Modifier.size(13.dp))
                                    Text(pnrContext.journeyClass, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = AppNavy)
                                }
                            }
                            if (pnrContext.dateOfJourney.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Outlined.CalendarToday, null, tint = AppMuted, modifier = Modifier.size(13.dp))
                                    Text(pnrContext.dateOfJourney, fontSize = 12.sp, color = AppMuted)
                                }
                            }
                        }

                        // Passengers
                        if (pnrContext.passengers.isNotEmpty()) {
                            HorizontalDivider(color = AppBorder, modifier = Modifier.padding(vertical = 8.dp))
                            pnrContext.passengers.forEach { p ->
                                val pSeat = buildString {
                                    val st = p.bookingStatus.substringBefore("/").trim().take(3)
                                    if (st.isNotBlank()) append(st)
                                    val bc = p.bookingCoachId; if (bc.isNotBlank()) append("/$bc")
                                    if (p.bookingBerthNo > 0) append("/${p.bookingBerthNo}")
                                    val bcd = p.bookingBerthCode; if (bcd.isNotBlank()) append("/$bcd")
                                }
                                val cSeat = buildString {
                                    val st = p.currentStatus.substringBefore("/").trim().take(3)
                                    if (st.isNotBlank()) append(st)
                                    val cc = p.currentCoachId.ifBlank { p.bookingCoachId }; if (cc.isNotBlank()) append("/$cc")
                                    val bn = if (p.currentBerthNo > 0) p.currentBerthNo else p.bookingBerthNo; if (bn > 0) append("/$bn")
                                    val bc = p.currentBerthCode.ifBlank { p.bookingBerthCode }; if (bc.isNotBlank()) append("/$bc")
                                }
                                Row(
                                    Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment     = Alignment.CenterVertically
                                ) {
                                    Text("Passenger ${p.serialNumber}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AppNavy)
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(pSeat.ifBlank { "–" }, fontSize = 12.sp, color = AppMuted)
                                        Icon(Icons.Outlined.ArrowForward, null, tint = AppOrange, modifier = Modifier.size(13.dp))
                                        Text(cSeat.ifBlank { pSeat.ifBlank { "–" } }, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = AppGreen)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
            }

            // ── Coach position card ───────────────────────────────────────
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFFFF4EC))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Coach label box
                Box(
                    Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(AppOrange),
                    contentAlignment = Alignment.Center
                ) {
                    if (pnrCoach.isNotBlank()) {
                        Text(pnrCoach, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, textAlign = TextAlign.Center)
                    } else {
                        Icon(Icons.Filled.DirectionsRailway, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(sLsc.coachPosition, fontSize = 9.sp, letterSpacing = 1.sp, color = AppMuted, fontWeight = FontWeight.Bold)
                    if (pnrCoach.isNotBlank()) {
                        Text(
                            buildString {
                                append("Coach $pnrCoach")
                                if (pnrBerth > 0) append("  Seat $pnrBerth")
                            },
                            fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AppNavy
                        )
                    } else {
                        Text(sLsc.searchByPnrToSeeCoach, fontSize = 12.sp, color = AppTitle)
                    }
                    if (currentStation != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Info, null, tint = AppMuted, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("${sLsc.nowAtStation} ${currentStation.name}", fontSize = 11.sp, color = AppMuted)
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── Action buttons ────────────────────────────────────────────
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { showLiveMap = true },
                    modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = AppOrange,
                        contentColor   = Color.White
                    )
                ) {
                    Icon(
                        Icons.Outlined.Map,
                        null, modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(sLsc.openLiveMap, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                }
                OutlinedButton(
                    onClick  = onSaveTrain,
                    enabled  = true,
                    modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (isSaved) AppOrange else AppNavy,
                    ),
                    border   = BorderStroke(1.dp, if (isSaved) AppOrange.copy(0.5f) else AppBorder)
                ) {
                    Icon(
                        if (isSaved) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                        null, modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (isSaved) sLsc.savedLabel else sLsc.saveTrain,
                        fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── "Now at" banner ───────────────────────────────────────────
            if (currentStation != null) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(AppNavy)
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(36.dp).clip(CircleShape).background(AppOrange),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Navigation, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(sLsc.nowAt, fontSize = 10.sp, color = Color.White.copy(0.6f))
                        Text(currentStation.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Text(
                        "0/${currentStation.distance.ifBlank { "–" }} km",
                        fontSize = 12.sp, color = Color.White.copy(0.7f)
                    )
                }
            }

            // Close button row (subtle)
            TextButton(
                onClick  = onClear,
                modifier = Modifier.align(Alignment.End).padding(end = 8.dp)
            ) {
                Icon(Icons.Default.Close, null, modifier = Modifier.size(14.dp), tint = AppMuted)
                Spacer(Modifier.width(4.dp))
                Text(sLsc.clearLabel, fontSize = 12.sp, color = AppMuted)
            }
        }
    }

    // Live map sheet — free users see blurred preview with upgrade card
    if (showLiveMap) {
        LiveMapSheet(
            trainName        = (train?.name ?: "").localTrainName(),
            trainNumber      = train?.number ?: "",
            schedule         = schedule,
            currentStopIndex = currentStopIndex,
            delayMin         = 0,
            alertedStations  = alertedStations,
            onAlertToggle    = onAlertToggle,
            onDismiss        = { showLiveMap = false },
            isPro            = isPro,
            onUpgrade        = { showLiveMap = false; onShowPaywall() }
        )
    }
}

@Composable
private fun StatPill(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color = AppNavy) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFF1F5F9))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(label, fontSize = 9.sp, color = AppMuted, letterSpacing = 0.5.sp)
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = valueColor)
    }
}

// ── The Stream — Route section ────────────────────────────────────────────────

@Composable
private fun RouteStreamHeader(stopCount: Int, trainName: String) {
    val s = LocalStrings.current
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
    ) {
        Text(
            s.theStream,
            fontSize = 10.sp, fontWeight = FontWeight.Bold,
            letterSpacing = 1.4.sp, color = Color(0xFF005BA2)
        )
        Text(
            s.stationsAlongRoute,
            fontSize = 22.sp, fontWeight = FontWeight.Black, color = AppNavy
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val sStream = LocalStrings.current
            Text(
                "$stopCount ${sStream.stopsLabel}",
                fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AppMuted
            )
            Text("·", fontSize = 13.sp, color = AppMuted)
            Text("${sStream.tapSetAlertPre} ", fontSize = 13.sp, color = AppMuted)
            Icon(
                Icons.Outlined.NotificationsNone,
                contentDescription = null,
                tint = AppMuted,
                modifier = Modifier.size(14.dp)
            )
            Text(" ${sStream.tapSetAlertPost}", fontSize = 13.sp, color = AppMuted)
        }
    }
    Spacer(Modifier.height(12.dp))
}

// ── Station row — past / current / future visual states ───────────────────────

private val PastDotColor    = Color(0xFF38BDF8)  // cyan-blue  (passed stations)
private val CurrentDotColor = AppOrange           // orange     (train is here)
private val FutureDotColor  = Color(0xFFCDD5E0)  // light grey (upcoming)

private val PastLineColor   = Color(0xFF38BDF8)
private val FutureLineColor = Color(0xFFE2E8F0)

@Composable
private fun StationRow(
    station:      Station,
    index:        Int,
    currentIndex: Int,
    isLast:       Boolean,
    isAlerted:    Boolean,
    onAlert:      () -> Unit,
    onMapTap:     () -> Unit
) {
    val isPast    = index < currentIndex
    val isCurrent = index == currentIndex
    val isFuture  = index > currentIndex

    // Dot colours
    val dotColor = when {
        isCurrent -> CurrentDotColor
        isPast    -> PastDotColor
        else      -> FutureDotColor
    }
    // Line colour below this row
    val lineColor = if (index < currentIndex) PastLineColor else FutureLineColor

    // Text colours
    val nameColor = when {
        isPast    -> Color(0xFFA0AEC0)
        isCurrent -> AppNavy
        else      -> AppNavy
    }
    val metaColor = if (isPast) Color(0xFFCDD5E0) else AppMuted
    val timeColor = when {
        isPast    -> Color(0xFFA0AEC0)
        isCurrent -> AppOrange
        else      -> AppNavy
    }

    val alertColor by animateColorAsState(
        targetValue   = if (isAlerted) AppOrange else AppMuted,
        animationSpec = tween(250), label = "alertColor"
    )

    // Effective time to display:
    // - First station: its departure  (it originates here, so show dep)
    // - Last station: its arrival     (it terminates here)
    // - Others: arrival at this stop
    val rawTime = when {
        index == 0 -> station.departure
        isLast     -> station.arrival
        else       -> station.arrival
    }
    val displayTime = rawTime.replace(".", ":").trim()

    // Day-offset suffix: show "+N" when day field > 1
    val dayNum = station.day.trim().toIntOrNull() ?: 1
    val daySuffix = if (dayNum > 1) "+${dayNum - 1}" else ""

    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {

        // ── Timeline column ───────────────────────────────────────────────
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier            = Modifier.width(28.dp)
        ) {
            when {
                isCurrent -> {
                    // Orange ring with filled orange inner dot
                    Box(
                        Modifier.size(22.dp).clip(CircleShape)
                            .background(CurrentDotColor.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            Modifier.size(14.dp).clip(CircleShape)
                                .background(CurrentDotColor)
                        )
                    }
                }
                isFuture  -> {
                    // Outlined grey circle (future)
                    Box(
                        Modifier.size(14.dp).clip(CircleShape)
                            .background(Color.White)
                            .drawBehind {
                                drawCircle(
                                    color  = FutureDotColor,
                                    radius = size.minDimension / 2f,
                                    style  = Stroke(width = 2.dp.toPx())
                                )
                            }
                    )
                }
                else      -> {
                    // Solid filled (past / first)
                    Box(
                        Modifier.size(14.dp).clip(CircleShape)
                            .background(dotColor)
                    )
                }
            }

            // Vertical line
            if (!isLast) {
                Box(
                    Modifier.width(2.dp).height(72.dp).background(lineColor)
                )
            }
        }

        Spacer(Modifier.width(14.dp))

        // ── Station content ───────────────────────────────────────────────
        Row(
            Modifier.weight(1f).padding(bottom = if (isLast) 4.dp else 0.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.Top
        ) {
            // Left: name + meta
            Column(Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    station.name,
                    fontSize   = if (isCurrent) 17.sp else 16.sp,
                    fontWeight = if (isCurrent || isLast || index == 0) FontWeight.Bold else FontWeight.Normal,
                    color      = nameColor,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    modifier              = Modifier.padding(top = 2.dp)
                ) {
                    Text(station.code, fontSize = 12.sp, color = metaColor, fontWeight = FontWeight.Medium)
                    if (station.halt > 0) {
                        Text("·", fontSize = 11.sp, color = metaColor)
                        Text("${station.halt}m halt", fontSize = 12.sp, color = metaColor)
                    }
                    if (station.distance.isNotBlank() && station.distance != "0") {
                        Text("·", fontSize = 11.sp, color = metaColor)
                        Text("${station.distance} km", fontSize = 12.sp, color = metaColor)
                    }
                }

                // "Train is here now" badge for current stop
                if (isCurrent) {
                    val sCurr = LocalStrings.current
                    Spacer(Modifier.height(5.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(AppOrange.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Box(Modifier.size(7.dp).clip(CircleShape).background(AppOrange))
                        Text(
                            sCurr.trainIsHereNow,
                            fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                            color = AppOrange
                        )
                    }
                }
            }

            // Right: alert pill + time
            Column(horizontalAlignment = Alignment.End) {
                // Alert pill — only for upcoming stops
                if (isFuture) {
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(50.dp))
                            .background(if (isAlerted) AppOrange.copy(0.12f) else Color(0xFFF1F5F9))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication        = null,
                                onClick           = onAlert
                            )
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            if (isAlerted) Icons.Filled.Notifications else Icons.Outlined.NotificationsNone,
                            null,
                            tint     = alertColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            LocalStrings.current.alertLabel,
                            fontSize   = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = alertColor
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                }

                // Time + day suffix + delay row
                if (displayTime.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            displayTime,
                            fontSize   = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color      = timeColor
                        )
                        if (daySuffix.isNotBlank()) {
                            Text(
                                daySuffix,
                                fontSize   = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color      = Color(0xFF6366F1)
                            )
                        }
                    }
                }
            }
        }
    }

    if (!isLast) Spacer(Modifier.height(4.dp))
}

// ── Action Bar ────────────────────────────────────────────────────────────────

@Composable
private fun ActionBar(isSignedIn: Boolean, onStationMap: () -> Unit, onComplaint: () -> Unit) {
    val s = LocalStrings.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick  = onStationMap,
            modifier = Modifier.weight(1f).height(48.dp),
            shape    = RoundedCornerShape(14.dp),
            colors   = ButtonDefaults.outlinedButtonColors(
                containerColor = if (isSignedIn) Color.Transparent else Color.White,
                contentColor   = if (isSignedIn) AppNavy else AppMuted
            ),
            border = BorderStroke(1.dp, if (isSignedIn) AppBorder else AppBorder.copy(alpha = 0.4f))
        ) {
            Icon(
                if (isSignedIn) Icons.Outlined.Map else Icons.Outlined.Lock,
                null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(s.stationMap, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }

        OutlinedButton(
            onClick  = onComplaint,
            modifier = Modifier.weight(1f).height(48.dp),
            shape    = RoundedCornerShape(14.dp),
            colors   = ButtonDefaults.outlinedButtonColors(contentColor = AppOrange),
            border   = BorderStroke(1.dp, AppOrange.copy(alpha = 0.5f))
        ) {
            Icon(Icons.Outlined.Feedback, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(s.fileComplaint, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
    }
}

// ── Alert Picker Bottom Sheet ─────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlertPickerSheet(
    station:     Station,
    trainNumber: String,
    onConfirm:   (Int) -> Unit,
    onDismiss:   () -> Unit
) {
    val sheetState        = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedMin by remember { mutableIntStateOf(15) }
    val options           = listOf(15, 30, 45, 60)

    // ETA label: prefer arrival time, fall back to departure
    val etaDisplay = station.arrival
        .ifBlank { station.departure }
        .replace(".", ":")
        .trim()
        .ifBlank { "—" }

    val sAp = LocalStrings.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = Color.White,
        dragHandle       = { BottomSheetDefaults.DragHandle(color = Color(0xFFCBD5E1)) }
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Bell icon
            Box(
                Modifier.size(56.dp).clip(CircleShape).background(AppOrange.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Notifications, null, tint = AppOrange, modifier = Modifier.size(28.dp))
            }

            Spacer(Modifier.height(12.dp))
            Text(sAp.arrivalAlert, fontSize = 22.sp, fontWeight = FontWeight.Black, color = AppNavy)
            Text(station.name, fontSize = 14.sp, color = AppBody, textAlign = TextAlign.Center)

            if (trainNumber.isNotBlank() || etaDisplay != "—") {
                Text(
                    buildString {
                        if (trainNumber.isNotBlank()) append("Train $trainNumber")
                        if (etaDisplay != "—") { if (trainNumber.isNotBlank()) append(" · "); append("ETA $etaDisplay") }
                    },
                    fontSize = 13.sp, color = AppOrange, textAlign = TextAlign.Center,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(24.dp))
            Text(sAp.wakeMeBeforeArrival, fontSize = 10.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.Bold, color = AppMuted)
            Spacer(Modifier.height(12.dp))

            // Minute picker pills
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { mins ->
                    val isSelected = mins == selectedMin
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) AppNavy else Color(0xFFF1F5F9))
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { selectedMin = mins }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$mins", fontSize = 20.sp, fontWeight = FontWeight.Black,
                                color = if (isSelected) Color.White else AppNavy)
                            Text(sAp.minLabel, fontSize = 11.sp,
                                color = if (isSelected) Color.White.copy(alpha = 0.75f) else AppMuted)
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

// ── Station Map Bottom Sheet ──────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StationMapSheet(station: Station, onDismiss: () -> Unit) {
    val info         = platformInfo(station.code)
    val sheetState   = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var fromPlatform by remember { mutableIntStateOf(1) }
    var toPlatform   by remember { mutableIntStateOf(1) }
    var fromExpanded by remember { mutableStateOf(false) }
    var toExpanded   by remember { mutableStateOf(false) }
    val context      = LocalContext.current

    val platformOptions = (1..info.platforms).toList()

    val sSm = LocalStrings.current
    val walkMinutes = when {
        fromPlatform == toPlatform                     -> 1
        kotlin.math.abs(fromPlatform - toPlatform) <= 2 -> 3
        else                                           -> 5
    }
    val walkDesc = when {
        fromPlatform == toPlatform -> sSm.samePlatformWalk
        else                       -> sSm.useFootOverBridge
    }

    ModalBottomSheet(
        onDismissRequest  = onDismiss,
        sheetState        = sheetState,
        containerColor    = Color(0xFFF0F4FB),
        dragHandle        = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Close
            OutlinedButton(
                onClick = onDismiss,
                shape   = RoundedCornerShape(50.dp),
                border  = BorderStroke(1.dp, AppBorder),
                colors  = ButtonDefaults.outlinedButtonColors(contentColor = AppNavy)
            ) { Text(sSm.closeLabel) }

            Spacer(Modifier.height(12.dp))

            Text(sSm.platformNavigation, fontSize = 10.sp, letterSpacing = 1.5.sp, color = Color(0xFF005BA2), fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(station.name, fontSize = 30.sp, fontWeight = FontWeight.Black, color = AppNavy)
            Text("${info.platforms} platforms  ·  ${info.fobs} foot-over-bridges", fontSize = 13.sp, color = AppMuted)

            Spacer(Modifier.height(20.dp))

            // Platform pickers
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment     = Alignment.Top
            ) {
                Column(Modifier.weight(1f)) {
                    Text(sSm.imOnLabel, fontSize = 9.sp, letterSpacing = 1.sp, color = AppMuted, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    ExposedDropdownMenuBox(
                        expanded        = fromExpanded,
                        onExpandedChange = { fromExpanded = it }
                    ) {
                        OutlinedTextField(
                            value        = "${sSm.platformLabel} $fromPlatform",
                            onValueChange = {},
                            readOnly     = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(fromExpanded) },
                            modifier     = Modifier.menuAnchor().fillMaxWidth(),
                            shape        = RoundedCornerShape(12.dp),
                            colors       = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor   = AppNavy,
                                unfocusedBorderColor = AppBorder,
                                focusedTextColor     = AppNavy,
                                unfocusedTextColor   = AppNavy
                            )
                        )
                        ExposedDropdownMenu(expanded = fromExpanded, onDismissRequest = { fromExpanded = false }) {
                            platformOptions.forEach { pf ->
                                DropdownMenuItem(
                                    text    = { Text("${sSm.platformLabel} $pf") },
                                    onClick = { fromPlatform = pf; fromExpanded = false }
                                )
                            }
                        }
                    }
                }

                Box(
                    Modifier.padding(top = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.ArrowForward, null, tint = AppOrange)
                }

                Column(Modifier.weight(1f)) {
                    Text(sSm.takeMeToLabel, fontSize = 9.sp, letterSpacing = 1.sp, color = AppMuted, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    ExposedDropdownMenuBox(
                        expanded         = toExpanded,
                        onExpandedChange = { toExpanded = it }
                    ) {
                        OutlinedTextField(
                            value         = "${sSm.platformLabel} $toPlatform",
                            onValueChange = {},
                            readOnly      = true,
                            trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(toExpanded) },
                            modifier      = Modifier.menuAnchor().fillMaxWidth(),
                            shape         = RoundedCornerShape(12.dp),
                            colors        = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor   = AppNavy,
                                unfocusedBorderColor = AppBorder,
                                focusedTextColor     = AppNavy,
                                unfocusedTextColor   = AppNavy
                            )
                        )
                        ExposedDropdownMenu(expanded = toExpanded, onDismissRequest = { toExpanded = false }) {
                            platformOptions.forEach { pf ->
                                DropdownMenuItem(
                                    text    = { Text("${sSm.platformLabel} $pf") },
                                    onClick = { toPlatform = pf; toExpanded = false }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Walk time card
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(44.dp).clip(CircleShape).background(AppOrange.copy(0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.DirectionsWalk, null, tint = AppOrange, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("~$walkMinutes ${sSm.walkMinutes}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AppNavy)
                    Text(walkDesc, fontSize = 12.sp, color = AppMuted)
                }
            }

            Spacer(Modifier.height(16.dp))

            // Station schematic
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(16.dp),
                colors    = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(sSm.stationSchematic, fontSize = 9.sp, letterSpacing = 1.2.sp, color = AppMuted, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    PlatformSchematic(
                        platformCount  = info.platforms.coerceAtMost(8), // cap for display
                        fobCount       = info.fobs,
                        activePlatform = fromPlatform,
                        targetPlatform = toPlatform,
                        modifier       = Modifier.fillMaxWidth().height(220.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Amenities
            Text(sSm.onThisStation, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppNavy)
            Spacer(Modifier.height(8.dp))
            defaultAmenities.forEach { amenity ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(amenity.icon, null, tint = AppNavy, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(amenity.label, fontSize = 14.sp, color = AppNavy, modifier = Modifier.weight(1f))
                    Text(amenity.location, fontSize = 12.sp, color = AppMuted)
                }
                HorizontalDivider(color = AppBorder, thickness = 0.5.dp)
            }

            // View in Google Maps CTA
            stationCoordFor(station.code)?.let { coord ->
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick  = {
                        val mapsUri = Uri.parse("geo:${coord.lat},${coord.lng}?q=${coord.lat},${coord.lng}")
                        context.startActivity(Intent(Intent.ACTION_VIEW, mapsUri))
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8))
                ) {
                    Icon(Icons.Outlined.Map, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("View in Google Maps", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

// ── Platform Schematic Canvas ─────────────────────────────────────────────────

@Composable
private fun PlatformSchematic(
    platformCount:  Int,
    fobCount:       Int,
    activePlatform: Int,
    targetPlatform: Int,
    modifier:       Modifier = Modifier
) {
    Canvas(modifier) {
        val labelW   = 36.dp.toPx()
        val trackH   = 10.dp.toPx()
        val fobW     = 14.dp.toPx()
        val totalH   = size.height
        val trackW   = size.width - labelW
        val spacing  = totalH / (platformCount + 1)

        // ── FOB bars (behind platforms) ───────────────────────────────────
        val fobXs = (1..fobCount).map { i ->
            labelW + trackW * (i.toFloat() / (fobCount + 1))
        }
        fobXs.forEach { fx ->
            drawRoundRect(
                color        = Color(0xFFDDE3EE),
                topLeft      = Offset(fx - fobW / 2, spacing * 0.6f),
                size         = Size(fobW, totalH - spacing * 0.6f),
                cornerRadius = CornerRadius(4.dp.toPx())
            )
        }

        // ── Platform tracks ───────────────────────────────────────────────
        for (i in 1..platformCount) {
            val y        = spacing * i
            val isActive = i == activePlatform
            val isTarget = i == targetPlatform && targetPlatform != activePlatform

            val trackColor = when {
                isActive -> Color(0xFF1A2856)
                isTarget -> AppOrange.copy(alpha = 0.6f)
                else     -> Color(0xFFCDD5E0)
            }

            drawRoundRect(
                color        = trackColor,
                topLeft      = Offset(labelW, y - trackH / 2),
                size         = Size(trackW, trackH),
                cornerRadius = CornerRadius(4.dp.toPx())
            )

            // "You" marker on active platform
            if (isActive) {
                drawCircle(
                    color  = Color.White,
                    radius = 7.dp.toPx(),
                    center = Offset(labelW + trackW * 0.35f, y)
                )
                drawCircle(
                    color  = Color(0xFF1A2856),
                    radius = 7.dp.toPx(),
                    center = Offset(labelW + trackW * 0.35f, y),
                    style  = Stroke(1.5f.dp.toPx())
                )
            }
        }
    }
}

// ── Live train row (location-based table) ────────────────────────────────────

@Composable
private fun LiveTrainRow(train: LiveTrainEntry, onTap: () -> Unit) {
    val delayColor  = if (train.delayMin > 0) Color(0xFFE53935) else Color(0xFF22C55E)
    val delayLabel  = if (train.delayMin > 0) "+${train.delayMin}m" else LocalStrings.current.statusOnTime
    val depDisplay  = if (train.departure == "Last") "—" else train.departure
    val showArrival = train.arrival != "First" && train.departure == "Last"

    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onTap)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ── Icon ─────────────────────────────────────────────────────────
        Box(
            Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(AppOrange.copy(0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.DirectionsRailway, null,
                tint = AppOrange, modifier = Modifier.size(24.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

        // ── Train name + status + number/platform ────────────────────────
        Column(Modifier.weight(1f)) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    train.name.localTrainName(),
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Color(0xFF1565C0),
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                    modifier   = Modifier.weight(1f, fill = false)
                )
                Text(
                    delayLabel,
                    fontSize   = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color      = delayColor
                )
            }
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier              = Modifier.padding(top = 2.dp)
            ) {
                Text(train.number, fontSize = 11.sp, color = AppMuted)
                if (train.platform != "–") {
                    Text("·", fontSize = 11.sp, color = AppMuted)
                    Text(
                        "PF ${train.platform}",
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = AppOrange
                    )
                }
            }
        }

        Spacer(Modifier.width(8.dp))

        // ── Route column ─────────────────────────────────────────────────
        Column(Modifier.width(108.dp)) {
            Text(
                train.fromName,
                fontSize = 11.sp, color = AppTitle,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text("↓", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AppOrange)
                Text(
                    train.toName,
                    fontSize = 11.sp, color = AppBody,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
        }

        // ── Time column ──────────────────────────────────────────────────
        Column(
            Modifier.width(52.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                depDisplay,
                fontSize   = 14.sp,
                fontWeight = FontWeight.Bold,
                color      = AppTitle
            )
            if (showArrival) {
                Text("arr ${train.arrival}", fontSize = 9.sp, color = AppMuted)
            }
        }
    }
}
