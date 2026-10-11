package com.yatrain.app.ui.screens.track

import android.content.Intent
import android.net.Uri
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Train
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*
import com.yatrain.app.R
import com.yatrain.app.i18n.LocalStrings
import com.yatrain.app.models.Station
import com.yatrain.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.*

// ── Time helpers ──────────────────────────────────────────────────────────────

private fun parseMin(time: String?): Int? {
    if (time.isNullOrBlank() || time == "First" || time == "Last" || time == "–" || time == "-") return null
    return try {
        val clean = time.replace("+1", "").replace(".", ":").trim()
        val parts = clean.split(":")
        val h = parts[0].trim().toInt(); val m = parts[1].trim().toInt()
        val base = h * 60 + m
        if (time.contains("+1")) base + 1440 else base
    } catch (_: Exception) { null }
}

private fun nowMin(): Int {
    val c = Calendar.getInstance()
    return c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE)
}

// ── Real-time position from schedule distances + current clock ────────────────

/**
 * Returns t ∈ [0,1] representing the train's actual position along the route.
 * Uses Station.distance (km from origin) + time interpolation between the
 * current station's departure and the next station's arrival.
 * Falls back to index-based fraction when distances are unavailable.
 */
private fun realT(stations: List<Station>, currentIdx: Int): Double {
    if (stations.size < 2) return 0.0
    val totalKm = stations.lastOrNull()?.distance?.toDoubleOrNull()?.takeIf { it > 0 }
        ?: return currentIdx.toDouble() / (stations.size - 1).coerceAtLeast(1)

    val current = stations.getOrNull(currentIdx) ?: return 0.0
    val currentKm = current.distance.toDoubleOrNull() ?: 0.0

    val next = stations.getOrNull(currentIdx + 1)
    if (next != null) {
        val nextKm = next.distance.toDoubleOrNull() ?: currentKm
        val depMin = parseMin(current.departure)
        val arrMin = parseMin(next.arrival)
        if (depMin != null && arrMin != null && arrMin > depMin) {
            val frac = ((nowMin() - depMin).toDouble() / (arrMin - depMin)).coerceIn(0.0, 1.0)
            val posKm = currentKm + (nextKm - currentKm) * frac
            return (posKm / totalKm).coerceIn(0.0, 1.0)
        }
    }
    return (currentKm / totalKm).coerceIn(0.0, 1.0)
}

/** Returns estimated speed in km/h between currentIdx and the next station. */
private fun scheduleSpeedKmh(stations: List<Station>, currentIdx: Int): Double {
    val cur  = stations.getOrNull(currentIdx) ?: return 60.0
    val next = stations.getOrNull(currentIdx + 1) ?: return 60.0
    val curKm  = cur.distance.toDoubleOrNull()  ?: return 60.0
    val nextKm = next.distance.toDoubleOrNull() ?: return 60.0
    val dep = parseMin(cur.departure)  ?: return 60.0
    val arr = parseMin(next.arrival)   ?: return 60.0
    val tH  = (arr - dep) / 60.0
    if (tH <= 0) return 60.0
    return ((nextKm - curKm) / tH).coerceIn(20.0, 200.0)
}

// ── Geometry ──────────────────────────────────────────────────────────────────

private fun hav(a: LatLng, b: LatLng): Double {
    val R = 6371.0
    val dLat = Math.toRadians(b.latitude - a.latitude)
    val dLon = Math.toRadians(b.longitude - a.longitude)
    val h = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(a.latitude)) * cos(Math.toRadians(b.latitude)) * sin(dLon / 2).pow(2)
    return 2 * R * asin(sqrt(h))
}

private fun buildSegKm(coords: List<LatLng>): List<Double> {
    if (coords.isEmpty()) return emptyList()
    val out = mutableListOf(0.0)
    for (i in 1 until coords.size) out.add(out.last() + hav(coords[i - 1], coords[i]))
    return out
}

