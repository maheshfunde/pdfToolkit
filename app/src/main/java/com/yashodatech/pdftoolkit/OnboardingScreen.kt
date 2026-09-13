package com.yashodatech.pdftoolkit

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch


data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val gradient: List<Color>
)


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onComplete: () -> Unit) {

    val pages = listOf(
        OnboardingPage(
            icon = Icons.Rounded.PictureAsPdf,
            title = "All-in-One PDF Toolkit",
            description = "Convert, merge, split, compress and view PDF files — all in one beautiful app.",
            gradient = listOf(Color(0xFF26221E), Color(0xFF151210))
        ),
        OnboardingPage(
            icon = Icons.Rounded.Image,
            title = "Images to PDF",
            description = "Snap photos or pick from gallery. Crop, reorder, and convert to professional PDFs instantly.",
            gradient = listOf(Color(0xFF26221E), Color(0xFF151210))
        ),
        OnboardingPage(
            icon = Icons.Rounded.Security,
            title = "100% Private & Offline",
            description = "All processing happens on your device. Your files never leave your phone. No internet needed.",
            gradient = listOf(Color(0xFF26221E), Color(0xFF151210))
        ),
        OnboardingPage(
            icon = Icons.Rounded.Bolt,
            title = "Fast & Free",
            description = "Lightning fast processing with no file size limits. Completely free to use!",
            gradient = listOf(Color(0xFF26221E), Color(0xFF151210))
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
    ) {

        // ── Pages ───────────────────────────────────
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { pageIndex ->
            OnboardingPageContent(page = pages[pageIndex])
        }

        // ── Bottom Section ──────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ── Page Indicators ─────────────────────
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                repeat(pages.size) { index ->
                    val isActive = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(if (isActive) 28.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (isActive)
                                    Brush.horizontalGradient(pages[index].gradient)
                                else
                                    Brush.horizontalGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                                        )
                                    )
                            )
                            .animateContentSize(
                                animationSpec = spring(dampingRatio = 0.7f)
                            )
                    )
                }
            }

            // ── Action Button ───────────────────────
            val isLastPage = pagerState.currentPage == pages.size - 1

            Button(
                onClick = {
                    if (isLastPage) {
                        onComplete()
                    } else {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(pages[pagerState.currentPage].gradient),
                            RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isLastPage) "Get Started" else "Continue",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            // ── Skip Button ─────────────────────────
            if (!isLastPage) {
                TextButton(
                    onClick = onComplete,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text(
                        "Skip",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}


@Composable
private fun OnboardingPageContent(page: OnboardingPage) {

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Spacer(Modifier.weight(0.3f))

        // ── Animated Icon ───────────────────────────
        Box(
            modifier = Modifier
                .size(160.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            page.gradient[0].copy(alpha = 0.2f),
                            page.gradient[1].copy(alpha = 0.05f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(page.gradient)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = page.icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        Spacer(Modifier.height(48.dp))

        // ── Title ───────────────────────────────────
        Text(
            text = page.title,
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
            lineHeight = 34.sp
        )

        Spacer(Modifier.height(16.dp))

        // ── Description ─────────────────────────────
        Text(
            text = page.description,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            lineHeight = 24.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(Modifier.weight(0.5f))
    }
}