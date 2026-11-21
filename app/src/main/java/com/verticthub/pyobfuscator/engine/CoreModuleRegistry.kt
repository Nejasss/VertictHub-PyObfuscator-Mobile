package com.verticthub.pyobfuscator.engine

import com.verticthub.pyobfuscator.data.model.CoreModuleStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CoreModuleRegistry(private val bridge: PythonEngineBridge) {
    private val _modules = MutableStateFlow<List<CoreModuleStatus>>(emptyList())
    val modules: StateFlow<List<CoreModuleStatus>> = _modules.asStateFlow()

    private val _verifiedCount = MutableStateFlow(0)
    val verifiedCount: StateFlow<Int> = _verifiedCount.asStateFlow()

    suspend fun refresh(): Result<List<CoreModuleStatus>> = runCatching {
        val json = bridge.getCoreInventory()
        val arr = json.optJSONArray("modules") ?: return@runCatching emptyList()
        val list = mutableListOf<CoreModuleStatus>()
        for (i in 0 until arr.length()) {
            val item = arr.getJSONObject(i)
            list.add(
                CoreModuleStatus(
                    filename = item.optString("filename"),
                    module = item.optString("module"),
                    filePresent = item.optBoolean("file_present"),
                    sha256Verified = item.optBoolean("sha256_verified"),
                    importOk = item.optBoolean("import_ok"),
                    apisOk = item.optBoolean("apis_ok"),
                    dependencyStatus = item.optString("dependency_status"),
                    executionEnvironment = item.optString("execution_environment"),
                    compatibility = item.optString("compatibility"),
                    status = item.optString("status"),
                )
            )
        }
        _modules.value = list
        _verifiedCount.value = json.optInt("modules_verified", 0)
        list
    }
}
