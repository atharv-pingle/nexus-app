package com.nexus.planner.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.planner.ui.theme.*

// Chamfered Shape (tactical cut corners)
val ChamferCorner = CutCornerShape(6.dp)
val SmallChamferCorner = CutCornerShape(4.dp)

@Composable
fun ChamferButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = false,
    colorAccent: Color? = null
) {
    val bg = if (isPrimary) (colorAccent ?: White) else Black
    val fg = if (isPrimary) Black else (colorAccent ?: Zinc200)
    val borderCol = colorAccent ?: (if (isPrimary) White else Zinc800)

    Box(
        modifier = modifier
            .clip(SmallChamferCorner)
            .background(bg)
            .border(1.dp, borderCol, SmallChamferCorner)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 0.5.sp,
            color = fg
        )
    }
}

@Composable
fun ChamferTag(
    text: String,
    modifier: Modifier = Modifier,
    colorAccent: Color = Zinc400,
    backgroundColor: Color = Zinc950
) {
    Box(
        modifier = modifier
            .clip(CutCornerShape(3.dp))
            .background(backgroundColor)
            .border(1.dp, colorAccent.copy(alpha = 0.4f), CutCornerShape(3.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            color = colorAccent
        )
    }
}

@Composable
fun SystemPanel(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    cornerTag: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(ChamferCorner)
            .background(Zinc950)
            .border(1.dp, Zinc800, ChamferCorner)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = title.uppercase(),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = Zinc300
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = Zinc500,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            if (cornerTag != null) {
                ChamferTag(text = cornerTag, colorAccent = EmeraldAccent)
            }
        }

        // Panel Body
        content()
    }
}

@Composable
fun JourneyArc(
    currentDay: Int,
    targetDays: Int = 66,
    streak: Int,
    modifier: Modifier = Modifier
) {
    val progress = (currentDay.toFloat() / targetDays.toFloat()).coerceIn(0f, 1f)
    val percentage = (progress * 100).toInt()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Zinc900.copy(alpha = 0.6f))
            .border(1.dp, Zinc800, RoundedCornerShape(6.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "66-DAY AUTOMATICITY ARC",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = Zinc400
            )
            Text(
                text = "STREAK: ${streak}D",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = EmeraldAccent
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Progress Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Zinc800)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = progress)
                    .fillMaxHeight()
                    .background(White)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Day $currentDay / $targetDays",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = Zinc300
            )
            Text(
                text = "$percentage% Automaticity",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = Zinc500
            )
        }
    }
}
