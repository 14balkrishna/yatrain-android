package com.yatrain.app.ui.screens.subscription

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yatrain.app.BuildConfig
import com.yatrain.app.i18n.LocalStrings
import com.yatrain.app.models.SubscriptionTier
import com.yatrain.app.ui.theme.*
import kotlinx.coroutines.launch

// ── Feature model ─────────────────────────────────────────────────────────────

private data class ProFeature(
    val icon: ImageVector,
    val name: String,
    val freeHas: Boolean,
    val proHas: Boolean = true
)

private val proFeatures = listOf(
    ProFeature(Icons.Outlined.ConfirmationNumber, "Ticket Search",          freeHas = true),
    ProFeature(Icons.Outlined.CalendarToday,      "Train Schedule",         freeHas = true),
    ProFeature(Icons.Outlined.DirectionsRailway,  "Basic Train Status",     freeHas = true),
    ProFeature(Icons.Outlined.MyLocation,         "Live GPS Tracking",      freeHas = false),
    ProFeature(Icons.Outlined.Map,                "Google Maps Tracking",   freeHas = false),
    ProFeature(Icons.Outlined.Train,              "Moving Train Animation", freeHas = false),
    ProFeature(Icons.Outlined.AccountTree,        "Coach Finder",           freeHas = false),
    ProFeature(Icons.Outlined.Explore,            "Travel Guide",           freeHas = false),
    ProFeature(Icons.Outlined.Smartphone,         "Platform Prediction",    freeHas = false),
    ProFeature(Icons.Outlined.NotificationsNone,  "Station Alerts",         freeHas = true),
    ProFeature(Icons.Outlined.BookmarkBorder,     "Saved Trains",           freeHas = true),
    ProFeature(Icons.Outlined.History,            "Journey History",        freeHas = false),
    ProFeature(Icons.Outlined.Cloud,              "Cloud Sync",             freeHas = false),
    ProFeature(Icons.Outlined.BarChart,           "Travel Analytics",       freeHas = false),
    ProFeature(Icons.Outlined.Download,           "Offline Journeys",       freeHas = false),
    ProFeature(Icons.Outlined.Group,              "Family Sharing",         freeHas = false),
    ProFeature(Icons.Outlined.GraphicEq,          "Live Activities",        freeHas = false),
    ProFeature(Icons.Outlined.RadioButtonUnchecked, "Dynamic Island",       freeHas = false),
)

