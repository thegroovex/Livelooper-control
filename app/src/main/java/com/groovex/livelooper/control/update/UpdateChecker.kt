package com.groovex.livelooper.control.update

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class ReleaseInfo(
    val version: Version,
    val tag: String,
    val notes: String,
    val apkUrl: String,
    val apkSize: Long,
    val sha256Url: String?,
)

/** Reads GitHub Releases. Stable = newest non-prerelease, Beta = newest of all. */
class UpdateChecker(private val repo: String) {

    fun fetchLatest(beta: Boolean): ReleaseInfo? {
        val conn = URL("https://api.github.com/repos/$repo/releases?per_page=15").openConnection() as HttpURLConnection
        conn.setRequestProperty("Accept", "application/vnd.github+json")
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        try {
            if (conn.responseCode != 200) throw IllegalStateException("HTTP ${conn.responseCode}")
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            return pickRelease(JSONArray(body), beta)
        } finally {
            conn.disconnect()
        }
    }

    companion object {
        fun pickRelease(releases: JSONArray, beta: Boolean): ReleaseInfo? {
            var best: ReleaseInfo? = null
            for (i in 0 until releases.length()) {
                val r = releases.getJSONObject(i)
                if (r.optBoolean("draft")) continue
                if (!beta && r.optBoolean("prerelease")) continue
                val info = parse(r) ?: continue
                if (best == null || info.version > best.version) best = info
            }
            return best
        }

        private fun parse(r: JSONObject): ReleaseInfo? {
            val tag = r.optString("tag_name")
            val v = Version.parse(tag) ?: return null
            val assets = r.optJSONArray("assets") ?: return null
            var apk: JSONObject? = null
            var sha: String? = null
            for (j in 0 until assets.length()) {
                val a = assets.getJSONObject(j)
                val name = a.optString("name")
                when {
                    name.endsWith(".apk") -> apk = a
                    name.endsWith(".apk.sha256") -> sha = a.optString("browser_download_url")
                }
            }
            apk ?: return null
            return ReleaseInfo(v, tag, r.optString("body"), apk.optString("browser_download_url"), apk.optLong("size"), sha)
        }
    }
}
