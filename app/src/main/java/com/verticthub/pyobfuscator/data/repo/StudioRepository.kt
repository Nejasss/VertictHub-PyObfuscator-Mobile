package com.verticthub.pyobfuscator.data.repo

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.verticthub.pyobfuscator.data.model.AppSettings
import com.verticthub.pyobfuscator.data.model.HistoryRecord
import com.verticthub.pyobfuscator.design.theme.ThemeMode
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.nio.charset.StandardCharsets

class StudioRepository(private val context: Context) {
    private val plainPrefs: SharedPreferences = context.getSharedPreferences("vh_studio_prefs", Context.MODE_PRIVATE)
    private val securePrefs: SharedPreferences = createSecurePrefs(context)

    private fun createSecurePrefs(ctx: Context): SharedPreferences = runCatching {
        val masterKey = MasterKey.Builder(ctx).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        EncryptedSharedPreferences.create(ctx, "vh_studio_secure_prefs", masterKey, EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV, EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)
    }.getOrElse { ctx.getSharedPreferences("vh_studio_fallback_secure", Context.MODE_PRIVATE) }

    fun loadSettings(): AppSettings {
        val themeStr = plainPrefs.getString("theme_mode", ThemeMode.DARK.name) ?: ThemeMode.DARK.name
        val theme = runCatching { ThemeMode.valueOf(themeStr) }.getOrDefault(ThemeMode.DARK)
        val tokenPresent = !securePrefs.getString("github_pat", "").isNullOrBlank()
        val keyPresent = !securePrefs.getString("signing_private_key", "").isNullOrBlank()
        return AppSettings(
            themeMode = theme,
            defaultPreset = plainPrefs.getString("default_preset", "default_cli") ?: "default_cli",
            autoCleanTempFiles = plainPrefs.getBoolean("auto_clean_temp", true),
            githubOwnerRepo = plainPrefs.getString("github_owner_repo", "") ?: "",
            githubBranch = plainPrefs.getString("github_branch", "main") ?: "main",
            githubTokenConfigured = tokenPresent,
            defaultBuildTarget = plainPrefs.getString("default_build_target", "android-arm64") ?: "android-arm64",
            signingKeyConfigured = keyPresent,
        )
    }

    fun saveSettings(settings: AppSettings) {
        plainPrefs.edit()
            .putString("theme_mode", settings.themeMode.name)
            .putString("default_preset", settings.defaultPreset)
            .putBoolean("auto_clean_temp", settings.autoCleanTempFiles)
            .putString("github_owner_repo", settings.githubOwnerRepo)
            .putString("github_branch", settings.githubBranch)
            .putString("default_build_target", settings.defaultBuildTarget)
            .apply()
    }

    fun saveGitHubToken(token: String) {
        if (token.isBlank()) securePrefs.edit().remove("github_pat").apply()
        else securePrefs.edit().putString("github_pat", token.trim()).apply()
    }

    fun getGitHubToken(): String = securePrefs.getString("github_pat", "") ?: ""

    fun saveSigningPrivateKey(pem: String) {
        if (pem.isBlank()) securePrefs.edit().remove("signing_private_key").apply()
        else securePrefs.edit().putString("signing_private_key", pem.trim()).apply()
    }

    fun getSigningPrivateKey(): String = securePrefs.getString("signing_private_key", "") ?: ""

    fun loadHistory(): List<HistoryRecord> {
        val raw = plainPrefs.getString("history_json", "[]") ?: "[]"
        val arr = runCatching { JSONArray(raw) }.getOrDefault(JSONArray())
        val out = mutableListOf<HistoryRecord>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            out.add(HistoryRecord(o.optString("id"), o.optString("projectName"), o.optString("sourceFilename"), o.optLong("timestampMillis"), o.optString("configSummary"), o.optString("status"), o.optString("buildMode"), o.optString("validationStatus"), o.optString("outputReference")))
        }
        return out
    }

    fun appendHistory(record: HistoryRecord, outputContent: String? = null) {
        val existing = loadHistory().toMutableList()
        var ref = record.outputReference
        if (outputContent != null) {
            val dir = File(context.filesDir, "artifacts").apply { mkdirs() }
            val file = File(dir, "${record.id}_${record.outputReference.replace(Regex("[^A-Za-z0-9._-]"), "_")}")
            file.writeText(outputContent, StandardCharsets.UTF_8)
            ref = file.absolutePath
        }
        existing.add(0, record.copy(outputReference = ref))
        val trimmed = existing.take(100)
        val arr = JSONArray()
        trimmed.forEach { item ->
            arr.put(JSONObject().apply {
                put("id", item.id); put("projectName", item.projectName); put("sourceFilename", item.sourceFilename)
                put("timestampMillis", item.timestampMillis); put("configSummary", item.configSummary); put("status", item.status)
                put("buildMode", item.buildMode); put("validationStatus", item.validationStatus); put("outputReference", item.outputReference)
            })
        }
        plainPrefs.edit().putString("history_json", arr.toString()).apply()
    }

    fun readArtifactContent(path: String): String? = runCatching {
        val f = File(path)
        if (f.exists() && f.isFile) f.readText(StandardCharsets.UTF_8) else null
    }.getOrNull()

    fun deleteHistoryItem(id: String) {
        val existing = loadHistory().toMutableList()
        val target = existing.find { it.id == id }
        if (target != null && target.outputReference.startsWith("/")) runCatching { File(target.outputReference).delete() }
        existing.removeAll { it.id == id }
        val arr = JSONArray()
        existing.forEach { item ->
            arr.put(JSONObject().apply {
                put("id", item.id); put("projectName", item.projectName); put("sourceFilename", item.sourceFilename)
                put("timestampMillis", item.timestampMillis); put("configSummary", item.configSummary); put("status", item.status)
                put("buildMode", item.buildMode); put("validationStatus", item.validationStatus); put("outputReference", item.outputReference)
            })
        }
        plainPrefs.edit().putString("history_json", arr.toString()).apply()
    }

    fun clearAllHistory() {
        File(context.filesDir, "artifacts").deleteRecursively()
        plainPrefs.edit().putString("history_json", "[]").apply()
    }
}
