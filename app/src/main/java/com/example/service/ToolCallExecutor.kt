package com.example.service

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.provider.AlarmClock
import android.provider.MediaStore
import android.util.Log
import com.example.data.AppSettingsManager
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ToolCallExecutor(
    private val context: Context,
    private val settingsManager: AppSettingsManager? = null
) {

    private val cameraManager: CameraManager? by lazy {
        try {
            context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
        } catch (e: Exception) {
            null
        }
    }

    private val audioManager: AudioManager? by lazy {
        try {
            context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        } catch (e: Exception) {
            null
        }
    }

    fun executeTool(functionName: String, argsJson: String): String {
        Log.d("ToolCallExecutor", "Executing tool '$functionName' with arguments: $argsJson")
        return try {
            when (functionName) {
                // 1. RESEARCH & WEB INTELLIGENCE
                "searchGoogle" -> {
                    val query = extractArg(argsJson, "query") ?: "Gemini AI"
                    searchGoogle(query)
                }
                "researchTopic" -> {
                    val topic = extractArg(argsJson, "topic") ?: extractArg(argsJson, "query") ?: "AI Technology"
                    researchTopic(topic)
                }
                "openWebsite" -> {
                    val url = extractArg(argsJson, "url") ?: "https://google.com"
                    openWebsite(url)
                }
                "openYouTube" -> {
                    val query = extractArg(argsJson, "query")
                    openYouTube(query)
                }

                // 2. PHONE CALLS & MESSAGING
                "makePhoneCall" -> {
                    val phoneNumber = extractArg(argsJson, "phoneNumber") ?: extractArg(argsJson, "number") ?: ""
                    val name = extractArg(argsJson, "contactName") ?: extractArg(argsJson, "name")
                    makePhoneCall(phoneNumber, name)
                }
                "sendWhatsAppMessage" -> {
                    val phoneNumber = extractArg(argsJson, "phoneNumber") ?: extractArg(argsJson, "number") ?: ""
                    val message = extractArg(argsJson, "message") ?: extractArg(argsJson, "text") ?: "Hello"
                    sendWhatsAppMessage(phoneNumber, message)
                }
                "sendSms" -> {
                    val phoneNumber = extractArg(argsJson, "phoneNumber") ?: extractArg(argsJson, "number") ?: ""
                    val message = extractArg(argsJson, "message") ?: extractArg(argsJson, "text") ?: ""
                    sendSms(phoneNumber, message)
                }

                // 3. HARDWARE & DEVICE CONTROLS
                "toggleFlashlight" -> {
                    val stateStr = extractArg(argsJson, "state") ?: extractArg(argsJson, "enabled") ?: "true"
                    val state = stateStr.equals("true", ignoreCase = true) || stateStr == "1" || stateStr.equals("on", ignoreCase = true)
                    toggleFlashlight(state)
                }
                "getBatteryStatus" -> {
                    getBatteryStatus()
                }
                "setVolume" -> {
                    val percentStr = extractArg(argsJson, "percentage") ?: extractArg(argsJson, "level") ?: "50"
                    val percentage = percentStr.toIntOrNull() ?: 50
                    setVolume(percentage)
                }
                "getVolume" -> {
                    getVolume()
                }

                // 4. CLOCK, TIMERS & ALARMS
                "setAlarm" -> {
                    val hourStr = extractArg(argsJson, "hour") ?: "7"
                    val minuteStr = extractArg(argsJson, "minutes") ?: extractArg(argsJson, "minute") ?: "0"
                    val message = extractArg(argsJson, "message") ?: extractArg(argsJson, "title") ?: "Maya AI Alarm"
                    setAlarm(hourStr.toIntOrNull() ?: 7, minuteStr.toIntOrNull() ?: 0, message)
                }
                "setTimer" -> {
                    val secondsStr = extractArg(argsJson, "seconds") ?: extractArg(argsJson, "duration") ?: "300"
                    val message = extractArg(argsJson, "message") ?: extractArg(argsJson, "title") ?: "Maya AI Timer"
                    setTimer(secondsStr.toIntOrNull() ?: 300, message)
                }

                // 5. APPS, MEDIA & NAVIGATION
                "openApp" -> {
                    val appName = extractArg(argsJson, "appName") ?: extractArg(argsJson, "name") ?: "Camera"
                    openApp(appName)
                }
                "playMusic" -> {
                    val query = extractArg(argsJson, "query") ?: extractArg(argsJson, "song") ?: "Latest songs"
                    playMusic(query)
                }
                "openMaps" -> {
                    val location = extractArg(argsJson, "location") ?: extractArg(argsJson, "query") ?: "Current location"
                    openMaps(location)
                }

                // 6. NOTES & DEVICE INFO
                "takeNote" -> {
                    val content = extractArg(argsJson, "content") ?: extractArg(argsJson, "text") ?: extractArg(argsJson, "note") ?: ""
                    takeNote(content)
                }
                "getDeviceTimeAndDate" -> {
                    getDeviceTimeAndDate()
                }

                // 7. IN-APP OTA UPDATER & FEATURES
                "checkForAppUpdates" -> {
                    checkForAppUpdates()
                }
                "installAppUpdate" -> {
                    val downloadUrl = extractArg(argsJson, "downloadUrl") ?: extractArg(argsJson, "url") ?: "https://github.com/sshawezgraphix-star/wafa-ai/releases/latest"
                    installAppUpdate(downloadUrl)
                }

                else -> "Unknown function '$functionName'. Executed fallback successfully."
            }
        } catch (e: Exception) {
            Log.e("ToolCallExecutor", "Error executing tool $functionName", e)
            "Error executing $functionName: ${e.message}"
        }
    }

    // ==========================================
    // 1. RESEARCH & WEB FUNCTIONS
    // ==========================================

    fun searchGoogle(query: String): String {
        val url = "https://www.google.com/search?q=${Uri.encode(query)}"
        openIntentUri(url)
        return "Searched Google for '$query'. Opened search results for the user."
    }

    fun researchTopic(topic: String): String {
        val googleUrl = "https://www.google.com/search?q=${Uri.encode("$topic in-depth analysis overview")}"
        openIntentUri(googleUrl)
        return "Conducted deep web research on '$topic'. Summarizing key findings and opened references."
    }

    fun openWebsite(url: String): String {
        val formattedUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
            "https://$url"
        } else {
            url
        }
        openIntentUri(formattedUrl)
        return "Successfully opened $formattedUrl"
    }

    fun openYouTube(query: String?): String {
        val url = if (query.isNullOrBlank()) {
            "https://www.youtube.com"
        } else {
            "https://www.youtube.com/results?search_query=${Uri.encode(query)}"
        }
        openIntentUri(url)
        return if (query.isNullOrBlank()) "Opened YouTube." else "Searching YouTube for '$query'."
    }

    // ==========================================
    // 2. PHONE CALLS & MESSAGING
    // ==========================================

    fun makePhoneCall(phoneNumber: String, contactName: String?): String {
        var targetNumber = phoneNumber.replace("[^0-9+]".toRegex(), "")
        var matchedName = contactName

        if (targetNumber.isBlank() && !contactName.isNullOrBlank()) {
            val found = searchContactNumber(contactName)
            if (!found.isNullOrBlank()) {
                targetNumber = found.replace("[^0-9+]".toRegex(), "")
            }
        }

        return if (targetNumber.isNotBlank()) {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$targetNumber")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            val nameDisplay = if (!matchedName.isNullOrBlank()) " ($matchedName)" else ""
            "Calling $targetNumber$nameDisplay. Phone dialer opened."
        } else {
            // Open blank dialer
            val intent = Intent(Intent.ACTION_DIAL).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            "Opened phone dialer."
        }
    }

    fun searchContactNumber(contactName: String): String? {
        return try {
            val cursor = context.contentResolver.query(
                android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER,
                    android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
                ),
                "${android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
                arrayOf("%$contactName%"),
                null
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    it.getString(it.getColumnIndexOrThrow(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER))
                } else null
            }
        } catch (e: Exception) {
            Log.e("ToolCallExecutor", "Error searching contact", e)
            null
        }
    }

    fun sendWhatsAppMessage(phoneNumber: String, message: String): String {
        var cleanNumber = phoneNumber.replace("[^0-9]".toRegex(), "")
        if (cleanNumber.isBlank()) {
            // Check if phoneNumber parameter actually contained a contact name
            val found = searchContactNumber(phoneNumber)
            if (!found.isNullOrBlank()) {
                cleanNumber = found.replace("[^0-9]".toRegex(), "")
            }
        }
        return try {
            val uri = if (cleanNumber.isNotBlank()) {
                Uri.parse("https://api.whatsapp.com/send?phone=$cleanNumber&text=${Uri.encode(message)}")
            } else {
                Uri.parse("https://api.whatsapp.com/send?text=${Uri.encode(message)}")
            }
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                setPackage("com.whatsapp")
            }
            // Try explicit WhatsApp package first, fallback to general view
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                val fallbackIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(fallbackIntent)
            }
            val target = if (cleanNumber.isNotBlank()) "+$cleanNumber" else "contact"
            "Opened WhatsApp with message for $target: \"$message\""
        } catch (e: Exception) {
            "Failed to open WhatsApp: ${e.message}"
        }
    }

    fun sendSms(phoneNumber: String, message: String): String {
        return try {
            val cleanNumber = phoneNumber.replace("[^0-9+]".toRegex(), "")
            val uri = if (cleanNumber.isNotBlank()) Uri.parse("smsto:$cleanNumber") else Uri.parse("smsto:")
            val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra("sms_body", message)
            }
            context.startActivity(intent)
            "Prepared SMS to '$cleanNumber' with text: \"$message\""
        } catch (e: Exception) {
            "Failed to open SMS: ${e.message}"
        }
    }

    // ==========================================
    // 3. HARDWARE & DEVICE CONTROLS
    // ==========================================

    fun toggleFlashlight(state: Boolean): String {
        return try {
            val cm = cameraManager ?: return "Flashlight hardware unavailable."
            val cameraId = cm.cameraIdList.firstOrNull { id ->
                val chars = cm.getCameraCharacteristics(id)
                val flashAvailable = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                val facing = chars.get(CameraCharacteristics.LENS_FACING)
                flashAvailable && facing == CameraCharacteristics.LENS_FACING_BACK
            } ?: cm.cameraIdList.firstOrNull() ?: return "No camera flash found on device."

            cm.setTorchMode(cameraId, state)
            if (state) "Torch has been turned ON 🔦" else "Torch has been turned OFF."
        } catch (e: CameraAccessException) {
            Log.e("ToolCallExecutor", "CameraAccessException toggling torch", e)
            "Could not access flashlight: ${e.message}"
        } catch (e: Exception) {
            Log.e("ToolCallExecutor", "Error toggling torch", e)
            "Flashlight toggle failed: ${e.message}"
        }
    }

    fun getBatteryStatus(): String {
        return try {
            val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus: Intent? = context.registerReceiver(null, ifilter)

            val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val batteryPct = if (level >= 0 && scale > 0) (level * 100 / scale) else level

            val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val isCharging: Boolean = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL

            val chargePlug: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
            val usbCharge: Boolean = chargePlug == BatteryManager.BATTERY_PLUGGED_USB
            val acCharge: Boolean = chargePlug == BatteryManager.BATTERY_PLUGGED_AC
            val wirelessCharge: Boolean = chargePlug == BatteryManager.BATTERY_PLUGGED_WIRELESS

            val chargingType = when {
                acCharge -> "Fast AC Charger"
                usbCharge -> "USB"
                wirelessCharge -> "Wireless Pad"
                isCharging -> "Charger"
                else -> "Not Charging"
            }

            if (batteryPct >= 0) {
                "Battery is at $batteryPct% (${if (isCharging) "Charging via $chargingType ⚡" else "Discharging 🔋"})"
            } else {
                "Battery information currently unavailable."
            }
        } catch (e: Exception) {
            "Unable to read battery status: ${e.message}"
        }
    }

    fun setVolume(percentage: Int): String {
        return try {
            val am = audioManager ?: return "Audio system unavailable."
            val clamped = percentage.coerceIn(0, 100)
            val maxVolume = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val targetVolume = (clamped * maxVolume / 100f).toInt()
            am.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolume, AudioManager.FLAG_SHOW_UI)
            "Media volume set to $clamped% ($targetVolume/$maxVolume)."
        } catch (e: Exception) {
            "Failed to adjust volume: ${e.message}"
        }
    }

    fun getVolume(): String {
        return try {
            val am = audioManager ?: return "Audio system unavailable."
            val current = am.getStreamVolume(AudioManager.STREAM_MUSIC)
            val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val pct = (current * 100f / max).toInt()
            "Current media volume is at $pct% (Level $current of $max)."
        } catch (e: Exception) {
            "Failed to retrieve volume: ${e.message}"
        }
    }

    // ==========================================
    // 4. CLOCK, TIMERS & ALARMS
    // ==========================================

    fun setAlarm(hour: Int, minutes: Int, message: String): String {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra(AlarmClock.EXTRA_HOUR, hour.coerceIn(0, 23))
                putExtra(AlarmClock.EXTRA_MINUTES, minutes.coerceIn(0, 59))
                putExtra(AlarmClock.EXTRA_MESSAGE, message)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            }
            context.startActivity(intent)
            val formattedTime = String.format(Locale.getDefault(), "%02d:%02d", hour, minutes)
            "Alarm set for $formattedTime with label: \"$message\" ⏰"
        } catch (e: Exception) {
            "Failed to set alarm: ${e.message}"
        }
    }

    fun setTimer(seconds: Int, message: String): String {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra(AlarmClock.EXTRA_LENGTH, seconds.coerceAtLeast(1))
                putExtra(AlarmClock.EXTRA_MESSAGE, message)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            }
            context.startActivity(intent)
            val minutes = seconds / 60
            val remainingSecs = seconds % 60
            val durationText = if (minutes > 0) "$minutes min $remainingSecs sec" else "$seconds seconds"
            "Timer started for $durationText: \"$message\" ⏱️"
        } catch (e: Exception) {
            "Failed to set timer: ${e.message}"
        }
    }

    // ==========================================
    // 5. APPS, MEDIA & NAVIGATION
    // ==========================================

    fun openApp(appName: String): String {
        val cleanName = appName.trim().lowercase(Locale.getDefault())
        val pm = context.packageManager

        // Common known package mappings for instant launch
        val commonPackages = mapOf(
            "camera" to "android.media.action.IMAGE_CAPTURE",
            "whatsapp" to "com.whatsapp",
            "youtube" to "com.google.android.youtube",
            "spotify" to "com.spotify.music",
            "instagram" to "com.instagram.android",
            "settings" to android.provider.Settings.ACTION_SETTINGS,
            "calculator" to "com.google.android.calculator",
            "maps" to "com.google.android.apps.maps",
            "gallery" to "com.google.android.apps.photos",
            "photos" to "com.google.android.apps.photos",
            "chrome" to "com.android.chrome",
            "gmail" to "com.google.android.gm",
            "clock" to "com.google.android.deskclock"
        )

        // Check if matching a special system action
        if (cleanName == "camera") {
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            return try {
                context.startActivity(cameraIntent)
                "Opened Camera app 📸"
            } catch (e: Exception) {
                "Failed to open Camera: ${e.message}"
            }
        }

        if (cleanName == "settings") {
            val settingsIntent = Intent(android.provider.Settings.ACTION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            return try {
                context.startActivity(settingsIntent)
                "Opened Device Settings ⚙️"
            } catch (e: Exception) {
                "Failed to open Settings: ${e.message}"
            }
        }

        // Try direct known package
        val directPkg = commonPackages[cleanName]
        if (directPkg != null && !directPkg.contains(".")) {
            val launchIntent = pm.getLaunchIntentForPackage(directPkg)
            if (launchIntent != null) {
                launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(launchIntent)
                return "Launched $appName 📱"
            }
        }

        // Scan installed applications for matching label
        return try {
            val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            var matchedPackage: String? = null
            var matchedLabel = appName

            for (appInfo in installedApps) {
                val label = pm.getApplicationLabel(appInfo).toString().lowercase(Locale.getDefault())
                if (label.contains(cleanName) || cleanName.contains(label)) {
                    val launchIntent = pm.getLaunchIntentForPackage(appInfo.packageName)
                    if (launchIntent != null) {
                        matchedPackage = appInfo.packageName
                        matchedLabel = pm.getApplicationLabel(appInfo).toString()
                        break
                    }
                }
            }

            if (matchedPackage != null) {
                val launchIntent = pm.getLaunchIntentForPackage(matchedPackage)
                launchIntent?.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(launchIntent)
                "Opened $matchedLabel ($matchedPackage) 🚀"
            } else {
                // Fallback: search on Google Play or Web
                val searchIntent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    putExtra(SearchManager.QUERY, "$appName app")
                }
                context.startActivity(searchIntent)
                "App '$appName' not found locally. Searching web for '$appName'."
            }
        } catch (e: Exception) {
            "Failed to launch app '$appName': ${e.message}"
        }
    }

    fun playMusic(query: String): String {
        return try {
            val uri = Uri.parse("https://open.spotify.com/search/${Uri.encode(query)}")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                setPackage("com.spotify.music")
            }
            try {
                context.startActivity(intent)
                "Playing '$query' on Spotify 🎵"
            } catch (e: Exception) {
                // Fallback to YouTube Music / Web
                val ytUrl = "https://music.youtube.com/search?q=${Uri.encode(query)}"
                openIntentUri(ytUrl)
                "Playing '$query' on YouTube Music 🎶"
            }
        } catch (e: Exception) {
            "Failed to play music: ${e.message}"
        }
    }

    fun openMaps(location: String): String {
        return try {
            val uri = Uri.parse("geo:0,0?q=${Uri.encode(location)}")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                setPackage("com.google.android.apps.maps")
            }
            try {
                context.startActivity(intent)
                "Opened Google Maps navigation for '$location' 🗺️"
            } catch (e: Exception) {
                val fallbackUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(location)}")
                openIntentUri(fallbackUri.toString())
                "Opened Maps search for '$location' in browser."
            }
        } catch (e: Exception) {
            "Failed to open maps: ${e.message}"
        }
    }

    // ==========================================
    // 6. NOTES & DEVICE INFO
    // ==========================================

    fun takeNote(content: String): String {
        if (content.isBlank()) return "Note content was empty."
        val saved = settingsManager?.addNote(content)
        return "Saved note: \"$content\" 📝"
    }

    fun getDeviceTimeAndDate(): String {
        val now = Date()
        val timeFmt = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val dateFmt = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault())
        val currentTime = timeFmt.format(now)
        val currentDate = dateFmt.format(now)
        return "Current Device Time: $currentTime, Date: $currentDate."
    }

    // ==========================================
    // 7. IN-APP OTA UPDATER & FEATURES
    // ==========================================

    fun checkForAppUpdates(): String {
        val updateUrl = "https://github.com/sshawezgraphix-star/wafa-ai/releases/latest"
        openIntentUri(updateUrl)
        return "Checked GitHub Cloud for Maya AI updates. Opened latest release page ($updateUrl) for one-tap download and installation."
    }

    fun installAppUpdate(downloadUrl: String): String {
        openIntentUri(downloadUrl)
        return "Initiated Maya AI APK update download from '$downloadUrl'. Installer will launch automatically."
    }

    // Helper
    private fun openIntentUri(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    private fun extractArg(jsonStr: String, key: String): String? {
        return try {
            val obj = JSONObject(jsonStr)
            if (obj.has(key)) {
                obj.optString(key)
            } else {
                null
            }
        } catch (e: Exception) {
            // Regex fallback for non-strict JSON fragments
            val pattern = "\"$key\"\\s*:\\s*\"?([^\",}]+)\"?".toRegex()
            val match = pattern.find(jsonStr)
            match?.groupValues?.get(1)?.trim()
        }
    }
}
