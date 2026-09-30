package com.yatrain.app.i18n

import androidx.compose.runtime.Composable

// Ordered longest-first so multi-word brands match before single words
private val trainTerms: List<Pair<String, Map<String, String>>> = listOf(
    "Vande Bharat" to mapOf(
        "hi" to "वंदे भारत",    "mr" to "वंदे भारत",    "gu" to "વંદે ભારત",
        "ta" to "வந்தே பாரத்",  "te" to "వందే భారత్",   "kn" to "ವಂದೇ ಭಾರತ್"
    ),
    "Jan Shatabdi" to mapOf(
        "hi" to "जन शताब्दी",   "mr" to "जन शताब्दी",   "gu" to "જન શતાબ્દી",
        "ta" to "ஜன் சதாப்தி",  "te" to "జన్ శతాబ్ది",  "kn" to "ಜನ್ ಶತಾಬ್ದಿ"
    ),
    "Garib Rath" to mapOf(
        "hi" to "गरीब रथ",      "mr" to "गरीब रथ",      "gu" to "ગરીબ રથ",
        "ta" to "கரீப் ரத்",   "te" to "గరీబ్ రత్",   "kn" to "ಗರೀಬ್ ರಥ"
    ),
    "Rajdhani" to mapOf(
        "hi" to "राजधानी",      "mr" to "राजधानी",      "gu" to "રાજધાની",
        "ta" to "ராஜ்தானி",    "te" to "రాజ్‌ధాని",   "kn" to "ರಾಜ್‌ಧಾನಿ"
    ),
    "Shatabdi" to mapOf(
        "hi" to "शताब्दी",      "mr" to "शताब्दी",      "gu" to "શતાબ્દી",
        "ta" to "சதாப்தி",      "te" to "శతాబ్ది",      "kn" to "ಶತಾಬ್ದಿ"
    ),
    "Duronto" to mapOf(
        "hi" to "दुरंतो",       "mr" to "दुरंतो",       "gu" to "દુરંતો",
        "ta" to "துரந்தோ",      "te" to "దురంతో",       "kn" to "ದುರಂತೋ"
    ),
    "Humsafar" to mapOf(
        "hi" to "हमसफर",        "mr" to "हमसफर",        "gu" to "હમસફર",
        "ta" to "ஹம்சஃபர்",    "te" to "హమ్‌సఫర్",    "kn" to "ಹಮ್‌ಸಫರ್"
    ),
    "Tejas" to mapOf(
        "hi" to "तेजस",         "mr" to "तेजस",         "gu" to "તેજસ",
        "ta" to "தேஜஸ்",        "te" to "తేజస్",        "kn" to "ತೇಜಸ್"
    ),
    "Antyodaya" to mapOf(
        "hi" to "अंत्योदय",     "mr" to "अंत्योदय",     "gu" to "અંત્યોદય",
        "ta" to "அந்த்யோதய",    "te" to "అంత్యోదయ",    "kn" to "ಅಂತ್ಯೋದಯ"
    ),
    "Suvidha" to mapOf(
        "hi" to "सुविधा",       "mr" to "सुविधा",       "gu" to "સુવિધા",
        "ta" to "சுவிதா",       "te" to "సువిధా",       "kn" to "ಸುವಿಧಾ"
    ),
    "Superfast" to mapOf(
        "hi" to "सुपरफास्ट",   "mr" to "सुपरफास्ट",   "gu" to "સુપરફાસ્ટ",
        "ta" to "சூப்பர்பாஸ்ட்","te" to "సూపర్‌ఫాస్ట్","kn" to "ಸೂಪರ್‌ಫಾಸ್ಟ್"
    ),
    "Intercity" to mapOf(
        "hi" to "इंटरसिटी",    "mr" to "इंटरसिटी",    "gu" to "ઇન્ટરસિટી",
        "ta" to "இன்டர்சிட்டி","te" to "ఇంటర్‌సిటీ", "kn" to "ಇಂಟರ್‌ಸಿಟಿ"
    ),
    "Special" to mapOf(
        "hi" to "स्पेशल",      "mr" to "स्पेशल",      "gu" to "સ્પેશ્યલ",
        "ta" to "ஸ்பெஷல்",     "te" to "స్పెషల్",     "kn" to "ಸ್ಪೆಷಲ್"
    ),
    "Express" to mapOf(
        "hi" to "एक्सप्रेस",  "mr" to "एक्सप्रेस",  "gu" to "એક્સપ્રેસ",
        "ta" to "எக்ஸ்பிரஸ்", "te" to "ఎక్స్‌ప్రెస్","kn" to "ಎಕ್ಸ್‌ಪ್ರೆಸ್"
    ),
    "Mail" to mapOf(
        "hi" to "मेल",          "mr" to "मेल",          "gu" to "મેઇલ",
        "ta" to "மெயில்",       "te" to "మెయిల్",       "kn" to "ಮೇಲ್"
    ),
)

fun translateTrainName(name: String, langCode: String): String {
    if (langCode == "en" || name.isBlank()) return name
    var result = name
    for ((english, translations) in trainTerms) {
        val local = translations[langCode] ?: continue
        result = result.replace(english, local, ignoreCase = true)
    }
    return result
}

@Composable
fun String.localTrainName(): String =
    translateTrainName(this, LocalStrings.current.langCode)