// ── Sheet ─────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProPaywallSheet(
    viewModel: SubscriptionViewModel,
    onDismiss: () -> Unit
) {
    val context         = LocalContext.current
    val activity        = context as? Activity
    val tier            by viewModel.tier.collectAsStateWithLifecycle()
    val isPro           = tier == SubscriptionTier.PRO
    val isLoading       by viewModel.purchaseLoading.collectAsStateWithLifecycle()
    val purchaseError   by viewModel.purchaseError.collectAsStateWithLifecycle()
    val monthlyPrice    by viewModel.monthlyPrice.collectAsStateWithLifecycle()
    val annualPrice     by viewModel.annualPrice.collectAsStateWithLifecycle()
    val s               = LocalStrings.current
    val snackbarHost    = remember { SnackbarHostState() }
    val scope           = rememberCoroutineScope()

    // Show error in snackbar and auto-clear
    LaunchedEffect(purchaseError) {
        purchaseError?.let { msg ->
            scope.launch {
                snackbarHost.showSnackbar(msg, duration = SnackbarDuration.Long)
                viewModel.clearError()
            }
        }
    }

    // Auto-dismiss paywall once user becomes Pro
    LaunchedEffect(isPro) {
        if (isPro) onDismiss()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor   = Color.White,
        dragHandle       = null
    ) {
        Box(Modifier.fillMaxWidth()) {
        LazyColumn(
            modifier       = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {
            // ── Header ────────────────────────────────────────────────────────
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppNavy)
                        .padding(horizontal = 20.dp, vertical = 18.dp)
                ) {
                    // Cancel button (left)
                    TextButton(
                        onClick  = onDismiss,
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Text(s.cancel, color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                    }

                    // Title (center)
                    Column(
                        modifier              = Modifier.align(Alignment.Center),
                        horizontalAlignment   = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text       = "YATRAIN PRO",
                            fontSize   = 18.sp,
                            fontWeight = FontWeight.Black,
                            color      = AppOrange,
                            letterSpacing = 2.sp
                        )
                        Text(
                            text     = s.proTagline,
                            fontSize = 11.sp,
                            color    = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // ── Debug test-mode banner ────────────────────────────────────────
            if (BuildConfig.DEBUG) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFFF3CD))
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            "⚠ TEST MODE — purchases are simulated",
                            fontSize     = 11.sp,
                            fontWeight   = FontWeight.Bold,
                            color        = Color(0xFF856404)
                        )
                    }
                }
            }

            // ── Column headers ────────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppBackground)
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text       = "FEATURE",
                        fontSize   = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color      = AppMuted,
                        modifier   = Modifier.weight(1f)
                    )
                    Text(
                        text       = s.freeColumnHeader,
                        fontSize   = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color      = AppMuted,
                        modifier   = Modifier.width(52.dp),
                        textAlign  = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Text(
                        text       = s.proColumnHeader,
                        fontSize   = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color      = AppOrange,
                        modifier   = Modifier.width(52.dp),
                        textAlign  = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            // ── Feature rows ──────────────────────────────────────────────────
            items(proFeatures.size) { index ->
                FeatureRow(feature = proFeatures[index], isEven = index % 2 == 0)
            }

            // ── Purchase / Manage buttons ──────────────────────────────────────
            item { Spacer(Modifier.height(24.dp)) }

            if (isPro) {
                item {
                    // Manage Subscription button
                    OutlinedButton(
                        onClick  = { viewModel.manageSubscription(context) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .height(52.dp),
                        shape  = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AppOrange),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, AppOrange)
                    ) {
                        Icon(Icons.Outlined.ManageAccounts, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(s.manageSubscription, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            } else {
                item {
                    Row(
                        modifier              = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Monthly button
                        Button(
                            onClick  = { if (!isLoading) activity?.let { viewModel.purchaseMonthly(it) } },
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape    = RoundedCornerShape(14.dp),
                            colors   = ButtonDefaults.buttonColors(containerColor = AppNavy),
                            enabled  = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    color     = Color.White,
                                    modifier  = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        s.proMonthly,
                                        fontSize   = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color      = Color.White.copy(alpha = 0.75f)
                                    )
                                    Text(
                                        monthlyPrice,
                                        fontSize   = 16.sp,
                                        fontWeight = FontWeight.Black,
                                        color      = Color.White
                                    )
                                }
                            }
                        }

                        // Annual button with "Best Value" badge
                        Box(modifier = Modifier.weight(1f)) {
                            Button(
                                onClick  = { if (!isLoading) activity?.let { viewModel.purchaseAnnual(it) } },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape    = RoundedCornerShape(14.dp),
                                colors   = ButtonDefaults.buttonColors(containerColor = AppOrange),
                                enabled  = !isLoading
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        color     = Color.White,
                                        modifier  = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            s.proAnnual,
                                            fontSize   = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color      = Color.White.copy(alpha = 0.85f)
                                        )
                                        Text(
                                            annualPrice,
                                            fontSize   = 16.sp,
                                            fontWeight = FontWeight.Black,
                                            color      = Color.White
                                        )
                                    }
                                }
                            }

                            // "Best Value" pill badge
                            if (!isLoading) {
                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .offset(y = (-10).dp),
                                    shape  = RoundedCornerShape(50),
                                    color  = Color(0xFFFFD700)
                                ) {
                                    Text(
                                        text     = s.bestValue,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        color    = Color(0xFF7B4E00),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Restore purchases
                item {
                    Spacer(Modifier.height(12.dp))
                    TextButton(
                        onClick  = { viewModel.restore() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled  = !isLoading
                    ) {
                        Text(
                            s.restorePurchases,
                            fontSize = 13.sp,
                            color    = AppMuted
                        )
                    }
                }

                // Legal note
                item {
                    Text(
                        text     = "Subscriptions renew automatically. Cancel anytime in Google Play.",
                        fontSize = 10.sp,
                        color    = AppMuted,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }

        // Error snackbar overlaid at bottom of sheet
        SnackbarHost(
            hostState = snackbarHost,
            modifier  = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 8.dp)
        )
        }  // end Box
    }
}

// ── Single feature row ────────────────────────────────────────────────────────

@Composable
private fun FeatureRow(feature: ProFeature, isEven: Boolean) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .background(if (isEven) Color.White else AppBackground.copy(alpha = 0.5f))
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon badge
        Box(
            modifier         = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(AppNavy.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = feature.icon,
                contentDescription = null,
                tint     = AppNavy,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(Modifier.width(10.dp))

        // Feature name
        Text(
            text       = feature.name,
            fontSize   = 13.sp,
            fontWeight = FontWeight.Medium,
            color      = AppTitle,
            modifier   = Modifier.weight(1f)
        )

        // FREE cell
        Box(
            modifier         = Modifier.width(52.dp),
            contentAlignment = Alignment.Center
        ) {
            if (feature.freeHas) {
                Box(
                    modifier         = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(AppGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✓", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            } else {
                Text("✗", fontSize = 14.sp, color = Zinc400)
            }
        }

        // PRO cell
        Box(
            modifier         = Modifier.width(52.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier         = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(AppOrange),
                contentAlignment = Alignment.Center
            ) {
                Text("✓", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}
