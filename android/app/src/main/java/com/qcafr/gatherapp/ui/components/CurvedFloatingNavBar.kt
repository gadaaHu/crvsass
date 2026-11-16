package com.qcafr.gatherapp.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class CurvedNavItem(
    val id: String,
    val icon: ImageVector,
    val title: String,
    val hasBadge: Boolean = false,
    val onClick: () -> Unit
)

/**
 * Premium floating capsule navigation bar.
 * - Adapts background to light/dark mode
 * - Prominent soft shadow with depth (no hard border)
 * - Active item elevated bubble with teal highlight
 */
@Composable
fun CurvedFloatingNavBar(
    items: List<CurvedNavItem>,
    selectedIndex: Int,
    isDarkMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return

    // Theme-aware colours
    val pillBg = if (isDarkMode)
        Brush.verticalGradient(
            listOf(
                Color(0xF2161F2E),
                Color(0xF80F1622),
                Color(0xFF0B1019)
            )
        )
    else
        Brush.verticalGradient(
            listOf(
                Color(0xFFFFFFFF),
                Color(0xFFFFFFFF),
                Color(0xFFF9FAFB)
            )
        )

    val activeBubbleBg = if (isDarkMode)
        Brush.radialGradient(
            listOf(
                Color(0xFF1A2638),
                Color(0xFF101724)
            )
        )
    else
        Brush.radialGradient(
            listOf(
                Color(0xFFFFFFFF),
                Color(0xFFCCFBF1)
            )
        )

    val activeColor = if (isDarkMode) Color(0xFF2DD4BF) else Color(0xFF0D9488)
    val inactiveIconColor = if (isDarkMode) Color(0xFF8B9CB6) else Color(0xFF64748B)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(84.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        // 1. Base capsule pill with deep visible elevation shadow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .shadow(
                    elevation = if (isDarkMode) 12.dp else 18.dp,
                    shape = RoundedCornerShape(32.dp),
                    clip = false,
                    ambientColor = if (isDarkMode) Color(0xFF2DD4BF) else Color.Black,
                    spotColor = if (isDarkMode) Color(0xFF0F766E) else Color.Black
                )
                .clip(RoundedCornerShape(32.dp))
                .background(pillBg)
        ) {
            // Bottom neon teal glow line in dark mode
            if (isDarkMode) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.78f)
                        .height(2.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color.Transparent,
                                    Color(0x442DD4BF),
                                    Color(0xFF2DD4BF),
                                    Color(0x442DD4BF),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }
        }

        // 2. Navigation items row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(84.dp)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            items.forEachIndexed { index, item ->
                val isSelected = index == selectedIndex
                val interactionSource = remember { MutableInteractionSource() }

                val itemColor by animateColorAsState(
                    targetValue = if (isSelected) activeColor else inactiveIconColor,
                    animationSpec = tween(220),
                    label = "navColor"
                )

                if (isSelected) {
                    // Elevated active item with floating bubble
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .height(84.dp)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null
                            ) { item.onClick() },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Top
                    ) {
                        // Elevated bubble popping out above the capsule with its own drop shadow
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .shadow(
                                    elevation = if (isDarkMode) 6.dp else 8.dp,
                                    shape = CircleShape,
                                    clip = false,
                                    ambientColor = if (isDarkMode) Color(0xFF2DD4BF) else Color.Black,
                                    spotColor = if (isDarkMode) Color(0xFF0D9488) else Color.Black
                                )
                                .clip(CircleShape)
                                .background(activeBubbleBg)
                                .border(
                                    BorderStroke(
                                        1.5.dp,
                                        Brush.linearGradient(
                                            listOf(
                                                activeColor,
                                                if (isDarkMode) Color(0xFF0F766E) else Color(0xFF14B8A6)
                                            )
                                        )
                                    ),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.title,
                                tint = activeColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(Modifier.height(2.dp))

                        Text(
                            text = item.title,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = activeColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(Modifier.height(2.dp))

                        // Glowing active indicator dot
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(activeColor)
                        )
                    }
                } else {
                    // Standard inactive item
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null
                            ) { item.onClick() }
                            .padding(bottom = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(contentAlignment = Alignment.TopEnd) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.title,
                                tint = itemColor,
                                modifier = Modifier.size(21.dp)
                            )

                            // Red notification dot for items like AI Tutor
                            if (item.hasBadge) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .offset(x = 4.dp, y = (-2).dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEF4444))
                                        .border(1.dp, if (isDarkMode) Color(0xFF101624) else Color(0xFFFFFFFF), CircleShape)
                                )
                            }
                        }

                        Spacer(Modifier.height(3.dp))

                        Text(
                            text = item.title,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = itemColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(Modifier.height(6.dp))
                    }
                }
            }
        }
    }
}
