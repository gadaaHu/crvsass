package com.qcafr.gatherapp.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.qcafr.gatherapp.ui.theme.*

@Composable
fun RatingScale(
    selectedRating: Int?,
    onRatingSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.appColors
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "1 = በጭራሽ የለኝም (None)",
                color = colors.textDim,
                fontSize = 11.sp
            )
            Text(
                text = "5 = በጣም አለኝ (High)",
                color = colors.textDim,
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            for (rating in 1..5) {
                val isSelected = selectedRating == rating
                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) PrimaryIndigo else colors.bgCard,
                    label = "ratingBg"
                )
                val borderColor by animateColorAsState(
                    targetValue = if (isSelected) PrimaryIndigo else colors.borderSubtle,
                    label = "ratingBorder"
                )
                // Text on selected is always white; on unselected use theme primary text
                val textColor = if (isSelected) Color.White else colors.textPrimary

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(bgColor)
                        .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                        .clickable { onRatingSelected(rating) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = rating.toString(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = textColor
                    )
                }
            }
        }
    }
}
