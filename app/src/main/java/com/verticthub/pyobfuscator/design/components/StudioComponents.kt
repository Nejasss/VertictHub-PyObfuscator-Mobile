package com.verticthub.pyobfuscator.design.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.verticthub.pyobfuscator.design.theme.LocalStudioPalette
import com.verticthub.pyobfuscator.design.tokens.CodeTextStyle

enum class BadgeTone { PRIMARY, SUCCESS, WARNING, ERROR, NEUTRAL }

data class NavDestinationItem(val route: String, val label: String, val icon: ImageVector)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VHTopBar(
    title: String,
    subtitle: String? = null,
    engineReady: Boolean = true,
    showBack: Boolean = false,
    onBackClick: () -> Unit = {},
    onSettingsClick: (() -> Unit)? = null,
) {
    val palette = LocalStudioPalette.current
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(containerColor = palette.surface, titleContentColor = palette.textPrimary),
        navigationIcon = {
            if (showBack) {
                IconButton(onClick = onBackClick, modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = palette.textPrimary)
                }
            } else {
                Box(
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(palette.surfaceElevated)
                        .border(1.dp, palette.divider, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Terminal, contentDescription = "Logo", tint = palette.primary, modifier = Modifier.size(18.dp))
                }
            }
        },
        title = {
            Column(modifier = Modifier.padding(start = if (showBack) 0.dp else 8.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = palette.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.labelMedium, color = palette.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        },
        actions = {
            VHStatusBadge(label = if (engineReady) "CORE 34 OK" else "INIT", tone = if (engineReady) BadgeTone.SUCCESS else BadgeTone.WARNING)
            if (onSettingsClick != null) {
                IconButton(onClick = onSettingsClick, modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)) {
                    Icon(Icons.Default.Settings, contentDescription = "Pengaturan", tint = palette.textPrimary)
                }
            }
        },
    )
}

@Composable
fun VHBottomNavigation(items: List<NavDestinationItem>, currentRoute: String?, onSelect: (String) -> Unit) {
    val palette = LocalStudioPalette.current
    NavigationBar(containerColor = palette.surface, contentColor = palette.textPrimary, tonalElevation = 0.dp) {
        items.forEach { item ->
            val selected = currentRoute == item.route
            NavigationBarItem(
                selected = selected,
                onClick = { onSelect(item.route) },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = palette.primary, selectedTextColor = palette.primary,
                    indicatorColor = palette.surfaceElevated, unselectedIconColor = palette.textSecondary, unselectedTextColor = palette.textSecondary,
                ),
            )
        }
    }
}

@Composable
fun VHStatusBadge(label: String, tone: BadgeTone = BadgeTone.NEUTRAL, modifier: Modifier = Modifier) {
    val palette = LocalStudioPalette.current
    val fg = when (tone) {
        BadgeTone.PRIMARY -> palette.primary
        BadgeTone.SUCCESS -> palette.success
        BadgeTone.WARNING -> palette.warning
        BadgeTone.ERROR -> palette.error
        BadgeTone.NEUTRAL -> palette.textSecondary
    }
    Surface(modifier = modifier, shape = RoundedCornerShape(6.dp), color = fg.copy(alpha = 0.14f), border = BorderStroke(1.dp, fg.copy(alpha = 0.35f))) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
    }
}

@Composable
fun VHSectionHeader(title: String, subtitle: String? = null, trailing: (@Composable () -> Unit)? = null, modifier: Modifier = Modifier) {
    val palette = LocalStudioPalette.current
    Row(modifier = modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = palette.textPrimary)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
        }
        if (trailing != null) { Spacer(Modifier.width(8.dp)); trailing() }
    }
}

