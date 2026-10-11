package com.yatrain.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.yatrain.app.i18n.LocalStrings
import com.yatrain.app.i18n.Language
import com.yatrain.app.i18n.MlKitTranslator
import com.yatrain.app.i18n.supportedLanguages
import com.yatrain.app.ui.theme.AppBorder
import com.yatrain.app.ui.theme.AppCard
import com.yatrain.app.ui.theme.AppMuted
import com.yatrain.app.ui.theme.AppNavy
import com.yatrain.app.ui.theme.AppOrange
import com.yatrain.app.ui.theme.AppTitle

@Composable
fun LanguagePickerDialog(
    currentCode: String,
    onSelect: (Language) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalStrings.current
    val translatorState by MlKitTranslator.state.collectAsState()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape     = RoundedCornerShape(22.dp),
            colors    = CardDefaults.cardColors(containerColor = AppCard),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Title
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        strings.selectLanguage,
                        fontSize   = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color      = AppTitle,
                        modifier   = Modifier.weight(1f)
                    )
                    TextButton(onClick = onDismiss) {
                        Text(strings.cancel, color = AppMuted)
                    }
                }

                HorizontalDivider(color = AppBorder, thickness = 0.5.dp)

                // Language list
                supportedLanguages.forEach { lang ->
                    val isSelected = lang.code == currentCode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(lang) }
                            .background(
                                if (isSelected) AppNavy.copy(alpha = 0.05f)
                                else Color.Transparent
                            )
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Language code badge
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) AppNavy else AppNavy.copy(alpha = 0.08f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                lang.buttonLabel,
                                fontSize   = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color      = if (isSelected) Color.White else AppNavy
                            )
                        }

                        Spacer(Modifier.width(14.dp))

                        // Names
                        Column(Modifier.weight(1f)) {
                            Text(
                                lang.nativeName,
                                fontSize   = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color      = AppTitle
                            )
                            Text(
                                lang.englishName,
                                fontSize = 12.sp,
                                color    = AppMuted
                            )
                        }

                        // Checkmark for selected
                        if (isSelected) {
                            Icon(
                                Icons.Filled.Check, null,
                                tint     = AppOrange,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    if (lang != supportedLanguages.last()) {
                        HorizontalDivider(
                            color    = AppBorder,
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    }
                }

                // Download progress banner — visible only while the ML Kit model downloads
                if (translatorState is MlKitTranslator.State.Downloading) {
                    HorizontalDivider(color = AppBorder, thickness = 0.5.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AppOrange.copy(alpha = 0.08f))
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier  = Modifier.size(14.dp),
                            color     = AppOrange,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Downloading language pack…",
                            fontSize = 12.sp,
                            color    = AppOrange
                        )
                    }
                } else if (translatorState is MlKitTranslator.State.Failed) {
                    HorizontalDivider(color = AppBorder, thickness = 0.5.dp)
                    Text(
                        "Translation unavailable — check your connection.",
                        fontSize = 11.sp,
                        color    = AppMuted,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
