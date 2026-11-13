package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.ApiEngineSettings
import com.example.data.remote.ApiProfile
import com.example.data.remote.ApiProviderType
import com.example.data.remote.EndpointPingResult
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.SunsetRose
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun ApiHubCard(
    settings: ApiEngineSettings,
    totalRequests: Int,
    successRequests: Int,
    failedRequests: Int,
    isDetectingModels: Boolean,
    detectModelsResult: String?,
    pingResult: EndpointPingResult?,
    isPinging: Boolean,
    isTestingApi: Boolean,
    apiTestResult: String?,
    rawTerminalOutput: String,
    savedProfiles: List<ApiProfile> = emptyList(),
    activeProfileId: String = "",
    onProviderTypeChange: (ApiProviderType) -> Unit,
    onBaseUrlChange: (String) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
    onMaxRetriesChange: (Int) -> Unit,
    onTimeoutSecondsChange: (Int) -> Unit,
    onRpmLimitChange: (Int) -> Unit,
    onAutoFallbackChange: (Boolean) -> Unit,
    onBackupKeyChange: (String) -> Unit,
    onDetectModels: () -> Unit,
    onPingEndpoint: () -> Unit,
    onTestApi: (String) -> Unit,
    onCancelTestApi: () -> Unit,
    onClearTerminal: () -> Unit,
    onResetStats: () -> Unit,
    onSaveProfile: (String) -> Unit = {},
    onDeleteProfile: (String) -> Unit = {},
    onActivateProfile: (ApiProfile) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isKeyVisible by remember { mutableStateOf(false) }
    var isBackupKeyVisible by remember { mutableStateOf(false) }
    var showAdvancedSettings by remember { mutableStateOf(false) }
    var isTerminalExpanded by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var profileNameInput by remember { mutableStateOf("") }

    var urlInput by remember(settings.baseUrl) { mutableStateOf(settings.baseUrl) }
    var keyInput by remember(settings.apiKey) { mutableStateOf(settings.apiKey) }
    var modelInput by remember(settings.selectedModel) { mutableStateOf(settings.selectedModel) }
    var backupKeyInput by remember(settings.backupGeminiKey) { mutableStateOf(settings.backupGeminiKey) }

    if (showSaveDialog) {
        BasicAlertDialog(onDismissRequest = { showSaveDialog = false }) {
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .border(1.dp, ObsidianBorder, RoundedCornerShape(18.dp)),
                color = ObsidianSurface
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "ذخیره تنظیمات فعلی به عنوان پروفایل",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "تنظیمات اندپوینت و کلید احراز هویت فعلی با یک نام دلخواه ذخیره می‌شود:",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = profileNameInput,
                        onValueChange = { profileNameInput = it },
                        placeholder = { Text("مثال: سرور پروکسی پرسرعت", color = TextMuted, fontSize = 12.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = ElectricCyan,
                            unfocusedBorderColor = ObsidianBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        androidx.compose.material3.TextButton(onClick = { showSaveDialog = false }) {
                            Text("انصراف", color = TextMuted)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (profileNameInput.isNotBlank()) {
                                    onSaveProfile(profileNameInput.trim())
                                    profileNameInput = ""
                                    showSaveDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonViolet),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("ذخیره پروفایل", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, ObsidianBorder, RoundedCornerShape(20.dp)),
        color = ObsidianSurface
    ) {
        Column(modifier = Modifier.padding(18.dp)) {

            // Header: Title & Ping Latency Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1E2536)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = NeonViolet,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "AI API & Provider Hub",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "پرووایدر، مدیریت ریکوئست‌ها و کنسول زنده",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                // Ping Latency Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF161F30))
                        .border(
                            1.dp,
                            if (pingResult?.isSuccess == true) Color(0xFF10B981) else ElectricCyan.copy(alpha = 0.4f),
                            RoundedCornerShape(10.dp)
                        )
                        .clickable(enabled = !isPinging) { onPingEndpoint() }
                        .padding(horizontal = 10.dp, vertical = 7.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isPinging) {
                            CircularProgressIndicator(
                                color = ElectricCyan,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("پینگ...", fontSize = 11.sp, color = TextPrimary)
                        } else {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = if (pingResult?.isSuccess == true) Color(0xFF34D399) else ElectricCyan,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            val pingText = if (pingResult != null && pingResult.isSuccess) {
                                "${pingResult.latencyMs}ms"
                            } else "تست پینگ"
                            Text(
                                text = pingText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (pingResult?.isSuccess == true) Color(0xFF34D399) else ElectricCyan
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 0. Saved API Profiles Manager (مدیریت و ذخیره پروفایل‌های API)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = null,
                        tint = ElectricCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "پروفایل‌های ذخیره‌شده API",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Button to save current settings as new profile
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(NeonViolet.copy(alpha = 0.2f))
                        .border(1.dp, NeonViolet.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .clickable { showSaveDialog = true }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = NeonViolet,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "ذخیره تنظیمات فعلی",
                        color = NeonViolet,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Horizontal list of saved profiles
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                savedProfiles.forEach { profile ->
                    val isActive = profile.id == activeProfileId ||
                            (profile.baseUrl.trimEnd('/') == settings.baseUrl.trimEnd('/') && profile.apiKey == settings.apiKey)

                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                width = if (isActive) 1.5.dp else 1.dp,
                                color = if (isActive) ElectricCyan else ObsidianBorder,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onActivateProfile(profile) },
                        color = if (isActive) Color(0xFF0F2236) else Color(0xFF131826)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isActive) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(ElectricCyan)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Column {
                                Text(
                                    text = profile.name,
                                    color = if (isActive) Color.White else TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
                                )
                                Text(
                                    text = profile.selectedModel,
                                    color = if (isActive) ElectricCyan else TextMuted,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            if (!profile.isSystemPreset) {
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = { onDeleteProfile(profile.id) },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "حذف پروفایل",
                                        tint = SunsetRose.copy(alpha = 0.8f),
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 1. Provider Presets Row
            Text(
                text = "پرووایدر هوش مصنوعی (AI Provider)",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            val scrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ApiProviderType.entries.forEach { type ->
                    val isSelected = type == settings.providerType
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) NeonViolet.copy(alpha = 0.25f) else Color(0xFF141925)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) NeonViolet else ObsidianBorder,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onProviderTypeChange(type) }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = NeonViolet,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = type.displayName,
                                color = if (isSelected) Color.White else TextMuted,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Base URL Input
            Text(
                text = "آدرس پایه سرور (Base URL)",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = urlInput,
                onValueChange = {
                    urlInput = it
                    onBaseUrlChange(it)
                },
                placeholder = { Text("https://api.openai.com/v1", color = TextMuted, fontSize = 12.sp) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = NeonViolet,
                    unfocusedBorderColor = ObsidianBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_api_base_url_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 3. API Key Input
            Text(
                text = "کلید احراز هویت (API Key)",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = keyInput,
                onValueChange = {
                    keyInput = it
                    onApiKeyChange(it)
                },
                placeholder = { Text("sk-...", color = TextMuted, fontSize = 12.sp) },
                visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { isKeyVisible = !isKeyVisible }) {
                        Icon(
                            imageVector = if (isKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle visibility",
                            tint = TextMuted
                        )
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = NeonViolet,
                    unfocusedBorderColor = ObsidianBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_api_key_input")
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Auto-Discovery of Available Models (Polished card - no squished button)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, ElectricCyan.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
                color = Color(0xFF0F172A)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Header row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = ElectricCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "شناسایی خودکار مدل‌های API",
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "واکشی زنده لیست از /v1/models",
                                    color = TextMuted,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        // Badge showing count
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1E293B))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${settings.discoveredModels.size} مدل",
                                color = ElectricCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Prominent discovery button (never squished)
                    Button(
                        onClick = { onDetectModels() },
                        enabled = !isDetectingModels,
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .testTag("btn_detect_models")
                    ) {
                        if (isDetectingModels) {
                            CircularProgressIndicator(
                                color = Color.Black,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("در حال استخراج لیست مدل‌ها از سرور...", fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("شناسایی خودکار مدل‌ها", fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (detectModelsResult != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = detectModelsResult,
                            color = if (detectModelsResult.contains("موفق")) Color(0xFF34D399) else SunsetRose,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "مدل‌های شناسایی‌شده روی سرور (انتخاب با ۱ لمس):",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    val modelsScroll = rememberScrollState()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(modelsScroll),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        settings.discoveredModels.forEach { modelName ->
                            val isChosen = modelName.equals(settings.selectedModel, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isChosen) ElectricCyan.copy(alpha = 0.25f) else Color(0xFF1E293B))
                                    .border(
                                        1.dp,
                                        if (isChosen) ElectricCyan else ObsidianBorder,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        modelInput = modelName
                                        onModelChange(modelName)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isChosen) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = ElectricCyan,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = modelName,
                                        color = if (isChosen) ElectricCyan else TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Manual Model Field
                    OutlinedTextField(
                        value = modelInput,
                        onValueChange = {
                            modelInput = it
                            onModelChange(it)
                        },
                        label = { Text("شناسه مدل فعال (Active Model ID)", fontSize = 11.sp) },
                        placeholder = { Text("gemini-2.5-flash", fontSize = 12.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = ElectricCyan,
                            unfocusedBorderColor = ObsidianBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 5. Request Rate & Retries Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF151B28))
                    .clickable { showAdvancedSettings = !showAdvancedSettings }
                    .padding(horizontal = 12.dp, vertical = 11.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = NeonViolet,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "تنظیم تعداد ریکوئست و پایداری (Rate Limits & Retries)",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Icon(
                    imageVector = if (showAdvancedSettings) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = TextMuted
                )
            }

            AnimatedVisibility(visible = showAdvancedSettings) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    // Max Retries
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("تعداد تلاش مجدد (Max Retries)", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Text("در صورت بروز خطای موقت سرور یا 429", color = TextMuted, fontSize = 10.sp)
                        }

                        val retriesOptions = listOf(1, 2, 3, 5)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            retriesOptions.forEach { r ->
                                val isSelected = r == settings.maxRetries
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) NeonViolet else Color(0xFF1E2536))
                                        .clickable { onMaxRetriesChange(r) }
                                        .padding(horizontal = 9.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "${r}x",
                                        color = if (isSelected) Color.White else TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Timeout Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("مهلت زمانی پاسخ (Timeout)", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Text("مدت انتظار برای پاسخ مدل قبل از خطا", color = TextMuted, fontSize = 10.sp)
                        }

                        val timeouts = listOf(15, 30, 45, 60)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            timeouts.forEach { t ->
                                val isSelected = t == settings.timeoutSeconds
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) ElectricCyan else Color(0xFF1E2536))
                                        .clickable { onTimeoutSecondsChange(t) }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "${t}s",
                                        color = if (isSelected) Color.Black else TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Rate Limit (RPM)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("محدودیت تعداد در دقیقه (RPM)", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Text("برای جلوگیری از بلاک یا خطای 429", color = TextMuted, fontSize = 10.sp)
                        }

                        val rpms = listOf(10, 30, 60, 0)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            rpms.forEach { rpm ->
                                val isSelected = rpm == settings.requestsPerMinute
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) Color(0xFF10B981) else Color(0xFF1E2536))
                                        .clickable { onRpmLimitChange(rpm) }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (rpm == 0) "نامحدود" else "${rpm} RPM",
                                        color = if (isSelected) Color.White else TextSecondary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Auto Fallback Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("سوئیچ خودکار به سرور پشتیبان Gemini", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text("در صورت از کار افتادن سرور سفارشی، پخش لیریک متوقف نمی‌شود", color = TextMuted, fontSize = 10.sp)
                        }
                        Switch(
                            checked = settings.autoFallbackToGemini,
                            onCheckedChange = { onAutoFallbackChange(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = NeonViolet,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = Color(0xFF1E2536)
                            )
                        )
                    }

                    if (settings.autoFallbackToGemini) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = backupKeyInput,
                            onValueChange = {
                                backupKeyInput = it
                                onBackupKeyChange(it)
                            },
                            label = { Text("کلید سرور پشتیبان Gemini", fontSize = 11.sp) },
                            visualTransformation = if (isBackupKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { isBackupKeyVisible = !isBackupKeyVisible }) {
                                    Icon(
                                        imageVector = if (isBackupKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null,
                                        tint = TextMuted
                                    )
                                }
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = NeonViolet,
                                unfocusedBorderColor = ObsidianBorder
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 6. Request Telemetry & Stats Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF111827))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Storage, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "کل: $totalRequests  |  موفق: $successRequests  |  ناموفق: $failedRequests",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
                Text(
                    text = "ریست آمار",
                    color = ElectricCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onResetStats() }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 7. Dynamic Test / Cancel Button
            if (isTestingApi) {
                // When request is in progress: Show Cancel Button!
                Button(
                    onClick = { onCancelTestApi() },
                    colors = ButtonDefaults.buttonColors(containerColor = SunsetRose),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("settings_btn_cancel_api")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "لغو اتصال و توقف درخواست",
                        fontSize = 13.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(14.dp)
                    )
                }
            } else {
                // When idle: Show Test API Button
                Button(
                    onClick = { onTestApi(keyInput.trim()) },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonViolet),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("settings_btn_test_api")
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "تست کامل اتصال و تولید لیریک با هوش مصنوعی",
                        fontSize = 12.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (apiTestResult != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (apiTestResult.contains("موفق") || apiTestResult.contains("200"))
                                Color(0xFF064E3B) else Color(0xFF451A1A)
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = apiTestResult,
                        color = if (apiTestResult.contains("موفق") || apiTestResult.contains("200"))
                            Color(0xFF34D399) else SunsetRose,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 8. Embedded Mini Terminal / Console (ترمینال فسقلی خروجی API)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(14.dp)),
                color = Color(0xFF080C14)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Terminal Top Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1E293B)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Terminal,
                                    contentDescription = null,
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "کنسول زنده خروجی API (Mini Terminal)",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Copy button
                            IconButton(
                                onClick = {
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    cm.setPrimaryClip(ClipData.newPlainText("API Terminal Output", rawTerminalOutput))
                                    Toast.makeText(context, "خروجی ترمینال در کلیپ‌بورد کپی شد", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy Terminal",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(15.dp)
                                )
                            }

                            // Clear button
                            IconButton(
                                onClick = { onClearTerminal() },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ClearAll,
                                    contentDescription = "Clear Terminal",
                                    tint = TextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // Expand/Collapse toggle
                            IconButton(
                                onClick = { isTerminalExpanded = !isTerminalExpanded },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = if (isTerminalExpanded) Icons.Default.ExpandLess else Icons.Default.OpenInFull,
                                    contentDescription = "Toggle Expand",
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Terminal Output Window
                    val terminalVerticalScroll = rememberScrollState()
                    val terminalHorizontalScroll = rememberScrollState()

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (isTerminalExpanded) 340.dp else 160.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF030712))
                            .border(1.dp, Color(0xFF1F2937), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                            .animateContentSize()
                            .verticalScroll(terminalVerticalScroll)
                            .horizontalScroll(terminalHorizontalScroll)
                    ) {
                        Text(
                            text = rawTerminalOutput,
                            color = Color(0xFF38BDF8),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }
    }
}
