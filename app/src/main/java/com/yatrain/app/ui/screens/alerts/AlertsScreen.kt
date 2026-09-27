package com.yatrain.app.ui.screens.alerts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yatrain.app.i18n.LocalStrings
import com.yatrain.app.models.SavedStationAlert
import com.yatrain.app.models.SavedTicket
import com.yatrain.app.models.SavedTrain
import com.yatrain.app.ui.components.HeroSection
import com.yatrain.app.ui.components.ProBannerCard
import com.yatrain.app.ui.screens.subscription.ProPaywallSheet
import com.yatrain.app.ui.screens.subscription.SubscriptionViewModel
import com.yatrain.app.ui.theme.*

@Composable
fun AlertsScreen(
    viewModel:    SavedDataViewModel = viewModel(),
    onTrackTrain: (String) -> Unit = {},
    onSearchPnr:  (String) -> Unit = {}
) {
    val trains  by viewModel.trains.collectAsStateWithLifecycle()
    val tickets by viewModel.tickets.collectAsStateWithLifecycle()
    val alerts  by viewModel.alerts.collectAsStateWithLifecycle()
    var showPaywall by remember { mutableStateOf(false) }
    val subViewModel: SubscriptionViewModel = viewModel()
    val isPro       by subViewModel.isPro.collectAsStateWithLifecycle()

    // Reload data every time this screen becomes visible
    LaunchedEffect(Unit) { viewModel.reload() }

    LazyColumn(
        modifier       = Modifier.fillMaxSize().background(AppBackground),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            val s = LocalStrings.current
            HeroSection(eyebrow = s.sleepEyebrow, title = s.sleepTitle, titleAccent = s.sleepAccent)
        }

        item { Spacer(Modifier.height(20.dp)) }

        item {
            ProBannerCard(
                viewModel = subViewModel,
                onTap     = { showPaywall = true },
                modifier  = Modifier.padding(horizontal = 20.dp)
            )
        }

        item { Spacer(Modifier.height(16.dp)) }

        item { Box(Modifier.padding(horizontal = 20.dp)) { SmartSleepCard() } }

        // ── Active Station Alerts ─────────────────────────────────────────────
        item {
            val sAlerts = LocalStrings.current
            SectionHeader(
                label        = sAlerts.activeAlertsLabel,
                title        = sAlerts.stationAlertsTitle,
                count        = alerts.size,
                freeCapacity = if (isPro) null else "${alerts.size}/2"
            )
        }

        if (!isPro && alerts.size >= 2) {
            item {
                Box(Modifier.padding(horizontal = 20.dp)) {
                    AlertsProLockedCard(
                        title     = LocalStrings.current.alertLimitReached,
                        subtitle  = LocalStrings.current.alertLimitMsg,
                        onUpgrade = { showPaywall = true }
                    )
                }
            }
        }

        if (alerts.isEmpty()) {
            item {
                Box(Modifier.padding(horizontal = 20.dp)) {
                    EmptySavedCard(
                        icon    = Icons.Filled.NotificationsNone,
                        title   = LocalStrings.current.noAlertsTitle,
                        message = LocalStrings.current.noAlertsMsg
                    )
                }
            }
        } else {
            items(alerts, key = { it.id }) { alert ->
                AlertRecordRow(
                    alert    = alert,
                    onDelete = { viewModel.deleteAlert(alert.id) },
                    onTrack  = { if (alert.trainNumber.isNotBlank()) onTrackTrain(alert.trainNumber) }
                )
            }
        }

        // ── Saved Trains ──────────────────────────────────────────────────────
        item {
            SectionHeader(
                label   = LocalStrings.current.savedTrainsSection,
                title   = LocalStrings.current.yourWatchlist,
                count   = if (isPro) trains.size else 0
            )
        }

        if (!isPro) {
            item {
                Box(Modifier.padding(horizontal = 20.dp)) {
                    AlertsProLockedCard(
                        title     = LocalStrings.current.proSaveTrain,
                        onUpgrade = { showPaywall = true }
                    )
                }
            }
        } else if (trains.isEmpty()) {
            item {
                Box(Modifier.padding(horizontal = 20.dp)) {
                    EmptySavedCard(
                        icon    = Icons.Outlined.BookmarkAdd,
                        title   = LocalStrings.current.noSavedTrainsTitle,
                        message = LocalStrings.current.noSavedTrainsMsg
                    )
                }
            }
        } else {
            items(trains, key = { it.id }) { train ->
                SavedTrainRow(
                    train    = train,
                    onDelete = { viewModel.deleteTrain(train.id) },
                    onTrack  = { onTrackTrain(train.trainNumber) }
                )
            }
        }

        // ── Saved Tickets ─────────────────────────────────────────────────────
        item {
            val sTickets = LocalStrings.current
            SectionHeader(
                label   = sTickets.savedTicketsLabel,
                title   = sTickets.yourBookingsLabel,
                count   = if (isPro) tickets.size else 0
            )
        }

        if (!isPro) {
            item {
                Box(Modifier.padding(horizontal = 20.dp)) {
                    AlertsProLockedCard(
                        title     = LocalStrings.current.proSaveTicket,
                        onUpgrade = { showPaywall = true }
                    )
                }
            }
        } else if (tickets.isEmpty()) {
            item {
                val sEmpty = LocalStrings.current
                Box(Modifier.padding(horizontal = 20.dp)) {
                    EmptySavedCard(
                        icon    = Icons.Outlined.ConfirmationNumber,
                        title   = sEmpty.noSavedTicketsTitle,
                        message = sEmpty.noSavedTicketsMsg
                    )
                }
            }
        } else {
            items(tickets, key = { it.id }) { ticket ->
                SavedTicketRow(
                    ticket   = ticket,
                    onDelete = { viewModel.deleteTicket(ticket.id) },
                    onTrack  = { onSearchPnr(ticket.pnrNumber) }
                )
            }
        }
    }

    if (showPaywall) {
        ProPaywallSheet(viewModel = subViewModel, onDismiss = { showPaywall = false })
    }
}

