package com.ourhouse.missions.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.ourhouse.missions.R

// Heebo via Android's Downloadable Fonts API (see res/values/font_certs.xml for the
// provider's certs): Play Services fetches and caches the .ttf on-device the first time
// it's needed, so no font files are bundled into the APK. If Play Services is ever
// unavailable, Compose transparently falls back to the system default font instead of
// crashing, so this is safe on every device.
private val heeboProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

private val heebo = GoogleFont("Heebo")

private val HeeboFontFamily = FontFamily(
    Font(googleFont = heebo, fontProvider = heeboProvider, weight = FontWeight.Normal),
    Font(googleFont = heebo, fontProvider = heeboProvider, weight = FontWeight.Medium),
    Font(googleFont = heebo, fontProvider = heeboProvider, weight = FontWeight.SemiBold),
    Font(googleFont = heebo, fontProvider = heeboProvider, weight = FontWeight.Bold),
    Font(googleFont = heebo, fontProvider = heeboProvider, weight = FontWeight.ExtraBold)
)

val MissionsTypography = Typography(
    titleLarge = TextStyle(fontFamily = HeeboFontFamily, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp),
    titleMedium = TextStyle(fontFamily = HeeboFontFamily, fontWeight = FontWeight.Bold, fontSize = 17.sp),
    bodyLarge = TextStyle(fontFamily = HeeboFontFamily, fontWeight = FontWeight.Normal, fontSize = 15.sp),
    bodyMedium = TextStyle(fontFamily = HeeboFontFamily, fontWeight = FontWeight.Normal, fontSize = 13.5.sp),
    labelLarge = TextStyle(fontFamily = HeeboFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp),
    labelMedium = TextStyle(fontFamily = HeeboFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
    labelSmall = TextStyle(fontFamily = HeeboFontFamily, fontWeight = FontWeight.Bold, fontSize = 11.sp)
)
