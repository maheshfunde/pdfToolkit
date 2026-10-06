package com.yashodatech.pdftoolkit

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yashodatech.pdftoolkit.components.ModernAppHeader
import com.yashodatech.pdftoolkit.components.ThemeModePicker
import com.yashodatech.pdftoolkit.data.PreferencesManager
import com.yashodatech.pdftoolkit.data.ThemeMode
import com.yashodatech.pdftoolkit.utils.LinkHelper
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val prefsManager = remember { PreferencesManager(context) }
    val scope = rememberCoroutineScope()
    val themeMode by prefsManager.themeMode.collectAsState(
        initial = ThemeMode.SYSTEM.key
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Modern Standardized Header
            ModernAppHeader(
                title = "Settings",
                subtitle = "App preferences & legal information",
                icon = Icons.Outlined.Tune,
                accentColor = MaterialTheme.colorScheme.primary,
                onBack = onBackClick,
                actions = {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Text(
                            text = "v${Config.APP_VERSION}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                // 🎨 Appearance Section — toggle dark / light / system
                SectionHeader(title = "Appearance")

                ModernSettingsCard {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Theme",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Dark, light or follow your system",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        ThemeModePicker(
                            mode = themeMode,
                            onSelect = { mode ->
                                scope.launch { prefsManager.setThemeMode(mode) }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ⭐ App Section
                SectionHeader(title = "App")

                ModernSettingsCard {
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

                Spacer(modifier = Modifier.height(20.dp))

                // 💬 Support Section
                SectionHeader(title = "Support")

                ModernSettingsCard {
                    SettingsItem(
                        icon = Icons.Outlined.Email,
                        iconBgColor = Color(0x24E4572E),
                        iconColor = Color(0xFFE4572E),
                        title = "Contact Us",
                        subtitle = "Send feedback or report issues",
                        onClick = { LinkHelper.contactUs(context) }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 📜 Legal Section
                SectionHeader(title = "Legal")

                ModernSettingsCard {
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
                ModernSettingsCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            modifier = Modifier.size(54.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
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
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = "Version ${Config.APP_VERSION}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Merge • Split • Compress • Convert",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "100% Offline & Secure",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }
}

@Composable
private fun ModernSettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            content = content
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.6.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(
            start = 4.dp,
            bottom = 8.dp,
            top = 4.dp
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
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(42.dp),
            shape = RoundedCornerShape(12.dp),
            color = iconBgColor
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
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
        modifier = Modifier.padding(start = 56.dp),
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    )
}