// ── Section header ─────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(
    label: String,
    title: String,
    count: Int,
    freeCapacity: String? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Column {
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp, color = AppNavy)
            Text(title, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = AppTitle)
        }
        if (freeCapacity != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50.dp))
                    .background(Color(0xFFF59E0B).copy(0.15f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(freeCapacity, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B))
            }
        } else if (count > 0) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50.dp))
                    .background(AppNavy)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text("$count", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

// ── Alerts Pro locked card ─────────────────────────────────────────────────────

@Composable
private fun AlertsProLockedCard(
    title: String,
    subtitle: String = LocalStrings.current.upgradeDesc,
    onUpgrade: () -> Unit
) {
    val s = LocalStrings.current
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = AppCard),
        border    = BorderStroke(1.dp, AppNavy.copy(0.15f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(AppNavy.copy(0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Lock, null, tint = AppNavy, modifier = Modifier.size(20.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AppTitle)
                Text(subtitle, fontSize = 11.sp, color = AppMuted, lineHeight = 14.sp)
            }
            androidx.compose.material3.Button(
                onClick        = onUpgrade,
                shape          = RoundedCornerShape(10.dp),
                colors         = ButtonDefaults.buttonColors(containerColor = AppOrange),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(s.upgradeToPro, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

// ── Alert record row ───────────────────────────────────────────────────────────

@Composable
private fun AlertRecordRow(
    alert:    SavedStationAlert,
    onDelete: () -> Unit,
    onTrack:  (() -> Unit)? = null
) {
    Card(
        modifier  = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = AppCard),
        border    = BorderStroke(1.dp, AppBorder),
        elevation = CardDefaults.cardElevation(0.dp),
        onClick   = { onTrack?.invoke() }
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier         = Modifier.size(42.dp).clip(CircleShape).background(AppOrange.copy(0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.NotificationsActive, null, tint = AppOrange, modifier = Modifier.size(22.dp))
            }

            Spacer(Modifier.width(14.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    alert.stationName,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color      = AppNavy,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(alert.trainName.ifBlank { alert.trainNumber }, fontSize = 12.sp, color = AppMuted, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                }
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier              = Modifier.padding(top = 4.dp)
                ) {
                    AlertPill("${alert.minutesBefore} min before")
                    if (alert.eta.isNotBlank()) AlertPill("ETA ${alert.eta}")
                }
            }

            if (onTrack != null) {
                IconButton(onClick = onTrack, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Outlined.ArrowForward, "Track train", tint = AppOrange, modifier = Modifier.size(16.dp))
                }
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Close, "Delete alert", tint = AppMuted, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun AlertPill(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50.dp))
            .background(AppOrange.copy(0.10f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = AppOrange)
    }
}

// ── Saved train row ────────────────────────────────────────────────────────────

@Composable
private fun SavedTrainRow(
    train:    SavedTrain,
    onDelete: () -> Unit,
    onTrack:  (() -> Unit)? = null
) {
    Card(
        modifier  = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = AppCard),
        border    = BorderStroke(1.dp, AppBorder),
        elevation = CardDefaults.cardElevation(0.dp),
        onClick   = { onTrack?.invoke() }
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier         = Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(AppNavy.copy(0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.DirectionsRailway, null, tint = AppNavy, modifier = Modifier.size(22.dp))
            }

            Spacer(Modifier.width(14.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    train.trainName,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color      = AppNavy,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
                Text(
                    train.trainNumber,
                    fontSize   = 12.sp,
                    color      = AppOrange,
                    fontWeight = FontWeight.SemiBold
                )
                if (train.from.isNotBlank() && train.to.isNotBlank()) {
                    Text(
                        "${train.from} → ${train.to}",
                        fontSize = 11.sp,
                        color    = AppMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (onTrack != null) {
                IconButton(onClick = onTrack, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Outlined.ArrowForward, "Track train", tint = AppOrange, modifier = Modifier.size(16.dp))
                }
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Close, "Remove train", tint = AppMuted, modifier = Modifier.size(16.dp))
            }
        }
    }
}

// ── Saved ticket row ───────────────────────────────────────────────────────────

@Composable
private fun SavedTicketRow(
    ticket:   SavedTicket,
    onDelete: () -> Unit,
    onTrack:  (() -> Unit)? = null
) {
    val isConfirmed = ticket.currentStatus.contains("CNF", ignoreCase = true)
    val isWaiting   = ticket.currentStatus.contains("WL",  ignoreCase = true)
    val statusColor = when { isConfirmed -> AppGreen; isWaiting -> AppOrange; else -> AppMuted }
    val statusLabel = ticket.currentStatus.ifBlank { "UNKNOWN" }

    Card(
        modifier  = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = AppCard),
        border    = BorderStroke(1.dp, AppBorder),
        elevation = CardDefaults.cardElevation(0.dp),
        onClick   = { onTrack?.invoke() }
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier         = Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(statusColor.copy(0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.ConfirmationNumber, null, tint = statusColor, modifier = Modifier.size(22.dp))
            }

            Spacer(Modifier.width(14.dp))

            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        ticket.pnrNumber,
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color      = AppNavy
                    )
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(statusColor.copy(0.12f)).padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(statusLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = statusColor)
                    }
                }
                Text(
                    ticket.trainName.ifBlank { ticket.trainNumber },
                    fontSize = 12.sp, color = AppMuted, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                if (ticket.coach.isNotBlank() || ticket.berth.isNotBlank()) {
                    Text(
                        buildString {
                            if (ticket.coach.isNotBlank()) append("Coach ${ticket.coach}")
                            if (ticket.berth.isNotBlank()) { if (ticket.coach.isNotBlank()) append("  ·  "); append("Berth ${ticket.berth}") }
                        },
                        fontSize = 11.sp, color = AppOrange, fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (onTrack != null) {
                IconButton(onClick = onTrack, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Outlined.ArrowForward, "Check PNR status", tint = AppOrange, modifier = Modifier.size(16.dp))
                }
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Close, "Remove ticket", tint = AppMuted, modifier = Modifier.size(16.dp))
            }
        }
    }
}

// ── Smart Sleep card ───────────────────────────────────────────────────────────

@Composable
private fun SmartSleepCard() {
    val s = LocalStrings.current
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(18.dp),
        colors    = CardDefaults.cardColors(containerColor = AppNavy),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier         = Modifier.size(52.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.NightsStay, null, tint = Color.White, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(s.smartSleep, fontWeight = FontWeight.Black, fontSize = 17.sp, color = Color.White)
                Spacer(Modifier.height(3.dp))
                Text(s.smartSleepDesc, fontSize = 12.sp, color = Color.White.copy(alpha = 0.75f), lineHeight = 18.sp)
            }
        }
    }
}

// ── Empty state card ───────────────────────────────────────────────────────────

@Composable
private fun EmptySavedCard(icon: ImageVector, title: String, message: String) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = AppCard),
        border    = BorderStroke(1.dp, AppBorder),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier            = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier         = Modifier.size(52.dp).clip(CircleShape).background(AppOrange.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = AppOrange, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AppTitle)
            Spacer(Modifier.height(6.dp))
            Text(
                message,
                fontSize  = 12.sp,
                color     = Zinc500,
                lineHeight = 18.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
