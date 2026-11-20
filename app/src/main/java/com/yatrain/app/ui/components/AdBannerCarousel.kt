package com.yatrain.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.yatrain.app.BuildConfig
import com.yatrain.app.i18n.LocalStrings
import com.yatrain.app.ui.theme.AppBorder
import com.yatrain.app.ui.theme.AppMuted
import com.yatrain.app.ui.theme.AppNavy
import com.yatrain.app.ui.theme.Zinc500
import kotlinx.coroutines.delay

private val bannerUnitIds = listOf(
    BuildConfig.ADMOB_BANNER_1,
    BuildConfig.ADMOB_BANNER_2,
    BuildConfig.ADMOB_BANNER_3,
)

// Auto-advancing banner carousel — 3 slots, 8-second rotation, hidden for Pro users.
@Composable
fun AdBannerCarousel(
    isPro: Boolean,
    modifier: Modifier = Modifier
) {
    if (isPro) return

    val s = LocalStrings.current
    val totalPages = Int.MAX_VALUE
    val startPage  = totalPages / 2
    val pagerState = rememberPagerState(initialPage = startPage, pageCount = { totalPages })

    LaunchedEffect(pagerState) {
        while (true) {
            delay(8_000L)
            pagerState.animateScrollToPage(pagerState.currentPage + 1)
        }
    }

    Column(modifier = modifier) {
        // Section header
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(
                s.partnerOffers,
                fontSize     = 11.sp,
                fontWeight   = FontWeight.Bold,
                letterSpacing = 1.sp,
                color        = AppMuted
            )
            Text(s.ads, fontSize = 10.sp, color = Zinc500, fontWeight = FontWeight.Medium)
        }

        // Pager with one real AdView per page
        HorizontalPager(
            state              = pagerState,
            beyondViewportPageCount = 0,         // don't preload off-screen ads
            modifier           = Modifier.fillMaxWidth()
        ) { pageIndex ->
            val adUnitId = bannerUnitIds[pageIndex % bannerUnitIds.size]
            BannerAdSlot(adUnitId = adUnitId)
        }

        // Page-dot indicator
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            repeat(bannerUnitIds.size) { idx ->
                val active = (pagerState.currentPage % bannerUnitIds.size) == idx
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .size(if (active) 7.dp else 5.dp)
                        .clip(CircleShape)
                        .background(if (active) AppNavy else AppBorder)
                )
            }
        }
    }
}

@Composable
private fun BannerAdSlot(adUnitId: String) {
    val context = LocalContext.current
    val adView  = remember(adUnitId) {
        AdView(context).apply {
            setAdSize(AdSize.BANNER)
            setAdUnitId(adUnitId)
        }
    }

    DisposableEffect(adView) {
        adView.loadAd(AdRequest.Builder().build())
        onDispose { adView.destroy() }
    }

    Box(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White),
        contentAlignment  = Alignment.Center
    ) {
        AndroidView(
            factory  = { adView },
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        )
    }
}
