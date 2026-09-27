package com.yatrain.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yatrain.app.R
import com.yatrain.app.i18n.LanguageViewModel
import com.yatrain.app.i18n.languageByCode
import com.yatrain.app.ui.theme.AppOrange

@Composable
fun HeroSection(
    eyebrow: String,
    title: String,
    titleAccent: String,
    modifier: Modifier = Modifier,
    bottomContent: @Composable (() -> Unit)? = null
) {
    val langVm: LanguageViewModel = viewModel()
    val langCode by langVm.languageCode.collectAsStateWithLifecycle()
    var showPicker by remember { mutableStateOf(false) }

    val topDark = Brush.verticalGradient(
        colorStops = arrayOf(
            0.00f to Color(0xBB000000),
            0.45f to Color(0x00000000)
        )
    )
    val bottomFade = Brush.verticalGradient(
        colorStops = arrayOf(
            0.50f to Color(0x00EFF3FB),
            1.00f to Color(0xFFEFF3FB)
        )
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 280.dp)
    ) {
        Image(
            painter            = painterResource(R.drawable.hero_bg),
            contentDescription = null,
            contentScale       = ContentScale.Crop,
            alignment          = Alignment.BottomCenter,
            modifier           = Modifier.matchParentSize()
        )
        Box(modifier = Modifier.matchParentSize().background(topDark))
        Box(modifier = Modifier.matchParentSize().background(bottomFade))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            // ── Header bar ─────────────────────────────────────────────────
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Yatrain logo image
                Image(
                    painter            = painterResource(R.drawable.yatrain_logo),
                    contentDescription = "Yatrain",
                    modifier           = Modifier
                        .height(36.dp)
                        .widthIn(max = 160.dp),
                    contentScale       = ContentScale.Fit,
                    alignment          = Alignment.CenterStart
                )

                // Language pill + notification bell
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    // Language pill — tappable
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50.dp))
                            .background(Color.White.copy(alpha = 0.25f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication        = null
                            ) { showPicker = true }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            languageByCode(langCode).buttonLabel,
                            fontSize   = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color      = Color.White
                        )
                    }

                    // Notification bell
                    Box(contentAlignment = Alignment.TopEnd) {
                        Box(
                            modifier         = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Notifications, "Notifications", tint = Color.White, modifier = Modifier.size(19.dp))
                        }
                        Box(
                            modifier         = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(AppOrange),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("4", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            Spacer(Modifier.height(88.dp))

            // ── Title block ────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .padding(bottom = if (bottomContent != null) 20.dp else 24.dp)
            ) {
                Text(eyebrow.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp, color = Color(0xFF555F6E))
                Spacer(Modifier.height(4.dp))
                Text(title,       fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0A1628))
                Text(titleAccent, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF005BA2))
            }

            if (bottomContent != null) {
                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                    bottomContent()
                }
            }
        }
    }

    // Language picker dialog
    if (showPicker) {
        LanguagePickerDialog(
            currentCode = langCode,
            onSelect    = { lang ->
                langVm.setLanguage(lang.code)
                showPicker = false
            },
            onDismiss = { showPicker = false }
        )
    }
}