private fun interp(coords: List<LatLng>, seg: List<Double>, total: Double, t: Double): LatLng? {
    if (coords.size < 2 || seg.size != coords.size) return coords.firstOrNull()
    val km = total * t.coerceIn(0.0, 1.0)
    for (i in 1 until coords.size) {
        if (seg[i] >= km || i == coords.size - 1) {
            val span = seg[i] - seg[i - 1]
            val f = if (span == 0.0) 0.0 else (km - seg[i - 1]) / span
            val a = coords[i - 1]; val b = coords[i]
            return LatLng(a.latitude + (b.latitude - a.latitude) * f,
                          a.longitude + (b.longitude - a.longitude) * f)
        }
    }
    return coords.last()
}

private fun bearingAt(coords: List<LatLng>, seg: List<Double>, total: Double, t: Double): Float {
    if (coords.size < 2) return 0f
    val km = total * t.coerceIn(0.0, 1.0)
    for (i in 1 until coords.size) {
        if (seg[i] >= km || i == coords.size - 1) {
            val a = coords[i - 1]; val b = coords[i]
            val la = Math.toRadians(a.latitude); val lb = Math.toRadians(b.latitude)
            val dL = Math.toRadians(b.longitude - a.longitude)
            return ((Math.toDegrees(atan2(sin(dL) * cos(lb),
                cos(la) * sin(lb) - sin(la) * cos(lb) * cos(dL))) + 360) % 360).toFloat()
        }
    }
    return 0f
}

private fun splitAt(
    coords: List<LatLng>, seg: List<Double>, total: Double, t: Double
): Pair<List<LatLng>, List<LatLng>> {
    if (coords.size < 2) return Pair(coords, coords)
    val pt  = interp(coords, seg, total, t) ?: return Pair(coords, coords)
    val km  = total * t.coerceIn(0.0, 1.0)
    val pas = mutableListOf<LatLng>()
    val upc = mutableListOf<LatLng>(pt)
    for (i in coords.indices) {
        if (seg[i] <= km) pas.add(coords[i]) else upc.add(coords[i])
    }
    pas.add(pt)
    if (pas.size < 2) pas.add(0, coords.first())
    if (upc.size < 2) upc.add(coords.last())
    return Pair(pas, upc)
}

// ── Custom bitmaps (created inside MapEffect — Maps renderer guaranteed ready) ─


/** Small endpoint dot for origin / destination */
private fun endpointDotBitmap(color: Int): Bitmap {
    val sz = 24; val bmp = Bitmap.createBitmap(sz, sz, Bitmap.Config.ARGB_8888)
    val cv = Canvas(bmp); val p = Paint(Paint.ANTI_ALIAS_FLAG)
    p.color = android.graphics.Color.argb(60,
        android.graphics.Color.red(color), android.graphics.Color.green(color), android.graphics.Color.blue(color))
    cv.drawCircle(sz / 2f, sz / 2f, sz / 2f, p)
    p.color = color; cv.drawCircle(sz / 2f, sz / 2f, sz / 2f * .65f, p)
    p.color = android.graphics.Color.WHITE; p.style = Paint.Style.STROKE; p.strokeWidth = sz * .12f
    cv.drawCircle(sz / 2f, sz / 2f, sz / 2f * .50f, p)
    return bmp
}

