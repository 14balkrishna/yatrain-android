package com.yatrain.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yatrain.app.i18n.LocalStrings
import com.yatrain.app.models.SubscriptionTier
import com.yatrain.app.ui.screens.subscription.SubscriptionViewModel
import com.yatrain.app.ui.theme.*

@Composable
fun ProBannerCard(
    viewModel: SubscriptionViewModel = viewModel(),
    isSignedIn: Boolean = true,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val s            = LocalStrings.current
    val tier         by viewModel.tier.collectAsStateWithLifecycle()
    val isPro        = tier == SubscriptionTier.PRO
    val monthlyPrice by viewModel.monthlyPrice.collectAsStateWithLifecycle()
    val annualPrice  by viewModel.annualPrice.collectAsStateWithLifecycle()

    if (!isSignedIn) {
        // Not signed in: muted gray state — prompt to sign in
        Box(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Zinc100)
                .clickable(onClick = onTap)
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = null,
                    tint     = AppMuted,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text       = "Yatrain Pro",
                        fontWeight = FontWeight.Bold,
                        fontSize   = 15.sp,
                        color      = AppMuted
                    )
                    Text(
                        text     = s.signInToUnlock,
                        fontSize = 12.sp,
                        color    = AppMuted.copy(alpha = 0.7f)
                    )
                }
                Icon(Icons.Default.ChevronRight, null, tint = AppMuted.copy(alpha = 0.5f))
            }
        }
    } else if (isPro) {
        // PRO state: navy background with crown icon
        Box(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(AppNavy)
                .clickable(onClick = onTap)
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.WorkspacePremium,
                    contentDescription = null,
                    tint     = AppOrange,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text       = "Yatrain Pro",
                        fontWeight = FontWeight.Bold,
                        fontSize   = 15.sp,
                        color      = Color.White
                    )
                    Text(
                        text     = s.proActiveMember,
                        fontSize = 12.sp,
                        color    = Color.White.copy(alpha = 0.7f)
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(AppOrange.copy(alpha = 0.2f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text       = s.pro,
                        fontSize   = 10.sp,
                        fontWeight = FontWeight.Black,
                        color      = AppOrange,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    } else {
        // FREE state: orange gradient background with star icon
        val orangeGradient = Brush.horizontalGradient(
            colors = listOf(AppOrange, AppOrangeDeep)
        )
        Box(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(orangeGradient)
                .clickable(onClick = onTap)
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = null,
                    tint     = Color.White,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text       = s.upgradeToPro,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 15.sp,
                        color      = Color.White
                    )
                    Text(
                        text     = "$monthlyPrice/mo · $annualPrice/yr",
                        fontSize = 12.sp,
                        color    = Color.White.copy(alpha = 0.85f)
                    )
                }
                Icon(Icons.Default.ChevronRight, null, tint = Color.White)
            }
        }
    }
}
