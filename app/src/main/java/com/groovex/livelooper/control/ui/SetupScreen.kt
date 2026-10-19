package com.groovex.livelooper.control.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.groovex.livelooper.control.BuildConfig
import com.groovex.livelooper.control.R
import com.groovex.livelooper.control.update.UpdateUiState
import com.groovex.livelooper.control.update.UpdateViewModel

@Composable
fun SetupScreen(updates: UpdateViewModel, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f).fillMaxHeight().glass(14.dp, LL.Brand0).padding(16.dp).verticalScroll(rememberScrollState())) {
            Text(stringResource(R.string.connection), style = llText(20.sp))
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.connection_hint), style = llText(12.sp, FontWeight.SemiBold, LL.Dim))
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.dex_hint), style = llText(12.sp, FontWeight.SemiBold, LL.Dim))
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.language_hint), style = llText(11.sp, FontWeight.SemiBold, LL.Dim))
        }
        Column(Modifier.weight(1f).fillMaxHeight().glass(14.dp, LL.Brand2).padding(16.dp).verticalScroll(rememberScrollState())) {
            Text(stringResource(R.string.update), style = llText(20.sp))
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.installed_version, BuildConfig.VERSION_NAME), style = llText(12.sp, FontWeight.SemiBold, LL.Dim))
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.channel), style = llText(12.sp), modifier = Modifier.padding(top = 6.dp))
                Pill(stringResource(R.string.channel_stable), !updates.beta) { updates.selectBeta(false) }
                Pill(stringResource(R.string.channel_beta), updates.beta) { updates.selectBeta(true) }
            }
            Spacer(Modifier.height(12.dp))
            when (val s = updates.state) {
                UpdateUiState.Idle -> LLButton(stringResource(R.string.check_updates), Modifier.fillMaxWidth().height(44.dp)) { updates.check() }
                UpdateUiState.Checking -> Text(stringResource(R.string.checking), style = llText(13.sp))
                UpdateUiState.UpToDate -> {
                    Text(stringResource(R.string.up_to_date), style = llText(13.sp, color = LL.Green))
                    Spacer(Modifier.height(8.dp))
                    LLButton(stringResource(R.string.check_updates), Modifier.fillMaxWidth().height(44.dp)) { updates.check() }
                }
                is UpdateUiState.Available -> {
                    Text(stringResource(R.string.update_available), style = llText(15.sp, color = LL.Brand1))
                    Text(stringResource(R.string.update_version, s.release.version.toString()), style = llText(13.sp))
                    Spacer(Modifier.height(10.dp))
                    LLButton(stringResource(R.string.download_install), Modifier.fillMaxWidth().height(44.dp), gradient = true) { updates.downloadAndInstall() }
                    if (s.needsPermission) {
                        Spacer(Modifier.height(6.dp))
                        Text(stringResource(R.string.allow_install), style = llText(11.sp, FontWeight.SemiBold, LL.Yellow))
                    }
                    if (s.release.notes.isNotBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Text(stringResource(R.string.whats_new), style = llText(13.sp))
                        Text(s.release.notes.take(1200), style = llText(11.sp, FontWeight.Normal, LL.Dim))
                    }
                }
                is UpdateUiState.Downloading -> Text(stringResource(R.string.downloading, s.percent), style = llText(13.sp))
                UpdateUiState.Verifying -> Text(stringResource(R.string.verifying), style = llText(13.sp))
                UpdateUiState.Installing -> Text(stringResource(R.string.installing), style = llText(13.sp))
                is UpdateUiState.Error -> {
                    Text(stringResource(R.string.update_error, s.message), style = llText(12.sp, color = LL.Red))
                    Spacer(Modifier.height(8.dp))
                    LLButton(stringResource(R.string.check_updates), Modifier.fillMaxWidth().height(44.dp)) { updates.check() }
                }
            }
            Spacer(Modifier.height(20.dp))
            Text(stringResource(R.string.designed_by), style = llText(13.sp))
            Text("livelooper control · Android · AGPLv3", style = llText(10.sp, FontWeight.SemiBold, LL.Dim))
        }
    }
}