private fun upcomingStationBitmap(alerted: Boolean): Bitmap {
    val sz = 56; val bmp = Bitmap.createBitmap(sz, sz, Bitmap.Config.ARGB_8888)
    val cv = Canvas(bmp); val p = Paint(Paint.ANTI_ALIAS_FLAG)
    val accent = if (alerted) android.graphics.Color.rgb(22, 163, 74)
                 else         android.graphics.Color.rgb(59, 130, 246)
    // Outer glow
    p.color = android.graphics.Color.argb(50,
        android.graphics.Color.red(accent),
        android.graphics.Color.green(accent),
        android.graphics.Color.blue(accent))
    cv.drawCircle(sz / 2f, sz / 2f, sz / 2f, p)
    // White body
    p.color = android.graphics.Color.WHITE
    cv.drawCircle(sz / 2f, sz / 2f, sz * .36f, p)
    // Accent ring
    p.color = accent; p.style = Paint.Style.STROKE; p.strokeWidth = sz * .07f
    cv.drawCircle(sz / 2f, sz / 2f, sz * .30f, p)
    p.style = Paint.Style.FILL

    if (alerted) {
        // Geometric checkmark (no text/emoji — safe on all Android versions)
        val path = android.graphics.Path()
        path.moveTo(sz * 0.27f, sz * 0.50f)
        path.lineTo(sz * 0.42f, sz * 0.65f)
        path.lineTo(sz * 0.70f, sz * 0.35f)
        p.color = accent; p.style = Paint.Style.STROKE
        p.strokeWidth = sz * 0.09f; p.strokeCap = Paint.Cap.ROUND; p.strokeJoin = Paint.Join.ROUND
        cv.drawPath(path, p)
    } else {
        // Geometric bell: dome arc + body rect + clapper dot + handle
        p.color = accent; p.style = Paint.Style.FILL
        cv.drawArc(RectF(sz * .30f, sz * .22f, sz * .70f, sz * .58f), 180f, 180f, true, p)
        cv.drawRect(sz * .30f, sz * .40f, sz * .70f, sz * .60f, p)
        cv.drawCircle(sz * .50f, sz * .66f, sz * .06f, p)
        cv.drawRect(sz * .44f, sz * .17f, sz * .56f, sz * .25f, p)
    }
    return bmp
}

// ── Train marker bitmap (drawn programmatically so centre is exact) ───────────
// Front (nose) at TOP of image. Anchor (0.5f, 0.5f) = bitmap centre = geographic position.
// This guarantees the train sits ON the polyline at every zoom level regardless of PNG padding.
private fun trainMarkerBitmap(): Bitmap {
    val w = 30; val h = 80
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val cv  = Canvas(bmp)
    val p   = Paint(Paint.ANTI_ALIAS_FLAG)
    val pad = 2f; val cr = 6f

    // White body
    p.color = android.graphics.Color.WHITE; p.style = Paint.Style.FILL
    cv.drawRoundRect(RectF(pad, pad, w - pad, h - pad), cr, cr, p)

    // Orange nose band at top
    p.color = android.graphics.Color.rgb(255, 107, 0)
    cv.drawRoundRect(RectF(pad, pad, w - pad, pad + 13f), cr, cr, p)
    p.color = android.graphics.Color.WHITE   // cut straight bottom edge of orange band
    cv.drawRect(pad, pad + 7f, w - pad, pad + 13f, p)

    // Coach separator at midpoint
    p.color = android.graphics.Color.rgb(200, 200, 200)
    p.style = Paint.Style.STROKE; p.strokeWidth = 1.5f
    cv.drawLine(pad + 1f, h / 2f, w - pad - 1f, h / 2f, p)

    // Windows — 2 per coach on each side
    p.color = android.graphics.Color.rgb(120, 170, 225); p.style = Paint.Style.FILL
    val wW = w * 0.26f; val wH = 5.5f; val wCr = 2f
    for (wy in listOf(h * 0.21f, h * 0.33f))  {          // upper coach
        cv.drawRoundRect(RectF(pad + 2f, wy, pad + 2f + wW, wy + wH), wCr, wCr, p)
        cv.drawRoundRect(RectF(w - pad - 2f - wW, wy, w - pad - 2f, wy + wH), wCr, wCr, p)
    }
    for (wy in listOf(h * 0.58f, h * 0.70f)) {           // lower coach
        cv.drawRoundRect(RectF(pad + 2f, wy, pad + 2f + wW, wy + wH), wCr, wCr, p)
        cv.drawRoundRect(RectF(w - pad - 2f - wW, wy, w - pad - 2f, wy + wH), wCr, wCr, p)
    }

    // Outline
    p.color = android.graphics.Color.rgb(90, 90, 90)
    p.style = Paint.Style.STROKE; p.strokeWidth = 1.5f
    cv.drawRoundRect(RectF(pad, pad, w - pad, h - pad), cr, cr, p)

    return bmp
}

// ── Share ─────────────────────────────────────────────────────────────────────

