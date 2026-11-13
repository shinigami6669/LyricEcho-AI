package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.diagnostics.AppLogger
import com.example.diagnostics.LogEntry
import com.example.diagnostics.LogLevel
import com.example.diagnostics.LogTag
import com.example.service.MusicNotificationListener
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.SunsetRose
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(
    onSimulateSpotify: (track: String, artist: String) -> Unit,
    onTestNotification: (track: String, artist: String) -> Unit,
    onTestGeminiApi: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allLogs by AppLogger.logs.collectAsStateWithLifecycle()

    var selectedTabIndex by remember { mutableIntStateOf(0) } // 0: Live Logs, 1: Health & Tests
    var searchQuery by remember { mutableStateOf("") }
    var selectedTagFilter by remember { mutableStateOf<LogTag?>(null) }
    var showOnlyErrors by remember { mutableStateOf(false) }
    var autoScroll by remember { mutableStateOf(true) }
    var expandedLogId by remember { mutableStateOf<Long?>(null) }
    var showExportDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    // Permission check states
    val isListenerConnected by MusicNotificationListener.isListenerConnected.collectAsStateWithLifecycle()
    var isOverlayGranted by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var isNotificationListenerGranted by remember {
        mutableStateOf(MusicNotificationListener.isNotificationListenerGranted(context))
    }

    // Refresh permissions on resume / interaction
    LaunchedEffect(selectedTabIndex) {
        isOverlayGranted = Settings.canDrawOverlays(context)
        isNotificationListenerGranted = MusicNotificationListener.isNotificationListenerGranted(context)
    }

    BackHandler { onBack() }

    // Export Dialog (for copying to AI assistant / chat)
    if (showExportDialog) {
        val fullReport = remember(allLogs) { AppLogger.copyToClipboard(context) }
        BasicAlertDialog(onDismissRequest = { showExportDialog = false }) {
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, ObsidianBorder, RoundedCornerShape(20.dp)),
                color = ObsidianSurface
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "گزارش کامل عیب‌یابی (برای چت)",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { showExportDialog = false }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "بستن", tint = TextMuted)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "متن زیر به کلیپ‌بورد کپی شد. می‌توانید آن را مستقیماً در پیام خود جای‌گذاری (Paste) کنید:",
                        color = ElectricCyan,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF080C14))
                            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = fullReport,
                            color = Color(0xFFE2E8F0),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                AppLogger.copyToClipboard(context)
                                Toast.makeText(context, "گزارش در حافظه کپی شد", Toast.LENGTH_SHORT).show()
                                showExportDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonViolet),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("کپی مجدد و خروج", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    val filteredLogs = remember(allLogs, searchQuery, selectedTagFilter, showOnlyErrors) {
        allLogs.filter { entry ->
            val matchesQuery = searchQuery.isBlank() ||
                    entry.message.contains(searchQuery, ignoreCase = true) ||
                    (entry.details?.contains(searchQuery, ignoreCase = true) == true) ||
                    entry.tag.display.contains(searchQuery, ignoreCase = true)

            val matchesTag = selectedTagFilter == null || entry.tag == selectedTagFilter
            val matchesError = !showOnlyErrors || entry.level == LogLevel.ERROR || entry.level == LogLevel.WARN

            matchesQuery && matchesTag && matchesError
        }
    }

    LaunchedEffect(filteredLogs.size, autoScroll) {
        if (autoScroll && filteredLogs.isNotEmpty()) {
            listState.animateScrollToItem(filteredLogs.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Sleek Modern App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back_diagnostics")) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "سیستم عیب‌یابی زنده",
                            color = TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        // Pulse live recording indicator
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF34D399))
                        )
                    }
                    Text(
                        text = "${allLogs.size} لاگ ثبت‌شده  •  Live Telemetry",
                        color = ElectricCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Copy for AI Chat
                Button(
                    onClick = {
                        AppLogger.copyToClipboard(context)
                        showExportDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonViolet),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("btn_export_for_ai")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("کپی برای چت", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = {
                        val text = AppLogger.copyToClipboard(context)
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "LyricEcho AI Diagnostics Logs")
                            putExtra(Intent.EXTRA_TEXT, text)
                        }
                        context.startActivity(Intent.createChooser(intent, "اشتراک‌گذاری گزارش لاگ"))
                    },
                    modifier = Modifier.testTag("btn_share_logs")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "اشتراک‌گذاری",
                        tint = NeonViolet,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = {
                        AppLogger.clear()
                        Toast.makeText(context, "تمام لاگ‌ها پاک شدند", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("btn_clear_logs")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "پاک‌سازی",
                        tint = SunsetRose,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Clean Two-Tab Navigation: [لاگ‌های زنده] vs [وضعیت سیستم و تست‌ها]
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = ObsidianSurface,
            contentColor = ElectricCyan,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    color = ElectricCyan,
                    height = 2.5.dp
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .clip(RoundedCornerShape(10.dp))
        ) {
            Tab(
                selected = selectedTabIndex == 0,
                onClick = { selectedTabIndex = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.BugReport, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "لاگ‌های زنده (${filteredLogs.size})",
                            fontSize = 12.sp,
                            fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                },
                selectedContentColor = ElectricCyan,
                unselectedContentColor = TextSecondary
            )

            Tab(
                selected = selectedTabIndex == 1,
                onClick = { selectedTabIndex = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "وضعیت سیستم و تست‌ها",
                            fontSize = 12.sp,
                            fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                },
                selectedContentColor = NeonViolet,
                unselectedContentColor = TextSecondary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (selectedTabIndex == 0) {
            // ==========================================
            // TAB 1: LIVE LOGS STREAM (Spacious & Clean)
            // ==========================================
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp)
            ) {
                // Sleek Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("جستجو در پیام‌ها، خطاها و داده‌های فنی...", color = TextMuted, fontSize = 11.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "پاک‌سازی", tint = TextMuted, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = ElectricCyan,
                        unfocusedBorderColor = ObsidianBorder,
                        focusedContainerColor = Color(0xFF0F1420),
                        unfocusedContainerColor = Color(0xFF0F1420)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("diag_search_input")
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Filter Chips Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = selectedTagFilter == null && !showOnlyErrors,
                        onClick = {
                            selectedTagFilter = null
                            showOnlyErrors = false
                        },
                        label = { Text("همه", fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricCyan,
                            selectedLabelColor = ObsidianBg
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    FilterChip(
                        selected = showOnlyErrors,
                        onClick = { showOnlyErrors = !showOnlyErrors },
                        label = { Text("خطاها ⚠️", fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SunsetRose,
                            selectedLabelColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    FilterChip(
                        selected = selectedTagFilter == LogTag.SPOTIFY_RECEIVER,
                        onClick = {
                            selectedTagFilter = if (selectedTagFilter == LogTag.SPOTIFY_RECEIVER) null else LogTag.SPOTIFY_RECEIVER
                        },
                        label = { Text("اسپاتیفای", fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF1DB954),
                            selectedLabelColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    FilterChip(
                        selected = selectedTagFilter == LogTag.NOTIFICATION_LISTENER,
                        onClick = {
                            selectedTagFilter = if (selectedTagFilter == LogTag.NOTIFICATION_LISTENER) null else LogTag.NOTIFICATION_LISTENER
                        },
                        label = { Text("نوتیفیکیشن", fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricCyan,
                            selectedLabelColor = ObsidianBg
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    FilterChip(
                        selected = selectedTagFilter == LogTag.GEMINI_AI,
                        onClick = {
                            selectedTagFilter = if (selectedTagFilter == LogTag.GEMINI_AI) null else LogTag.GEMINI_AI
                        },
                        label = { Text("هوش مصنوعی API", fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonViolet,
                            selectedLabelColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    FilterChip(
                        selected = selectedTagFilter == LogTag.OVERLAY_POPUP,
                        onClick = {
                            selectedTagFilter = if (selectedTagFilter == LogTag.OVERLAY_POPUP) null else LogTag.OVERLAY_POPUP
                        },
                        label = { Text("پاپ‌آپ شناور", fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFF472B6),
                            selectedLabelColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                // Controls: Auto-scroll & Counter
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "نمایش ${filteredLogs.size} از ${allLogs.size} رکورد",
                        color = TextMuted,
                        fontSize = 11.sp
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "اسکرول خودکار", color = TextSecondary, fontSize = 11.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Switch(
                            checked = autoScroll,
                            onCheckedChange = { autoScroll = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ElectricCyan,
                                checkedTrackColor = NeonViolet
                            ),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Live Logs List
                if (filteredLogs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.BugReport,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "هیچ لاگی با فیلتر فعلی یافت نشد",
                                color = TextSecondary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "یک آهنگ در اسپاتیفای پخش کنید یا به تب «وضعیت سیستم و تست‌ها» بروید.",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filteredLogs, key = { it.id }) { logItem ->
                            EnhancedLogItemCard(
                                entry = logItem,
                                isExpanded = expandedLogId == logItem.id,
                                onToggleExpand = {
                                    expandedLogId = if (expandedLogId == logItem.id) null else logItem.id
                                },
                                onCopy = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Log Detail", "${logItem.message}\n${logItem.details ?: ""}")
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "جزئیات لاگ کپی شد", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(30.dp))
                        }
                    }
                }
            }
        } else {
            // ==========================================
            // TAB 2: SYSTEM HEALTH, GUIDES & TEST TOOLS
            // ==========================================
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Critical Spotify Configuration Banner
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, Color(0xFF1DB954).copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
                    color = Color(0xFF0B1910)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1DB954).copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = null,
                                    tint = Color(0xFF1DB954),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "راهنمای فعال‌سازی هماهنگی با اسپاتیفای",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "۱. در اسپاتیفای به Settings (تنظیمات) بروید و گزینه «Device Broadcast Status» را روشن کنید.\n۲. دسترسی شنیدن نوتیفیکیشن را در زیر روشن کنید تا برنامه فوراً بدون نیاز به هیچ تنظیمی آهنگ را شناسایی و پاپ‌آپ را باز کند.",
                            color = Color(0xFFE2E8F0),
                            fontSize = 11.sp,
                            lineHeight = 17.sp
                        )
                    }
                }

                // 2. Permission Status Cards
                Text(
                    text = "وضعیت دسترسی‌های سیستمی:",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Notification Listener Card
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                1.dp,
                                if (isListenerConnected) Color(0xFF10B981) else if (isNotificationListenerGranted) ElectricCyan else SunsetRose,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                if (isNotificationListenerGranted) {
                                    MusicNotificationListener.rebindService(context)
                                    Toast.makeText(context, "سرویس شنونده اعلان‌ها ری‌کانکت شد", Toast.LENGTH_SHORT).show()
                                } else {
                                    MusicNotificationListener.openNotificationListenerSettings(context)
                                }
                            },
                        color = ObsidianSurface
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isListenerConnected) Icons.Default.CheckCircle else Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = if (isListenerConnected) Color(0xFF10B981) else if (isNotificationListenerGranted) ElectricCyan else SunsetRose,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "شنیدن نوتیفیکیشن",
                                    color = TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = when {
                                    isListenerConnected -> "متصل و فعال ✓"
                                    isNotificationListenerGranted -> "مجوز دارد (لمس برای ری‌کانکت)"
                                    else -> "غیرفعال (لمس برای فعال‌سازی)"
                                },
                                color = if (isListenerConnected) Color(0xFF34D399) else if (isNotificationListenerGranted) ElectricCyan else SunsetRose,
                                fontSize = 10.sp
                            )
                        }
                    }

                    // Overlay Permission Card
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                1.dp,
                                if (isOverlayGranted) Color(0xFF10B981) else ElectricCyan,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                try {
                                    val intent = Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                                    context.startActivity(intent)
                                }
                            },
                        color = ObsidianSurface
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isOverlayGranted) Icons.Default.CheckCircle else Icons.Default.PictureInPictureAlt,
                                    contentDescription = null,
                                    tint = if (isOverlayGranted) Color(0xFF10B981) else ElectricCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "پاپ‌آپ روی برنامه‌ها",
                                    color = TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (isOverlayGranted) "فعال (Overlay مجاز)" else "غیرفعال (لمس برای اعطا)",
                                color = if (isOverlayGranted) Color(0xFF34D399) else SunsetRose,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                // 3. System Connectivity Check Action
                Text(
                    text = "بررسی ارتباط با سرور هوش مصنوعی:",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, ObsidianBorder, RoundedCornerShape(14.dp)),
                    color = ObsidianSurface
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                onTestGeminiApi()
                                selectedTabIndex = 0
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2536)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("diag_btn_ping_gemini")
                        ) {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("بررسی اتصال به سرویس هوش مصنوعی (API Check)", fontSize = 12.sp, color = ElectricCyan, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // 4. Telemetry Statistics Summary
                val errorCount = remember(allLogs) { allLogs.count { it.level == LogLevel.ERROR } }
                val warnCount = remember(allLogs) { allLogs.count { it.level == LogLevel.WARN } }
                val successCount = remember(allLogs) { allLogs.count { it.level == LogLevel.SUCCESS } }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, ObsidianBorder, RoundedCornerShape(14.dp)),
                    color = Color(0xFF0D121D)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "آمار سلامت ارتباطات:",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "$successCount", color = Color(0xFF34D399), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Text(text = "موفق", color = TextMuted, fontSize = 10.sp)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "$warnCount", color = Color(0xFFFBBF24), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Text(text = "هشدار", color = TextMuted, fontSize = 10.sp)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "$errorCount", color = SunsetRose, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Text(text = "خطا", color = TextMuted, fontSize = 10.sp)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "${allLogs.size}", color = ElectricCyan, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Text(text = "کل رویدادها", color = TextMuted, fontSize = 10.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

@Composable
private fun EnhancedLogItemCard(
    entry: LogEntry,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onCopy: () -> Unit
) {
    val levelColor = when (entry.level) {
        LogLevel.VERBOSE -> Color(0xFF6B7280)
        LogLevel.DEBUG -> Color(0xFF38BDF8)
        LogLevel.INFO -> Color(0xFF60A5FA)
        LogLevel.SUCCESS -> Color(0xFF34D399)
        LogLevel.WARN -> Color(0xFFFBBF24)
        LogLevel.ERROR -> Color(0xFFF87171)
    }

    val tagColor = when (entry.tag) {
        LogTag.SPOTIFY_RECEIVER -> Color(0xFF1DB954)
        LogTag.NOTIFICATION_LISTENER -> ElectricCyan
        LogTag.GEMINI_AI -> NeonViolet
        LogTag.OVERLAY_POPUP -> Color(0xFFF472B6)
        LogTag.PERMISSION_CHECK -> Color(0xFFFBBF24)
        LogTag.DATABASE_ROOM -> Color(0xFF818CF8)
        LogTag.PLAYBACK_ENGINE -> Color(0xFF34D399)
        LogTag.SYSTEM -> Color(0xFF9CA3AF)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, levelColor.copy(alpha = 0.22f), RoundedCornerShape(10.dp))
            .clickable { onToggleExpand() },
        color = ObsidianSurface
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Header Row: Level + Tag + Time + Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Level badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(levelColor.copy(alpha = 0.18f))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = entry.level.label,
                            color = levelColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Tag badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(tagColor.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = entry.tag.display,
                            color = tagColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Time badge and action icons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = entry.timeFormatted,
                        color = TextMuted,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = onCopy,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "کپی لاگ",
                            tint = TextMuted,
                            modifier = Modifier.size(13.dp)
                        )
                    }

                    if (!entry.details.isNullOrBlank()) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Main Message
            Text(
                text = entry.message,
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 16.sp
            )

            // Expandable details container
            if (!entry.details.isNullOrBlank()) {
                AnimatedVisibility(visible = isExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF080C14))
                            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "DETAILED PAYLOAD:",
                                color = ElectricCyan,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "کلیک برای کپی",
                                color = TextMuted,
                                fontSize = 9.sp,
                                modifier = Modifier.clickable { onCopy() }
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = entry.details,
                            color = Color(0xFFCBD5E1),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }
    }
}
