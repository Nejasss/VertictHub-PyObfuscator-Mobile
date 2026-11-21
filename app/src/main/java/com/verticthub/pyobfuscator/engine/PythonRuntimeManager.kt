package com.verticthub.pyobfuscator.engine

import android.content.Context
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class RuntimeInitState(val initialized: Boolean = false, val pythonVersion: String = "3.13 (Chaquopy)", val error: String? = null)

class PythonRuntimeManager(private val context: Context) {
    private val _state = MutableStateFlow(RuntimeInitState())
    val state: StateFlow<RuntimeInitState> = _state.asStateFlow()

    @Synchronized
    fun ensureStarted(): Boolean = try {
        if (!Python.isStarted()) Python.start(AndroidPlatform(context.applicationContext))
        val ver = Python.getInstance().getModule("sys")["version"]?.toString()?.substringBefore(" ") ?: "3.13"
        _state.value = RuntimeInitState(initialized = true, pythonVersion = ver, error = null)
        true
    } catch (t: Throwable) {
        _state.value = RuntimeInitState(initialized = false, error = "${t.javaClass.simpleName}: ${t.message}")
        false
    }

    fun getAdapterModule(): PyObject {
        check(ensureStarted()) { _state.value.error ?: "Python runtime belum aktif." }
        return Python.getInstance().getModule("vh.android_adapter.api")
    }
}