@Composable
fun VHSurfaceCard(modifier: Modifier = Modifier, elevated: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    val palette = LocalStudioPalette.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = if (elevated) palette.surfaceElevated else palette.surface,
        border = BorderStroke(1.dp, palette.divider),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

@Composable
fun VHPrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, icon: ImageVector? = null) {
    val palette = LocalStudioPalette.current
    Button(
        onClick = onClick, enabled = enabled, modifier = modifier.heightIn(min = 48.dp), shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = palette.primary, contentColor = palette.onPrimary, disabledContainerColor = palette.surfaceSoft, disabledContentColor = palette.textSecondary),
    ) {
        if (icon != null) { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)) }
        Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun VHSecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, icon: ImageVector? = null) {
    val palette = LocalStudioPalette.current
    OutlinedButton(
        onClick = onClick, enabled = enabled, modifier = modifier.heightIn(min = 48.dp), shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, palette.divider),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = palette.surfaceElevated, contentColor = palette.textPrimary),
    ) {
        if (icon != null) { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)) }
        Text(text, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
fun VHFileCard(filename: String, metadata: String, statusLabel: String, statusTone: BadgeTone = BadgeTone.SUCCESS, onClick: (() -> Unit)? = null) {
    val palette = LocalStudioPalette.current
    Surface(
        modifier = Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(8.dp), color = palette.surfaceElevated, border = BorderStroke(1.dp, palette.divider),
    ) {
        Row(modifier = Modifier.padding(12.dp).heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Text(filename, style = MaterialTheme.typography.titleSmall, color = palette.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(metadata, style = MaterialTheme.typography.bodySmall, color = palette.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(8.dp))
            VHStatusBadge(label = statusLabel, tone = statusTone)
        }
    }
}

@Composable
fun VHFeatureRow(
    title: String, description: String, defaultBadge: String, compatibilityBadge: String,
    checked: Boolean, onCheckedChange: (Boolean) -> Unit, moduleReference: String? = null, enabled: Boolean = true,
) {
    val palette = LocalStudioPalette.current
    var expanded by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(palette.surfaceElevated)
            .border(1.dp, palette.divider, RoundedCornerShape(8.dp)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f).clickable { expanded = !expanded }) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = palette.textPrimary)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    VHStatusBadge(defaultBadge, BadgeTone.NEUTRAL)
                    VHStatusBadge(compatibilityBadge, if (compatibilityBadge.contains("ON-DEVICE")) BadgeTone.SUCCESS else BadgeTone.PRIMARY)
                }
            }
            IconButton(onClick = { expanded = !expanded }, modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)) {
                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = "Detail", tint = palette.textSecondary)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
        }
        AnimatedVisibility(visible = expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(description, style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                if (moduleReference != null) Text("Core module: $moduleReference", style = MaterialTheme.typography.labelSmall, color = palette.primary)
            }
        }
    }
}

@Composable
fun VHSettingRow(title: String, subtitle: String, trailing: @Composable () -> Unit, onClick: (() -> Unit)? = null) {
    val palette = LocalStudioPalette.current
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = palette.textPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
        }
        Spacer(Modifier.width(12.dp))
        trailing()
    }
}

@Composable
fun VHProgressIndicator(stageLabel: String, activeModule: String, progressFraction: Float) {
    val palette = LocalStudioPalette.current
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(stageLabel, style = MaterialTheme.typography.titleSmall, color = palette.textPrimary)
            VHStatusBadge(activeModule, BadgeTone.PRIMARY)
        }
        LinearProgressIndicator(
            progress = { progressFraction.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
            color = palette.primary, trackColor = palette.surfaceSoft,
        )
        Text("${(progressFraction * 100).toInt()}% (tahap mesin aktual)", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
    }
}

@Composable
fun VHLogViewer(lines: List<String>, modifier: Modifier = Modifier) {
    val palette = LocalStudioPalette.current
    Box(
        modifier = modifier.fillMaxWidth().heightIn(min = 140.dp, max = 260.dp).clip(RoundedCornerShape(8.dp))
            .background(palette.codeBackground).border(1.dp, palette.divider, RoundedCornerShape(8.dp))
            .padding(10.dp).verticalScroll(rememberScrollState()).horizontalScroll(rememberScrollState()),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            if (lines.isEmpty()) {
                Text("Belum ada log eksekusi.", style = CodeTextStyle, color = palette.textSecondary)
            } else {
                lines.forEach { line ->
                    val lineColor = when {
                        line.contains("ERROR", true) || line.contains("FAIL", true) -> palette.error
                        line.contains("WARN", true) -> palette.warning
                        line.contains("Completed", true) || line.contains("valid", true) -> palette.success
                        else -> palette.textPrimary
                    }
                    Text(line, style = CodeTextStyle, color = lineColor, softWrap = false)
                }
            }
        }
    }
}

