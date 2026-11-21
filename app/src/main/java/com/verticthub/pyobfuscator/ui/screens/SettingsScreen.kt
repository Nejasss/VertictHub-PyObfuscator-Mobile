package com.verticthub.pyobfuscator.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.verticthub.pyobfuscator.design.components.*
import com.verticthub.pyobfuscator.design.theme.LocalStudioPalette
import com.verticthub.pyobfuscator.design.theme.ThemeMode
import com.verticthub.pyobfuscator.ui.StudioUiState
import com.verticthub.pyobfuscator.ui.StudioViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(state: StudioUiState, viewModel: StudioViewModel) {
    val palette = LocalStudioPalette.current
    val settings = state.settings
    var tokenInput by remember { mutableStateOf("") }
    var pemInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        VHSurfaceCard {
            VHSectionHeader(title = "Tampilan & Tema Studio")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.entries.forEach { mode ->
                    FilterChip(selected = settings.themeMode == mode, onClick = { viewModel.setThemeMode(mode) }, label = { Text(mode.name) })
                }
            }
        }

        VHSurfaceCard {
            VHSectionHeader(title = "Konfigurasi Build Backend (GitHub Actions)", subtitle = "Digunakan saat menjalankan build binary/native (PyInstaller, Cython, C, Rust)")
            OutlinedTextField(value = settings.githubOwnerRepo, onValueChange = { v -> viewModel.updateSettings { it.copy(githubOwnerRepo = v) } }, label = { Text("Repository (owner/repo)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = settings.githubBranch, onValueChange = { v -> viewModel.updateSettings { it.copy(githubBranch = v) } }, label = { Text("Branch / Ref (default: main)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = tokenInput, onValueChange = { tokenInput = it }, label = { Text(if (settings.githubTokenConfigured) "GitHub PAT (Tersimpan terenkripsi - isi untuk ganti)" else "GitHub Personal Access Token (workflow scope)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            VHPrimaryButton(text = "Simpan Token GitHub", onClick = { viewModel.saveGitHubToken(tokenInput); tokenInput = "" }, modifier = Modifier.fillMaxWidth())
        }

        VHSurfaceCard {
            VHSectionHeader(title = "Kunci Penandatanganan Manifest (Ed25519)", subtitle = "Disimpan di Android EncryptedSharedPreferences")
            OutlinedTextField(value = pemInput, onValueChange = { pemInput = it }, label = { Text(if (settings.signingKeyConfigured) "Private Key PEM (Tersimpan)" else "Tempel Ed25519 Private Key PEM") }, modifier = Modifier.fillMaxWidth())
            VHSecondaryButton(text = "Simpan Kunci Signing", onClick = { viewModel.saveSigningPrivateKey(pemInput); pemInput = "" }, modifier = Modifier.fillMaxWidth())
        }

        VHSurfaceCard {
            VHSectionHeader(title = "Inventaris 34 Modul Inti (vh.core)", subtitle = "Status pemuatan dan verifikasi SHA-256 pada perangkat")
            state.coreModules.forEach { mod ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(mod.filename, style = MaterialTheme.typography.bodySmall, color = palette.textPrimary)
                        Text("${mod.executionEnvironment} • ${mod.dependencyStatus}", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                    }
                    VHStatusBadge(label = mod.status, tone = if (mod.importOk) BadgeTone.SUCCESS else BadgeTone.WARNING)
                }
            }
        }

        VHSurfaceCard {
            VHSectionHeader(title = "Penyimpanan & Cache")
            VHSecondaryButton(text = "Bersihkan Cache Ekspor Sementara", onClick = { viewModel.clearTempCache() }, modifier = Modifier.fillMaxWidth())
        }
        Spacer(Modifier.height(24.dp))
    }
}
