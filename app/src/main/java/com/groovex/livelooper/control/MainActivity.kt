package com.groovex.livelooper.control

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.Display
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.remember
import com.groovex.livelooper.control.model.SurfaceModel
import com.groovex.livelooper.control.ui.App
import com.groovex.livelooper.control.ui.LivelooperTheme
import com.groovex.livelooper.control.update.UpdateViewModel

class MainActivity : ComponentActivity() {
    private val updates: UpdateViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        applyOrientation()
        updates.autoCheck()
        setContent {
            val model = remember { SurfaceModel() }
            LivelooperTheme { App(model, updates) }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        applyOrientation()
    }

    /**
     * Landscape lock only on the phone's own screen in full screen. On DeX, external displays and in
     * multi-window the window stays freely resizable (an orientation lock would block resizing).
     */
    private fun applyOrientation() {
        val displayId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) display?.displayId ?: Display.DEFAULT_DISPLAY
        else @Suppress("DEPRECATION") windowManager.defaultDisplay.displayId
        val ownScreen = displayId == Display.DEFAULT_DISPLAY
        val dex = resources.configuration.let { isDex(it) }
        val phone = resources.configuration.smallestScreenWidthDp < 600
        requestedOrientation = if (ownScreen && phone && !dex && !isInMultiWindowMode)
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    private fun isDex(config: Configuration): Boolean = try {
        val c = config.javaClass
        c.getField("semDesktopModeEnabled").getInt(config) == c.getField("SEM_DESKTOP_MODE_ENABLED").getInt(c)
    } catch (_: Exception) {
        false
    }
}
