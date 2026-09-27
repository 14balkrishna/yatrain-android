package com.yatrain.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yatrain.app.i18n.LanguageViewModel
import com.yatrain.app.i18n.LocalStrings
import com.yatrain.app.i18n.stringsForCode
import com.yatrain.app.navigation.YatrainNavGraph
import com.yatrain.app.ui.theme.YatrainTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val langVm: LanguageViewModel = viewModel()
            val langCode by langVm.languageCode.collectAsStateWithLifecycle()

            CompositionLocalProvider(LocalStrings provides stringsForCode(langCode)) {
                YatrainTheme {
                    YatrainNavGraph()
                }
            }
        }
    }
}