private fun buildShareText(
    trainNumber: String,
    trainName:   String,
    schedule:    List<Station>,
    currentIdx:  Int,
    delayMin:    Int
): String {
    val current = schedule.getOrNull(currentIdx)
    val next    = schedule.getOrNull(currentIdx + 1)
    val origin  = schedule.firstOrNull()
    val dest    = schedule.lastOrNull()

    return buildString {
        appendLine("Train $trainNumber${if (trainName.isNotBlank()) " · $trainName" else ""}")
        if (origin != null && dest != null)
            appendLine("${origin.name.orEmpty().ifBlank { "—" }} → ${dest.name.orEmpty().ifBlank { "—" }}")
        appendLine()
        if (current != null)
            appendLine("Near: ${current.name.orEmpty().ifBlank { "—" }}")
        if (next != null) {
            val rawEta  = next.arrival.orEmpty().ifBlank { null }
            val etaText = when {
                rawEta == null   -> ""
                delayMin > 0     -> " · ETA $rawEta (+${delayMin}m)"
                else             -> " · ETA $rawEta"
            }
            appendLine("Next stop: ${next.name.orEmpty().ifBlank { "—" }}$etaText")
        }
        appendLine("Status: ${if (delayMin > 0) "Delayed +${delayMin} min" else "On time"}")
        appendLine()
        append("Tracked on Yatrain")
    }
}

