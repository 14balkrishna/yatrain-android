package com.yatrain.app.ui.screens.profile

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yatrain.app.i18n.LocalStrings
import com.yatrain.app.ui.components.HeroSection
import com.yatrain.app.ui.components.ProBannerCard
import com.yatrain.app.ui.screens.subscription.ProPaywallSheet
import com.yatrain.app.ui.screens.subscription.SubscriptionViewModel
import com.yatrain.app.ui.theme.*
import com.yatrain.app.utils.Constants

@Composable
fun ProfileScreen(
    onNavigateToSleep: () -> Unit = {}
) {
    val context           = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope             = rememberCoroutineScope()
    var showPaywall       by remember { mutableStateOf(false) }
    val subViewModel: SubscriptionViewModel = viewModel()

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBackground)
                .verticalScroll(rememberScrollState())
        ) {
            val s = LocalStrings.current

            HeroSection(
                eyebrow     = s.profileEyebrow,
                title       = s.profileTitle,
                titleAccent = "Traveller."
            )

            IdentityCard(
                initials    = "YT",
                displayName = "Traveller",
                email       = null,
                modifier    = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .offset(y = (-24).dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(Modifier.height(8.dp))
                Text(s.yatrainPro, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    letterSpacing = 1.8.sp, color = AppNavy)
                Spacer(Modifier.height(12.dp))
                ProBannerCard(viewModel = subViewModel, isSignedIn = true, onTap = { showPaywall = true })

                Spacer(Modifier.height(24.dp))
                Text(s.travel, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    letterSpacing = 1.8.sp, color = AppNavy)
                Spacer(Modifier.height(12.dp))
                ProfileMenuCard {
                    ProfileRow(Icons.Outlined.BookmarkBorder, s.savedTrainsMenu) {
                        onNavigateToSleep()
                    }
                    HorizontalDivider(color = AppBorder, thickness = 0.5.dp)
                    ProfileRow(Icons.Outlined.History, s.searchHistory) {
                        scope.launch { snackbarHostState.showSnackbar("Coming soon") }
                    }
                    HorizontalDivider(color = AppBorder, thickness = 0.5.dp)
                    ProfileRow(Icons.Outlined.Notifications, s.notificationPrefs) {
                        context.startActivity(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            }
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))
                Text(s.about, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    letterSpacing = 1.8.sp, color = AppNavy)
                Spacer(Modifier.height(12.dp))
                ProfileMenuCard {
                    ProfileRow(Icons.Outlined.Lock, s.privacyPolicy) {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Constants.PRIVACY_URL)))
                    }
                    HorizontalDivider(color = AppBorder, thickness = 0.5.dp)
                    ProfileRow(Icons.Outlined.Description, s.termsOfUse) {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Constants.TERMS_URL)))
                    }
                    HorizontalDivider(color = AppBorder, thickness = 0.5.dp)
                    ProfileRow(Icons.Outlined.HeadsetMic, s.support) {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Constants.SUPPORT_URL)))
                    }
                    HorizontalDivider(color = AppBorder, thickness = 0.5.dp)
                    ProfileRow(Icons.Outlined.Mail, s.contact) {
                        context.startActivity(
                            Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${Constants.CONTACT_EMAIL}"))
                        )
                    }
                }

                Spacer(Modifier.height(32.dp))
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier  = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        )
    }

    if (showPaywall) {
        ProPaywallSheet(viewModel = subViewModel, onDismiss = { showPaywall = false })
    }
}

// ── Identity card ─────────────────────────────────────────────────────────────

@Composable
private fun IdentityCard(
    initials: String,
    displayName: String,
    email: String?,
    modifier: Modifier = Modifier
) {
    val s = LocalStrings.current
    Card(
        modifier  = modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(22.dp),
        colors    = CardDefaults.cardColors(containerColor = AppCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(s.yatrainMember, fontSize = 9.sp, fontWeight = FontWeight.Black,
                    letterSpacing = 2.5.sp, color = AppNavy.copy(alpha = 0.5f))
                Text(s.pro, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                    letterSpacing = 1.8.sp, color = AppOrange)
            }

            Row(
                modifier          = Modifier.padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier         = Modifier.size(64.dp).clip(CircleShape).background(AppNavy),
                    contentAlignment = Alignment.Center
                ) {
                    Text(initials, fontSize = 22.sp, fontWeight = FontWeight.Black, color = AppOrange)
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(s.passenger, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp, color = Zinc400)
                    Spacer(Modifier.height(3.dp))
                    Text(displayName, fontSize = 20.sp, fontWeight = FontWeight.Black,
                        color = AppNavy, maxLines = 1)
                    if (email != null) {
                        Text(email, fontSize = 11.sp, color = Zinc400, maxLines = 1)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = AppBorder, thickness = 1.dp)
            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem("0", s.trips)
                Box(Modifier.width(1.dp).height(32.dp).background(AppBorder))
                StatItem("0", s.savedStat)
                Box(Modifier.width(1.dp).height(32.dp).background(AppBorder))
                StatItem("0", s.alertsStat)
            }
        }
    }
}

@Composable
private fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Black, color = AppNavy)
        Text(label, fontSize = 11.sp, color = Zinc400)
    }
}

// ── Profile menu card ─────────────────────────────────────────────────────────

@Composable
private fun ProfileMenuCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = AppCard),
        border    = BorderStroke(1.dp, AppBorder),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(content = content)
    }
}

// ── Single profile row ────────────────────────────────────────────────────────

@Composable
private fun ProfileRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier         = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(AppNavy.copy(alpha = 0.07f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = AppNavy, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
            color = AppTitle, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, null, tint = Zinc300, modifier = Modifier.size(18.dp))
    }
}
