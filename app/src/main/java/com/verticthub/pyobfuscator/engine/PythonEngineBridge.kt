package com.verticthub.pyobfuscator.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class PythonEngineBridge(private val runtimeManager: PythonRuntimeManager) {
    suspend fun getEngineStatus(): JSONObject = withContext(Dispatchers.IO) {
        JSONObject(runtimeManager.getAdapterModule().callAttr("get_engine_status_json").toString())
    }
    suspend fun getCoreInventory(): JSONObject = withContext(Dispatchers.IO) {
        JSONObject(runtimeManager.getAdapterModule().callAttr("get_core_inventory_json").toString())
    }
    suspend fun analyzeProject(source: String, filename: String): JSONObject = withContext(Dispatchers.IO) {
        JSONObject(runtimeManager.getAdapterModule().callAttr("analyze_project_json", source, filename).toString())
    }
    suspend fun processProject(source: String, optionsJson: String, filename: String): JSONObject = withContext(Dispatchers.IO) {
        JSONObject(runtimeManager.getAdapterModule().callAttr("process_project_json", source, optionsJson, filename).toString())
    }
    suspend fun buildProject(projectJson: String, optionsJson: String): JSONObject = withContext(Dispatchers.IO) {
        JSONObject(runtimeManager.getAdapterModule().callAttr("build_project_json", projectJson, optionsJson).toString())
    }
    suspend fun runTransparentDiagnostics(optionsJson: String): JSONObject = withContext(Dispatchers.IO) {
        JSONObject(runtimeManager.getAdapterModule().callAttr("run_transparent_diagnostics_json", optionsJson).toString())
    }
    suspend fun computeSslPin(certPemOrDerB64: String): JSONObject = withContext(Dispatchers.IO) {
        JSONObject(runtimeManager.getAdapterModule().callAttr("compute_ssl_pin_json", certPemOrDerB64).toString())
    }
}