// ── Sheet ─────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveMapSheet(
    trainName:        String,
    trainNumber:      String,
    schedule:         List<Station>,
    currentStopIndex: Int,
    delayMin:         Int,
    alertedStations:  Set<String>,
    onAlertToggle:    (Station) -> Unit,
    onDismiss:        () -> Unit,
    isPro:            Boolean = true,
    onUpgrade:        () -> Unit = {}
) {
    val s        = LocalStrings.current
    val context  = LocalContext.current
    val mapScope = rememberCoroutineScope()

    // ── Route geometry ────────────────────────────────────────────────────────
    val routePoints = remember(schedule) {
        schedule.mapNotNull { st ->
            stationCoordFor(st.code)?.let { info -> st to LatLng(info.lat, info.lng) }
        }
    }
    val coords = remember(routePoints) { routePoints.map { it.second } }
    val seg    = remember(coords)      { buildSegKm(coords) }
    val total  = remember(seg)         { seg.lastOrNull()?.coerceAtLeast(1.0) ?: 1.0 }

    // Real initial position from schedule distances + clock
    val initialT = remember(schedule, currentStopIndex) { realT(schedule, currentStopIndex) }
    val speedKmh = remember(schedule, currentStopIndex) { scheduleSpeedKmh(schedule, currentStopIndex) }

    // ── Animation state (use plain mutableStateOf to avoid primitive-delegate edge cases) ──
    val animTState      = remember { mutableStateOf(initialT) }
    val animTargetState = remember {
        val lookAhead = speedKmh / 3600.0 / total * 30.0
        mutableStateOf((initialT + lookAhead).coerceAtMost(1.0))
    }
    val headingState    = remember { mutableStateOf(0f) }

    val markerState = rememberMarkerState(
        position = interp(coords, seg, total, initialT) ?: LatLng(20.5937, 78.9629)
    )
    // Bitmap descriptors — null until created in MapEffect
    val trainIconState   = remember { mutableStateOf<BitmapDescriptor?>(null) }
    val passedIconState  = remember { mutableStateOf<BitmapDescriptor?>(null) }
    val currentIconState = remember { mutableStateOf<BitmapDescriptor?>(null) }
    val bellIconState    = remember { mutableStateOf<BitmapDescriptor?>(null) }
    val alertedIconState = remember { mutableStateOf<BitmapDescriptor?>(null) }

    // ── Camera (must be declared before LaunchedEffect that uses it) ─────────
    val camState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            interp(coords, seg, total, initialT) ?: LatLng(20.5937, 78.9629),
            11f
        )
    }

    // Unwrap to local vals for use in composition
    val heading     = headingState.value
    val trainIcon   = trainIconState.value
    val passedIcon  = passedIconState.value
    val currentIcon = currentIconState.value
    val bellIcon    = bellIconState.value
    val alertedIcon = alertedIconState.value

    // ── Progress split for two-colour polyline ────────────────────────────────
    // Quantise to 0.2% steps so we don't create new LatLng lists at 60 fps.
    val splitT by remember { derivedStateOf { (animTState.value * 500).toLong() / 500.0 } }
    val (passedCoords, upcomingCoords) = remember(splitT, coords) {
        if (coords.size >= 2) splitAt(coords, seg, total, splitT)
        else Pair(emptyList<LatLng>(), coords)
    }

    // ── Animation loop + camera follow (single coroutine) ────────────────────
    LaunchedEffect(coords) {
        if (coords.size < 2) return@LaunchedEffect

        var lastNs = System.nanoTime()
        var frame  = 0
        while (true) {
            delay(16L)
            val now = System.nanoTime()
            val dt  = ((now - lastNs) / 1_000_000_000.0).coerceAtMost(0.1)
            lastNs  = now
            frame++

            val driftPerSec = speedKmh / 3600.0 / total
            animTargetState.value = (animTargetState.value + driftPerSec * dt).coerceAtMost(1.0)

            val ease = 1.0 - exp(-dt * 2.2)
            animTState.value = (animTState.value + (animTargetState.value - animTState.value) * ease).coerceIn(0.0, 1.0)

            val t = animTState.value
            interp(coords, seg, total, t)?.let { markerState.position = it }
            headingState.value = bearingAt(coords, seg, total, t)

            // Follow train smoothly every ~1 s (60 frames at 60 fps)
            if (frame % 60 == 0) {
                try { camState.animate(CameraUpdateFactory.newLatLng(markerState.position), 800) }
                catch (_: Exception) {}
            }
        }
    }

    // ── UI ────────────────────────────────────────────────────────────────────
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor   = AppCard,
        shape            = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.9f)) {

            // Header
            Row(
                Modifier.fillMaxWidth().background(AppNavy)
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(trainName.ifBlank { trainNumber },
                        fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    val label = if (delayMin > 0) "+${delayMin}m late" else s.statusOnTime
                    val col   = if (delayMin > 0) Color(0xFFFFA726) else Color(0xFF4CAF50)
                    Text(label, fontSize = 12.sp, color = col)
                }
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.clip(RoundedCornerShape(6.dp)).background(AppOrange)
                        .padding(horizontal = 8.dp, vertical = 3.dp)) {
                        Text("PRO", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    IconButton(
                        onClick = {
                            val text   = buildShareText(trainNumber, trainName, schedule, currentStopIndex, delayMin)
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, text)
                                putExtra(Intent.EXTRA_SUBJECT, "Train $trainNumber live location")
                            }
                            context.startActivity(Intent.createChooser(intent, "Share location"))
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Outlined.Share, contentDescription = "Share location",
                            tint = Color.White.copy(0.7f))
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Outlined.Close, null, tint = Color.White.copy(0.7f))
                    }
                }
            }

            // Map
            Box(Modifier.fillMaxWidth().weight(1f)) {
                GoogleMap(
                    modifier            = Modifier.fillMaxSize().then(
                        if (!isPro) Modifier.alpha(0.15f) else Modifier
                    ),
                    cameraPositionState = camState,
                    uiSettings = MapUiSettings(
                        zoomControlsEnabled     = false,
                        myLocationButtonEnabled = false,
                        compassEnabled          = true
                    ),
                    properties = MapProperties(
                        mapType           = MapType.NORMAL,
                        maxZoomPreference = 16f,
                        mapStyleOptions   = try {
                            MapStyleOptions.loadRawResourceStyle(context, R.raw.map_night)
                        } catch (_: Exception) { null }
                    )
                ) {
                    // Create all icons once Maps renderer is ready — must stay inside GoogleMap{}
                    MapEffect(Unit) { _ ->
                        if (trainIconState.value == null) {
                            try {
                                // Programmatic bitmap — body precisely centred so (0.5f,0.5f) anchor sits on the polyline
                                trainIconState.value = BitmapDescriptorFactory.fromBitmap(trainMarkerBitmap())
                            } catch (_: Exception) {}
                            try { passedIconState.value  = BitmapDescriptorFactory.fromBitmap(endpointDotBitmap(android.graphics.Color.rgb(160, 160, 160))) } catch (_: Exception) {}
                            try { currentIconState.value = BitmapDescriptorFactory.fromBitmap(endpointDotBitmap(android.graphics.Color.rgb(255, 107, 0))) } catch (_: Exception) {}
                            try { bellIconState.value    = BitmapDescriptorFactory.fromBitmap(upcomingStationBitmap(alerted = false)) } catch (_: Exception) {}
                            try { alertedIconState.value = BitmapDescriptorFactory.fromBitmap(upcomingStationBitmap(alerted = true)) }  catch (_: Exception) {}
                        }
                    }

                    // Route polyline — dark for completed journey, bright for remaining
                    if (passedCoords.size >= 2)
                        Polyline(points = passedCoords,   color = Color(0xFF1A4A8A), width = 14f, zIndex = 1f)
                    if (upcomingCoords.size >= 2)
                        Polyline(points = upcomingCoords, color = Color(0xFF4FAAFF), width = 14f, zIndex = 1f)

                    // Station markers — only key stops to keep map uncluttered
                    if (passedIcon != null && currentIcon != null && bellIcon != null && alertedIcon != null) {
                        routePoints.forEachIndexed { idx, (station, pos) ->
                            val isOrigin      = idx == 0
                            val isDestination = idx == routePoints.lastIndex
                            val isCurrent     = idx == currentStopIndex
                            val isNextOrTwo   = idx == currentStopIndex + 1 || idx == currentStopIndex + 2
                            val isAlerted     = alertedStations.contains(station.code)

                            val arrivalLabel  = station.arrival.orEmpty().ifBlank { "—" }
                            val platformLabel = station.platform.orEmpty().ifBlank { "—" }

                            when {
                                isCurrent -> Marker(
                                    state   = MarkerState(pos),
                                    icon    = currentIcon,
                                    anchor  = Offset(0.5f, 0.5f),
                                    title   = station.name.orEmpty().ifBlank { "—" },
                                    snippet = "Train is here · Pf $platformLabel",
                                    zIndex  = 2f
                                )
                                isOrigin || isDestination -> Marker(
                                    state   = MarkerState(pos),
                                    icon    = passedIcon,
                                    anchor  = Offset(0.5f, 0.5f),
                                    title   = station.name.orEmpty().ifBlank { "—" },
                                    snippet = arrivalLabel,
                                    zIndex  = 1f
                                )
                                isNextOrTwo -> Marker(
                                    state   = MarkerState(pos),
                                    icon    = if (isAlerted) alertedIcon else bellIcon,
                                    anchor  = Offset(0.5f, 0.5f),
                                    title   = station.name.orEmpty().ifBlank { "—" },
                                    snippet = if (isAlerted) "Alert set · ETA $arrivalLabel" else "Next stop · $arrivalLabel",
                                    zIndex  = 1f,
                                    onClick = { onAlertToggle(station); true }
                                )
                                // All other stations hidden — clean navigation view
                            }
                        }
                    }

                    // Animated train marker — icon stays null (default pin) until MapEffect sets it
                    if (coords.size >= 2) {
                        Marker(
                            state    = markerState,
                            title    = trainNumber,
                            snippet  = schedule.getOrNull(currentStopIndex)?.name ?: "",
                            icon     = trainIcon,   // null → default marker; no BitmapDescriptorFactory call outside MapEffect
                            rotation = heading,
                            flat     = true,
                            anchor   = Offset(0.5f, 0.5f),
                            zIndex   = 2f
                        )
                    }
                }

                // Custom zoom controls — replaces native buttons; + locks at maxZoomPreference (16f)
                val atMaxZoom = camState.position.zoom >= 16f
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 10.dp, bottom = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Surface(
                        onClick = {
                            if (!atMaxZoom) mapScope.launch { camState.animate(CameraUpdateFactory.zoomIn()) }
                        },
                        shape          = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp),
                        color          = if (atMaxZoom) Color(0xFF2C2C2C) else Color.White,
                        shadowElevation = 4.dp,
                        modifier       = Modifier.size(40.dp)
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            if (atMaxZoom) {
                                Icon(Icons.Outlined.Lock, contentDescription = "Max zoom reached",
                                    tint = Color(0xFF777777), modifier = Modifier.size(16.dp))
                            } else {
                                Text("+", fontSize = 22.sp, color = Color(0xFF333333),
                                    fontWeight = FontWeight.Light)
                            }
                        }
                    }
                    HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFDDDDDD))
                    Surface(
                        onClick        = { mapScope.launch { camState.animate(CameraUpdateFactory.zoomOut()) } },
                        shape          = RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp),
                        color          = Color.White,
                        shadowElevation = 4.dp,
                        modifier       = Modifier.size(40.dp)
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("−", fontSize = 22.sp, color = Color(0xFF333333),
                                fontWeight = FontWeight.Light)
                        }
                    }
                }

                // Blurred map paywall overlay — shown to free users
                if (!isPro) {
                    Box(
                        Modifier.fillMaxSize().background(Color(0xBB000000)),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            shape  = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2340)),
                            modifier = Modifier.padding(horizontal = 32.dp)
                        ) {
                            Column(
                                horizontalAlignment  = Alignment.CenterHorizontally,
                                verticalArrangement  = Arrangement.spacedBy(12.dp),
                                modifier             = Modifier.padding(28.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.Lock, null,
                                    tint     = Color(0xFFFF6B00),
                                    modifier = Modifier.size(40.dp)
                                )
                                Text(
                                    "Live Map is Pro",
                                    fontWeight = FontWeight.Bold,
                                    fontSize   = 18.sp,
                                    color      = Color.White
                                )
                                Text(
                                    "Watch the train move on Google Maps in real time — stations, ETA and all.",
                                    fontSize   = 13.sp,
                                    color      = Color(0xFFAAAAAA),
                                    textAlign  = TextAlign.Center
                                )
                                Button(
                                    onClick  = onUpgrade,
                                    colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B00)),
                                    shape    = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        "Unlock Pro · ₹49/mo",
                                        fontWeight = FontWeight.Bold,
                                        fontSize   = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Route summary footer
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val origin  = schedule.firstOrNull()?.name ?: "—"
                val dest    = schedule.lastOrNull()?.name  ?: "—"
                val current = schedule.getOrNull(currentStopIndex)?.name ?: "—"
                Column(Modifier.weight(1f)) {
                    Text(origin, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AppTitle)
                    Text(s.departure, fontSize = 10.sp, color = AppMuted)
                }
                Icon(Icons.Outlined.Train, null, tint = AppOrange, modifier = Modifier.size(18.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(current, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = AppOrange)
                    Text(s.nowAt, fontSize = 10.sp, color = AppMuted)
                }
                Icon(Icons.Outlined.Train, null, tint = AppOrange, modifier = Modifier.size(18.dp))
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text(dest, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AppTitle,
                        textAlign = TextAlign.End)
                    Text(s.trackArrow, fontSize = 10.sp, color = AppMuted)
                }
            }

            // Open in Google Maps CTA
            val mapsCoord = schedule.getOrNull(currentStopIndex)?.let { stationCoordFor(it.code) }
                ?: schedule.firstOrNull()?.let { stationCoordFor(it.code) }
            mapsCoord?.let { coord ->
                TextButton(
                    onClick  = {
                        val mapsUri = Uri.parse("geo:${coord.lat},${coord.lng}?q=${coord.lat},${coord.lng}")
                        context.startActivity(Intent(Intent.ACTION_VIEW, mapsUri))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 4.dp)
                ) {
                    Icon(Icons.Outlined.Map, null, tint = Color(0xFF1A73E8), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Open in Google Maps", color = Color(0xFF1A73E8), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }

            Spacer(Modifier.navigationBarsPadding())
        }
    }
}
