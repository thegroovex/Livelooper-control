package com.groovex.livelooper.control.update

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.groovex.livelooper.control.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface UpdateUiState {
    data object Idle : UpdateUiState
    data object Checking : UpdateUiState
    data object UpToDate : UpdateUiState
    data class Available(val release: ReleaseInfo, val needsPermission: Boolean = false) : UpdateUiState
    data class Downloading(val percent: Int) : UpdateUiState
    data object Verifying : UpdateUiState
    data object Installing : UpdateUiState
    data class Error(val message: String) : UpdateUiState
}

class UpdateViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("updates", Context.MODE_PRIVATE)
    private val checker = UpdateChecker(BuildConfig.UPDATE_REPO)
    private val manager = UpdateManager(app)

    var state by mutableStateOf<UpdateUiState>(UpdateUiState.Idle)
        private set
    var beta by mutableStateOf(prefs.getBoolean("beta", false))
        private set

    fun selectBeta(value: Boolean) {
        beta = value
        prefs.edit().putBoolean("beta", value).apply()
        check()
    }

    /** Automatic check at start, at most once per 24 h. */
    fun autoCheck() {
        val last = prefs.getLong("lastCheck", 0L)
        if (System.currentTimeMillis() - last > 24L * 3600 * 1000) check(silent = true)
    }

    fun check(silent: Boolean = false) {
        if (state is UpdateUiState.Downloading || state == UpdateUiState.Installing) return
        state = UpdateUiState.Checking
        viewModelScope.launch {
            state = try {
                val latest = withContext(Dispatchers.IO) { checker.fetchLatest(beta) }
                prefs.edit().putLong("lastCheck", System.currentTimeMillis()).apply()
                val current = Version.parse(BuildConfig.VERSION_NAME)
                if (latest != null && (current == null || latest.version > current)) UpdateUiState.Available(latest)
                else if (silent) UpdateUiState.Idle else UpdateUiState.UpToDate
            } catch (e: Exception) {
                if (silent) UpdateUiState.Idle else UpdateUiState.Error(e.message ?: e.javaClass.simpleName)
            }
        }
    }

    fun downloadAndInstall() {
        val avail = state as? UpdateUiState.Available ?: return
        if (!manager.canInstall()) {
            state = avail.copy(needsPermission = true)
            manager.openInstallPermissionSettings()
            return
        }
        viewModelScope.launch {
            state = try {
                state = UpdateUiState.Downloading(0)
                val file = manager.download(avail.release) { p -> state = UpdateUiState.Downloading(p) }
                state = UpdateUiState.Verifying
                if (!manager.verify(file, avail.release)) throw IllegalStateException("SHA-256 mismatch")
                state = UpdateUiState.Installing
                manager.install(file)
                UpdateUiState.Installing
            } catch (e: Exception) {
                UpdateUiState.Error(e.message ?: e.javaClass.simpleName)
            }
        }
    }
}
