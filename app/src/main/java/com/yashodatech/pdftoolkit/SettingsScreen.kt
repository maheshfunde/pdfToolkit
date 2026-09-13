package com.yashodatech.pdftoolkit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yashodatech.pdftoolkit.components.LiquidGlassCard
import com.yashodatech.pdftoolkit.components.LiquidHeader
import com.yashodatech.pdftoolkit.theme.GlassHeaderGradient
import com.yashodatech.pdftoolkit.utils.LinkHelper

@Composable
fun SettingsScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            // Liquid Gradient Header
            LiquidHeader(
                title = "Settings",
                subtitle = "App preferences & legal information",
                icon = Icons.Outlined.Tune,
                gradientColors = GlassHeaderGradient,
                onBackClick = onBackClick,
                statusBadge = "v${Config.APP_VERSION}"
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // ⭐ App Section
                SectionHeader(title = "App")

                LiquidGlassCard {
                    SettingsItem(
                        icon = Icons.Outlined.Star,
                        iconBgColor = Color(0x24E4572E),
                        iconColor = Color(0xFFE4572E),
                        title = "Rate App",
                        subtitle = "Love the app? Rate us 5 stars!",
                        onClick = { LinkHelper.rateApp(context) }
                    )

                    SettingsDivider()

                    SettingsItem(
                        icon = Icons.Outlined.Share,
                        iconBgColor = Color(0x24E4572E),
                        iconColor = Color(0xFFE5484D),
                        title = "Share App",
                        subtitle = "Share with friends & family",
                        onClick = { LinkHelper.shareApp(context) }
                    )

                    SettingsDivider()

                    SettingsItem(
                        icon = Icons.Outlined.Apps,
                        iconBgColor = Color(0x24E11D48),
                        iconColor = Color(0xFFE4572E),
                        title = "More Apps",
                        subtitle = "Check out our other applications",
                        onClick = { LinkHelper.moreApps(context) }
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 💬 Support Section
                SectionHeader(title = "Support")

                LiquidGlassCard {
                    SettingsItem(
                        icon = Icons.Outlined.Email,
                        iconBgColor = Color(0x24E4572E),
                        iconColor = Color(0xFFE4572E),
                        title = "Contact Us",
                        subtitle = "Send feedback or report issues",
                        onClick = { LinkHelper.contactUs(context) }
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 📜 Legal Section
                SectionHeader(title = "Legal")

                LiquidGlassCard {
                    SettingsItem(
                        icon = Icons.Outlined.PrivacyTip,
                        iconBgColor = Color(0x24BE123C),
                        iconColor = Color(0xFFFDA4AF),
                        title = "Privacy Policy",
                        subtitle = "Read our privacy policy",
                        onClick = { LinkHelper.openPrivacyPolicy(context) }
                    )

                    SettingsDivider()

                    SettingsItem(
                        icon = Icons.Outlined.Description,
                        iconBgColor = Color(0x24E11D48),
                        iconColor = Color(0xFFE4572E),
                        title = "Disclaimer",
                        subtitle = "Read our disclaimer",
                        onClick = { LinkHelper.openDisclaimer(context) }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ℹ️ App Info
                LiquidGlassCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            modifier = Modifier.size(54.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Outlined.PictureAsPdf,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = Config.APP_NAME,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            text = "Version ${Config.APP_VERSION}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Merge • Split • Compress • Convert",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "100% Offline & Secure",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(
            start = 4.dp,
            bottom = 6.dp,
            top = 2.dp
        )
    )
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    iconBgColor: Color,
    iconColor: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = RoundedCornerShape(12.dp),
            color = iconBgColor
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 64.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
    )
}