package com.yashodatech.pdftoolkit.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.yashodatech.pdftoolkit.theme.*

@Composable
fun LiquidHeader(
    title: String,
    subtitle: String,
    icon: ImageVector,
    gradientColors: List<Color> = ModernHeaderGradient,
    onBackClick: () -> Unit,
    statusBadge: String? = "100% Offline"
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(InkSurface)
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back button — quiet circle on raised tile
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(InkSurfaceElevated)
                            .clickable(onClick = onBackClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = InkBone,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    // Tool mark — mono glyph on a raised tile
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(InkSurfaceElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = InkBone,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = title,
                            color = InkBone,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.sp
                        )
                        Text(
                            text = subtitle,
                            color = InkMuted,
                            fontSize = 11.5.sp,
                            maxLines = 1
                        )
                    }
                }

                if (statusBadge != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(InkSurfaceElevated)
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(SuccessGreen)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = statusBadge,
                                color = InkMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // The one signature stamp — a thin vermilion rule under the header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(Vermilion)
        )
    }
}

@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.97f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "bouncyScale"
    )

    Card(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(
                if (onClick != null) {
                    Modifier.pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                isPressed = true
                                tryAwaitRelease()
                                isPressed = false
                            },
                            onTap = {
                                onClick()
                            }
                        )
                    }
                } else Modifier
            ),
        shape = RoundedCornerShape(cornerRadius),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun ModernGlassLoader(
    isShowing: Boolean,
    title: String = "Processing PDF...",
    progress: Float? = null,
    statusText: String = "Please wait a moment"
) {
    if (!isShowing) return

    val infiniteTransition = rememberInfiniteTransition(label = "loaderRotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, InkBorder),
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                ,
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .rotate(rotation)
                            .clip(CircleShape)
                            .background(
                                Brush.sweepGradient(
                                    listOf(
                                        Vermilion,
                                        VermilionDeep,
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        if (progress != null) {
                            Text(
                                text = "${(progress * 100).toInt()}%",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Vermilion
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.Shield,
                                contentDescription = null,
                                tint = Vermilion,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = statusText,
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                if (progress != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(50)),
                        color = Vermilion,
                        trackColor = Vermilion.copy(alpha = 0.15f)
                    )
                }
            }
        }
    }
}

@Composable
fun LiquidPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    gradientColors: List<Color> = ButtonSapphireGradient
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Vermilion,
            contentColor = InkBone,
            disabledContainerColor = InkSurfaceElevated,
            disabledContentColor = InkFaint
        ),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = text,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.2.sp
            )
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}