@Composable
fun VHEmptyState(title: String, description: String, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    val palette = LocalStudioPalette.current
    VHSurfaceCard(elevated = false) {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
            Text(description, style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
            if (actionLabel != null && onAction != null) VHPrimaryButton(text = actionLabel, onClick = onAction)
        }
    }
}

@Composable
fun VHErrorPanel(title: String, message: String, code: String? = null, onDismiss: (() -> Unit)? = null) {
    val palette = LocalStudioPalette.current
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), color = palette.error.copy(alpha = 0.12f), border = BorderStroke(1.dp, palette.error.copy(alpha = 0.45f))) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = palette.error)
                if (code != null) VHStatusBadge(code, BadgeTone.ERROR)
            }
            Text(message, style = MaterialTheme.typography.bodySmall, color = palette.textPrimary)
            if (onDismiss != null) VHSecondaryButton(text = "Tutup", onClick = onDismiss)
        }
    }
}

@Composable
fun VHCodePreview(source: String, searchQuery: String = "", maxLines: Int = 350, modifier: Modifier = Modifier) {
    val palette = LocalStudioPalette.current
    val allLines = remember(source) { source.lines() }
    val visibleLines = remember(allLines, maxLines) { allLines.take(maxLines) }
    Box(
        modifier = modifier.fillMaxWidth().heightIn(min = 180.dp, max = 380.dp).clip(RoundedCornerShape(8.dp))
            .background(palette.codeBackground).border(1.dp, palette.divider, RoundedCornerShape(8.dp))
            .padding(10.dp).verticalScroll(rememberScrollState()).horizontalScroll(rememberScrollState()),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            visibleLines.forEachIndexed { idx, line ->
                val matches = searchQuery.isNotBlank() && line.contains(searchQuery, ignoreCase = true)
                val rowBg = if (matches) palette.primary.copy(alpha = 0.18f) else Color.Transparent
                Row(modifier = Modifier.background(rowBg).padding(horizontal = 4.dp, vertical = 1.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text((idx + 1).toString().padStart(4, ' '), style = CodeTextStyle, color = palette.textSecondary, softWrap = false)
                    Spacer(Modifier.width(10.dp))
                    val codeColor = when {
                        line.trimStart().startsWith("#") -> palette.textSecondary
                        line.trimStart().startsWith("def ") || line.trimStart().startsWith("class ") || line.trimStart().startsWith("import ") || line.trimStart().startsWith("from ") -> palette.primary
                        else -> palette.textPrimary
                    }
                    Text(line, style = CodeTextStyle, color = codeColor, softWrap = false)
                }
            }
            if (allLines.size > maxLines) {
                Spacer(Modifier.height(6.dp))
                Text("... (${allLines.size - maxLines} baris berikutnya dipotong untuk tampilan; file penuh tetap diproses)", style = MaterialTheme.typography.labelMedium, color = palette.warning)
            }
        }
    }
}

@Composable
fun VHStatItem(label: String, value: String, modifier: Modifier = Modifier) {
    val palette = LocalStudioPalette.current
    Column(
        modifier = modifier.clip(RoundedCornerShape(8.dp)).background(palette.surfaceElevated).border(1.dp, palette.divider, RoundedCornerShape(8.dp)).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontFamily = FontFamily.Monospace, color = palette.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(label, style = MaterialTheme.typography.labelMedium, color = palette.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun VHConfirmationDialog(title: String, message: String, confirmLabel: String = "Lanjutkan", dismissLabel: String = "Batal", onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val palette = LocalStudioPalette.current
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = palette.surface, titleContentColor = palette.textPrimary, textContentColor = palette.textSecondary,
        title = { Text(title, style = MaterialTheme.typography.titleMedium) },
        text = { Text(message, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = { VHPrimaryButton(text = confirmLabel, onClick = onConfirm) },
        dismissButton = { VHSecondaryButton(text = dismissLabel, onClick = onDismiss) },
    )
}
