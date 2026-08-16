package com.example.service

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import com.example.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

sealed class UpdateStatus {
    object Idle : UpdateStatus()
    object Checking : UpdateStatus()
    data class UpdateAvailable(
        val versionTag: String,
        val releaseName: String,
        val changelog: String,
        val downloadUrl: String,
        val sizeBytes: Long
    ) : UpdateStatus()
    data class Downloading(val progress: Float) : UpdateStatus()
    object UpToDate : UpdateStatus()
    data class Error(val message: String) : UpdateStatus()
}

class AppUpdateManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val _updateStatus = MutableStateFlow<UpdateStatus>(UpdateStatus.Idle)
    val updateStatus: StateFlow<UpdateStatus> = _updateStatus.asStateFlow()

    val currentVersion: String = "v${BuildConfig.VERSION_NAME} (Build 6)"

    fun checkForUpdates(onComplete: ((Boolean, String) -> Unit)? = null) {
        _updateStatus.value = UpdateStatus.Checking
        scope.launch {
            try {
                val url = "https://api.github.com/repos/sshawezgraphix-star/wafa-ai/releases/latest"
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Maya-AI-Updater")
                    .header("Accept", "application/vnd.github.v3+json")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        val errMsg = "GitHub server returned code ${response.code}"
                        _updateStatus.value = UpdateStatus.Error(errMsg)
                        withContext(Dispatchers.Main) { onComplete?.invoke(false, errMsg) }
                        return@launch
                    }

                    val jsonStr = response.body?.string() ?: "{}"
                    val json = JSONObject(jsonStr)

                    val tagName = json.optString("tag_name", "")
                    val releaseName = json.optString("name", "New Maya AI Update")
                    val body = json.optString("body", "New features & performance enhancements.")

                    val assets = json.optJSONArray("assets")
                    var downloadUrl = ""
                    var sizeBytes = 0L

                    if (assets != null) {
                        for (i in 0 until assets.length()) {
                            val asset = assets.getJSONObject(i)
                            val name = asset.optString("name", "")
                            if (name.endsWith(".apk")) {
                                downloadUrl = asset.optString("browser_download_url", "")
                                sizeBytes = asset.optLong("size", 0L)
                                break
                            }
                        }
                    }

                    if (downloadUrl.isBlank()) {
                        downloadUrl = "https://github.com/sshawezgraphix-star/wafa-ai/releases/download/$tagName/app-debug.apk"
                    }

                    // Check if tag is newer than current build
                    val isNewer = isTagNewer(tagName, currentVersion)

                    if (isNewer && downloadUrl.isNotBlank()) {
                        val available = UpdateStatus.UpdateAvailable(
                            versionTag = tagName,
                            releaseName = releaseName,
                            changelog = body,
                            downloadUrl = downloadUrl,
                            sizeBytes = sizeBytes
                        )
                        _updateStatus.value = available
                        withContext(Dispatchers.Main) {
                            onComplete?.invoke(true, "New update $tagName available!")
                        }
                    } else {
                        _updateStatus.value = UpdateStatus.UpToDate
                        withContext(Dispatchers.Main) {
                            onComplete?.invoke(false, "You are on the latest version of Maya AI.")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("AppUpdateManager", "Error checking for updates", e)
                val errMsg = e.localizedMessage ?: "Failed to connect to update server"
                _updateStatus.value = UpdateStatus.Error(errMsg)
                withContext(Dispatchers.Main) { onComplete?.invoke(false, errMsg) }
            }
        }
    }

    fun downloadAndInstall(downloadUrl: String) {
        _updateStatus.value = UpdateStatus.Downloading(0f)
        scope.launch {
            try {
                val apkFile = File(context.cacheDir, "maya_ai_update.apk")
                if (apkFile.exists()) apkFile.delete()

                val request = Request.Builder()
                    .url(downloadUrl)
                    .header("User-Agent", "Mozilla/5.0")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        _updateStatus.value = UpdateStatus.Error("Failed to download APK (${response.code})")
                        return@launch
                    }

                    val body = response.body ?: throw Exception("Empty response body")
                    val totalBytes = body.contentLength()
                    var bytesDownloaded = 0L

                    body.byteStream().use { inputStream ->
                        FileOutputStream(apkFile).use { outputStream ->
                            val buffer = ByteArray(8192)
                            var read: Int
                            while (inputStream.read(buffer).also { read = it } != -1) {
                                outputStream.write(buffer, 0, read)
                                bytesDownloaded += read
                                if (totalBytes > 0) {
                                    val progress = bytesDownloaded.toFloat() / totalBytes.toFloat()
                                    _updateStatus.value = UpdateStatus.Downloading(progress)
                                }
                            }
                            outputStream.flush()
                        }
                    }

                    withContext(Dispatchers.Main) {
                        installApk(apkFile)
                    }
                }
            } catch (e: Exception) {
                Log.e("AppUpdateManager", "Download error", e)
                _updateStatus.value = UpdateStatus.Error("Download failed: ${e.localizedMessage}")
            }
        }
    }

    private fun installApk(apkFile: File) {
        try {
            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                apkFile
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            _updateStatus.value = UpdateStatus.Idle
        } catch (e: Exception) {
            Log.e("AppUpdateManager", "Error launching APK installer", e)
            _updateStatus.value = UpdateStatus.Error("Installer error: ${e.localizedMessage}")
        }
    }

    private fun isTagNewer(latestTag: String, current: String): Boolean {
        // e.g. v1.0.0-build-7 vs v1.0 (Build 6)
        val latestBuildNum = Regex("build-(\\d+)").find(latestTag)?.groupValues?.get(1)?.toIntOrNull()
        val currentBuildNum = Regex("Build (\\d+)").find(current)?.groupValues?.get(1)?.toIntOrNull()

        if (latestBuildNum != null && currentBuildNum != null) {
            return latestBuildNum > currentBuildNum
        }
        return latestTag.isNotBlank() && !current.contains(latestTag, ignoreCase = true)
    }
}
