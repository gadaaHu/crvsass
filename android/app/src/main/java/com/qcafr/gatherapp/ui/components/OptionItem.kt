package com.qcafr.gatherapp.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qcafr.gatherapp.data.model.QuestionOption
import com.qcafr.gatherapp.ui.theme.*

@Composable
fun OptionItem(
    option: QuestionOption,
    isSelected: Boolean,
    onSelect: () -> Unit,
    isAmharic: Boolean = true,
    modifier: Modifier = Modifier
) {
    // Robust normalization ensuring Amharic letters ሀ, ለ, ሐ, መ are strictly shown
    val normalizedKey = when (option.optionKey.trim().uppercase()) {
        "U", "A", "1" -> "ሀ"
        "B", "2" -> "ለ"
        "C", "3" -> "ሐ"
        "D", "4" -> "መ"
        else -> if (option.optionKey.isNotBlank()) option.optionKey.trim() else "•"
    }

    val isDark = MaterialTheme.appColors.isDark

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) PrimaryIndigo else BorderSubtle,
        label = "borderColor"
    )
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) {
            if (isDark) PrimaryIndigo.copy(alpha = 0.18f) else PrimaryIndigo.copy(alpha = 0.08f)
        } else BgCard,
        label = "bgColor"
    )
    val badgeBgColor by animateColorAsState(
        targetValue = if (isSelected) {
            PrimaryIndigo
        } else {
            if (isDark) Color(0x26FFFFFF) else Color(0x140F172A)
        },
        label = "badgeBgColor"
    )
    val badgeTextColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else TextWhite,
        label = "badgeTextColor"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onSelect() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Circle with option key: 'ሀ', 'ለ', 'ሐ', 'መ'
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(badgeBgColor)
                .border(
                    width = if (isSelected) 1.5.dp else 1.dp,
                    color = if (isSelected) PrimaryIndigo else BorderSubtle,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = normalizedKey,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = badgeTextColor
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            // Primary label (Amharic or English based on preference)
            Text(
                text = if (isAmharic) option.labelAm else option.labelEn,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) MaterialTheme.appColors.textPrimary else MaterialTheme.appColors.textPrimary,
                lineHeight = 22.sp
            )

            // Secondary label (other language, if different)
            val secondaryLabel = if (isAmharic) option.labelEn else option.labelAm
            if (secondaryLabel.isNotBlank() && secondaryLabel != (if (isAmharic) option.labelAm else option.labelEn)) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = secondaryLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.appColors.textMuted,
                    lineHeight = 18.sp
                )
            }
        }

        if (isSelected) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Selected",
                tint = PrimaryIndigo,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
