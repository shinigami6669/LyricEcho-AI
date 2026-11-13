package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CacheEmerald
import com.example.ui.theme.CacheEmeraldDark
import com.example.ui.theme.LiveCyan
import com.example.ui.theme.LiveCyanDark

@Composable
fun CacheBadge(
    isFromCache: Boolean,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isFromCache) CacheEmeraldDark.copy(alpha = 0.5f) else LiveCyanDark.copy(alpha = 0.5f)
    val borderColor = if (isFromCache) CacheEmerald.copy(alpha = 0.8f) else LiveCyan.copy(alpha = 0.8f)
    val textColor = if (isFromCache) CacheEmerald else Color(0xFF38BDF8)
    val text = if (isFromCache) "Offline Cache" else "AI Live"
    val icon = if (isFromCache) Icons.Default.OfflinePin else Icons.Default.AutoAwesome

    Row(
        modifier = modifier
            .testTag(if (isFromCache) "badge_offline_cache" else "badge_ai_live")
            .background(bgColor, RoundedCornerShape(12.dp))
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = textColor,
            modifier = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}
