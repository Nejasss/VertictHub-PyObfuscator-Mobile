package com.verticthub.pyobfuscator.engine

import com.verticthub.pyobfuscator.data.model.ProcessingResult
import com.verticthub.pyobfuscator.data.model.TransformOptions
import com.verticthub.pyobfuscator.data.model.TransformationPassSummary
import org.json.JSONArray
import org.json.JSONObject

object EngineResultMapper {
    fun optionsToJson(opts: TransformOptions): String {
        val pins = opts.sslPinSha256Csv.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }
        return JSONObject().apply {
            put("preset", opts.preset)
            put("output_mode", opts.outputMode)
            put("validate_input", opts.validateInput)
            put("validate_output", opts.validateOutput)
            put("preserve_docstrings", opts.preserveDocstrings)
            put("run_leak_scan", opts.runLeakScan)
            put("output_filename", opts.outputFilename)
            put("encrypt_strings", opts.encryptStrings)
            put("min_string_len", opts.minStringLen)
            put("rename_identifiers", opts.renameIdentifiers)
            put("cjk_identifiers", opts.cjkIdentifiers)
            put("junk_intensity", opts.junkIntensity)
            put("inject_decoy_methods", opts.injectDecoyMethods)
            put("control_flow_obfuscation", opts.controlFlowObfuscation)
            put("constant_protection", opts.constantProtection)
            put("expression_obfuscation", opts.expressionObfuscation)
            put("import_obfuscation", opts.importObfuscation)
            put("attribute_obfuscation", opts.attributeObfuscation)
            put("function_transformation", opts.functionTransformation)
            put("data_flow_obfuscation", opts.dataFlowObfuscation)
            put("lock_filename", opts.lockFilename)
            put("require_launcher_gate", opts.requireLauncherGate)
            put("ssl_pin_sha256", JSONArray(pins))
            put("anti_debug", opts.antiDebug)
            put("periodic_recheck", opts.periodicRecheck)
            put("recheck_interval_min", opts.recheckIntervalMin)
            put("recheck_interval_max", opts.recheckIntervalMax)
            put("bytecode_integrity_check", opts.bytecodeIntegrityCheck)
            put("enable_antisandbox", opts.enableAntisandbox)
            put("antisandbox_threshold", opts.antisandboxThreshold)
            put("anti_vpn", opts.antiVpn)
            put("strict_network_check", opts.strictNetworkCheck)
            put("hook_detection", opts.hookDetection)
        }.toString()
    }

    fun parseProcessingResult(raw: JSONObject, originalSource: String): ProcessingResult {
        val inputObj = raw.optJSONObject("input") ?: JSONObject()
        val outputObj = raw.optJSONObject("output") ?: JSONObject()
        val coreObj = raw.optJSONObject("core") ?: JSONObject()
        val integrityObj = raw.optJSONObject("integrity") ?: JSONObject()
        val validationObj = raw.optJSONObject("validation") ?: JSONObject()
        val leakObj = integrityObj.optJSONObject("leak_scan") ?: JSONObject()

        val passList = mutableListOf<TransformationPassSummary>()
        val tArr = raw.optJSONArray("transformations") ?: JSONArray()
        for (i in 0 until tArr.length()) {
            val item = tArr.getJSONObject(i)
            val rep = item.optJSONObject("report")?.toString() ?: item.optString("note")
            passList.add(TransformationPassSummary(item.optString("key"), item.optString("label"), item.optBoolean("enabled"), item.optString("status"), rep))
        }
        val warnings = mutableListOf<String>()
        val wArr = raw.optJSONArray("warnings") ?: JSONArray()
        for (i in 0 until wArr.length()) warnings.add(wArr.getString(i))

        val logs = mutableListOf<String>()
        val lArr = raw.optJSONArray("logs") ?: JSONArray()
        for (i in 0 until lArr.length()) logs.add(lArr.getString(i))

        val cleanReport = JSONObject(raw.toString()).apply { optJSONObject("output")?.remove("source") }.toString(2)

        return ProcessingResult(
            success = raw.optBoolean("success", false),
            status = raw.optString("status", "unknown"),
            executionEnvironment = raw.optString("execution_environment", "ON_DEVICE"),
            inputFilename = inputObj.optString("filename"),
            inputSizeBytes = inputObj.optInt("size"),
            inputSha256 = inputObj.optString("sha256"),
            outputFilename = outputObj.optString("filename"),
            outputSizeBytes = outputObj.optInt("size"),
            outputSha256 = outputObj.optString("sha256"),
            originalSource = originalSource,
            transformedSource = outputObj.optString("source"),
            modulesLoaded = coreObj.optInt("modules_loaded"),
            modulesVerified = coreObj.optInt("modules_verified"),
            transformations = passList,
            leakTotalCount = leakObj.optInt("total_leaks", 0),
            leakFormattedReport = leakObj.optString("formatted_report", "Leak scan selesai."),
            validationSummary = "Input AST: ${validationObj.optBoolean("input_valid")} (${validationObj.optInt("input_nodes")} node) | Output AST: ${validationObj.optBoolean("output_valid")} (${validationObj.optInt("output_nodes")} node)",
            manifestSummary = "Launcher gate: ${integrityObj.optBoolean("launcher_gate_enabled")} | Filename lock: ${integrityObj.optBoolean("filename_locked")} | SHA-256: ${outputObj.optString("sha256").take(16)}...",
            runtimeCompatibilitySummary = "Chaquopy ARM64 Python 3.13 | Environment: ${raw.optString("execution_environment", "ON_DEVICE")}",
            warnings = warnings,
            logs = logs,
            rawJsonReport = cleanReport,
        )
    }
}
