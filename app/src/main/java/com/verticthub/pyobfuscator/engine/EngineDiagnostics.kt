package com.verticthub.pyobfuscator.engine

import org.json.JSONObject

data class DiagnosticFinding(val module: String, val check: String, val triggered: Boolean, val detail: String)
data class DiagnosticReport(val mode: String, val platform: String, val pythonVersion: String, val findings: List<DiagnosticFinding>)

class EngineDiagnostics(private val bridge: PythonEngineBridge) {
    suspend fun runTransparentProbe(antidebug: Boolean, antisandbox: Boolean, hookProbe: Boolean, sandboxThreshold: Int): Result<DiagnosticReport> = runCatching {
        val req = JSONObject().apply {
            put("enable_antidebug_probe", antidebug); put("enable_antisandbox_probe", antisandbox)
            put("enable_hook_probe", hookProbe); put("antisandbox_threshold", sandboxThreshold)
        }.toString()
        val raw = bridge.runTransparentDiagnostics(req)
        val list = mutableListOf<DiagnosticFinding>()
        raw.optJSONArray("findings")?.let { arr ->
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(DiagnosticFinding(obj.optString("module"), obj.optString("check"), obj.optBoolean("triggered"), obj.optString("detail")))
            }
        }
        DiagnosticReport(raw.optString("mode"), raw.optString("platform"), raw.optString("python_version"), list)
    }

    suspend fun calculateSpkiPin(certPemOrDerB64: String): Result<String> = runCatching {
        val raw = bridge.computeSslPin(certPemOrDerB64)
        if (!raw.optBoolean("success", false)) throw IllegalArgumentException(raw.optString("error", "Sertifikat DER/PEM tidak valid."))
        raw.getString("pin_sha256")
    }
}
