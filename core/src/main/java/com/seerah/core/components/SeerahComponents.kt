package com.seerah.core.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.seerah.core.theme.SeerahAntiqueGold
import com.seerah.core.theme.SeerahDeepGreen
import com.seerah.core.theme.SeerahEmerald
import com.seerah.core.theme.SeerahHistoricalBrown
import com.seerah.core.theme.SeerahIvory
import com.seerah.core.theme.SeerahParchment

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeerahTopBar(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {}
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = SeerahParchment.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "رجوع",
                        tint = SeerahIvory
                    )
                }
            }
        },
        actions = { actions() },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = SeerahEmerald,
            titleContentColor = SeerahIvory,
            actionIconContentColor = SeerahIvory
        )
    )
}

/**
 * بطاقة موسوعية عاجية بحدود ذهبية رفيعة مستوحاة من الأطلس التاريخي.
 */
@Composable
fun SeerahEncyclopediaCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    parchmentTone: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val containerColor = if (parchmentTone) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        MaterialTheme.colorScheme.surface
    }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, SeerahAntiqueGold.copy(alpha = 0.55f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
fun SeerahHeroBanner(
    title: String,
    subtitle: String,
    badge: String? = null,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(SeerahDeepGreen, SeerahEmerald)
                )
            )
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!badge.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = SeerahAntiqueGold.copy(alpha = 0.22f),
                    border = BorderStroke(1.dp, SeerahAntiqueGold.copy(alpha = 0.6f))
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = SeerahParchment,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = SeerahIvory
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = SeerahParchment.copy(alpha = 0.9f)
            )
        }
    }
}

@Composable
fun CitationBadge(
    label: String,
    modifier: Modifier = Modifier,
    toneColor: Color = SeerahAntiqueGold
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = toneColor.copy(alpha = 0.14f),
        border = BorderStroke(0.75.dp, toneColor.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.MenuBook,
                contentDescription = null,
                tint = toneColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun EmptyVerifiedSourceState(
    title: String,
    message: String,
    icon: ImageVector = Icons.Outlined.Info,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    SeerahEncyclopediaCard(parchmentTone = true) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = SeerahHistoricalBrown,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!actionLabel.isNullOrBlank() && onAction != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SeerahEmerald,
                        modifier = Modifier.clickable(onClick = onAction)
                    ) {
                        Text(
                            text = actionLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = SeerahIvory,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun SeerahGoldButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(Color(0xFFC8A464), SeerahAntiqueGold, Color(0xFFE0C285))
                )
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 28.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = SeerahDeepGreen
        )
    }
}

@Composable
fun SeerahTimelineNodeRow(
    title: String,
    dateBadge: String,
    subtitle: String,
    isHighlighted: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // العمود الزمني الذهبي والعقدة الدائرية
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(34.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(2.5.dp)
                    .height(22.dp)
                    .background(SeerahAntiqueGold.copy(alpha = 0.65f))
            )
            Box(
                modifier = Modifier
                    .size(if (isHighlighted) 16.dp else 12.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (isHighlighted) SeerahEmerald else SeerahParchment)
                    .padding(2.dp)
            )
            Box(
                modifier = Modifier
                    .width(2.5.dp)
                    .height(22.dp)
                    .background(SeerahAntiqueGold.copy(alpha = 0.65f))
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isHighlighted) SeerahEmerald else MaterialTheme.colorScheme.surfaceVariant
            ),
            border = BorderStroke(1.dp, SeerahAntiqueGold.copy(alpha = 0.7f))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isHighlighted) SeerahIvory else MaterialTheme.colorScheme.onSurface
                )
                if (dateBadge.isNotBlank()) {
                    Text(
                        text = dateBadge,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isHighlighted) SeerahParchment else SeerahHistoricalBrown
                    )
                }
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isHighlighted) SeerahParchment.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
