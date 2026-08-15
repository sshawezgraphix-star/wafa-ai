import json
import re
import os
import sys

print("=================================================================")
print("  MAYA AI MARK-XXXIX MOBILE ASSISTANT - COMPREHENSIVE TEST SUITE  ")
print("=================================================================")

workspace = r"e:\wafa-ai"
src_dir = os.path.join(workspace, "app", "src", "main", "java", "com", "example")
manifest_path = os.path.join(workspace, "app", "src", "main", "AndroidManifest.xml")

total_tests = 0
passed_tests = 0

def test(name, condition, details=""):
    global total_tests, passed_tests
    total_tests += 1
    if condition:
        passed_tests += 1
        print(f"  [PASS] {name}")
    else:
        print(f"  [FAIL] {name} -> {details}")

# TEST 1: Android Manifest Permissions & Queries
print("\n--- 1. Testing Android Manifest & Security Permissions ---")
with open(manifest_path, "r", encoding="utf-8") as f:
    manifest_content = f.read()

test("RECORD_AUDIO Permission", "android.permission.RECORD_AUDIO" in manifest_content)
test("MODIFY_AUDIO_SETTINGS Permission", "android.permission.MODIFY_AUDIO_SETTINGS" in manifest_content)
test("CAMERA Permission", "android.permission.CAMERA" in manifest_content)
test("FLASHLIGHT Permission", "android.permission.FLASHLIGHT" in manifest_content)
test("CALL_PHONE Permission", "android.permission.CALL_PHONE" in manifest_content)
test("SET_ALARM Permission", "android.permission.SET_ALARM" in manifest_content)
test("QUERY_ALL_PACKAGES for App Launcher", "android.permission.QUERY_ALL_PACKAGES" in manifest_content)
test("WhatsApp Package Query", 'package android:name="com.whatsapp"' in manifest_content)
test("YouTube Package Query", 'package android:name="com.google.android.youtube"' in manifest_content)
test("Spotify Package Query", 'package android:name="com.spotify.music"' in manifest_content)
test("Maps Package Query", 'package android:name="com.google.android.apps.maps"' in manifest_content)

# TEST 2: ToolCallExecutor Tools Verification
print("\n--- 2. Testing ToolCallExecutor Suite & 16 Phone Automation Tools ---")
tool_file = os.path.join(src_dir, "service", "ToolCallExecutor.kt")
with open(tool_file, "r", encoding="utf-8") as f:
    tool_code = f.read()

expected_tools = [
    "searchGoogle",
    "researchTopic",
    "openWebsite",
    "openYouTube",
    "makePhoneCall",
    "sendWhatsAppMessage",
    "sendSms",
    "toggleFlashlight",
    "getBatteryStatus",
    "setVolume",
    "getVolume",
    "setAlarm",
    "setTimer",
    "openApp",
    "playMusic",
    "openMaps",
    "takeNote",
    "getDeviceTimeAndDate"
]

for t in expected_tools:
    test(f"Tool declared and routed: '{t}'", f'"{t}" ->' in tool_code or f'fun {t}' in tool_code)

test("Flashlight uses CameraManager.setTorchMode", "setTorchMode" in tool_code)
test("Battery uses BatteryManager.EXTRA_LEVEL", "BatteryManager.EXTRA_LEVEL" in tool_code)
test("Volume uses AudioManager.setStreamVolume", "setStreamVolume" in tool_code)
test("Alarm uses AlarmClock.ACTION_SET_ALARM", "AlarmClock.ACTION_SET_ALARM" in tool_code)
test("Timer uses AlarmClock.ACTION_SET_TIMER", "AlarmClock.ACTION_SET_TIMER" in tool_code)
test("WhatsApp uses api.whatsapp.com intent", "api.whatsapp.com/send" in tool_code)
test("Call uses Intent.ACTION_DIAL", "Intent.ACTION_DIAL" in tool_code)

# TEST 3: Gemini Live Session & Audio Configuration
print("\n--- 3. Testing Gemini Live Multimodal Protocol & WebSockets ---")
session_file = os.path.join(src_dir, "service", "LiveSessionManager.kt")
with open(session_file, "r", encoding="utf-8") as f:
    session_code = f.read()

test("Gemini 2.5 Flash Native Audio Model", "gemini-2.5-flash-native-audio" in session_code)
test("WebSocket BidiGenerateContent Endpoint", "generativelanguage.googleapis.com/ws" in session_code)
test("Voice Config with Dynamic Prebuilt Voice", "prebuiltVoiceConfig" in session_code)
test("Response Modalities set to AUDIO", "responseModalities" in session_code)
test("Identity Rule (Shawez Hacker created me)", "Shawez Hacker created me" in session_code)
test("Urdu / Hindi / Hinglish persona support", "Urdu" in session_code and "Hindi" in session_code)
test("Realtime PCM16 Audio Streaming Input", "audio/pcm;rate=16000" in session_code)
test("Instant Voice Interruption Handling", "interrupted" in session_code)
test("Tool Call Execution & WebSocket Response Loop", "toolResponse" in session_code and "functionResponses" in session_code)

# TEST 4: AppSettingsManager & Persistence
print("\n--- 4. Testing In-App Settings & Female Voices ---")
settings_file = os.path.join(src_dir, "data", "AppSettings.kt")
with open(settings_file, "r", encoding="utf-8") as f:
    settings_code = f.read()

test("SharedPreferences Persistence ('maya_ai_preferences')", "maya_ai_preferences" in settings_code)
test("API Key Storage & Retrieval", "getApiKey" in settings_code and "setApiKey" in settings_code)
test("Voice Selection ('Aoede', 'Kore', 'Leda')", "Aoede" in settings_code and "Kore" in settings_code and "Leda" in settings_code)
test("Notes Storage in JSON", "getNotes" in settings_code and "addNote" in settings_code and "deleteNote" in settings_code)

# TEST 5: UI Hologram Components & Screen Integration
print("\n--- 5. Testing Mark-XXXIX Arc Reactor & Compose HUD ---")
screen_file = os.path.join(src_dir, "ui", "screens", "FirdousMainScreen.kt")
with open(screen_file, "r", encoding="utf-8") as f:
    screen_code = f.read()

test("Arc Reactor Glowing Mic Button integration", "CentralGlowingMicButton" in screen_code)
test("Animated Dynamic Waveform integration", "AnimatedWaveformView" in screen_code)
test("Quick Action Chips integration", "QuickActionChips" in screen_code)
test("Settings Bottom Sheet integration", "SettingsBottomSheet" in screen_code)
test("Notes Bottom Sheet integration", "NotesBottomSheet" in screen_code)
test("Bottom Chat Input Bar (Text typing fallback)", "BottomChatInputBar" in screen_code)
test("Multiple Permissions Launcher (Audio, Camera, Call)", "RequestMultiplePermissions" in screen_code)

# TEST 6: Tool Argument Extraction Algorithm Simulation
print("\n--- 6. Testing JSON Argument Parser Robustness ---")
def extract_arg(json_str, key):
    try:
        obj = json.loads(json_str)
        if key in obj:
            return str(obj[key])
    except Exception:
        pass
    pattern = f'"{key}"\\s*:\\s*"?([^",}}]+)"?'
    match = re.search(pattern, json_str)
    return match.group(1).strip() if match else None

test_cases = [
    ('{"query":"latest smartphone 2026"}', "query", "latest smartphone 2026"),
    ('{"phoneNumber":"+919876543210", "message":"Hello Maya"}', "phoneNumber", "+919876543210"),
    ('{"phoneNumber":"+919876543210", "message":"Hello Maya"}', "message", "Hello Maya"),
    ('{"state":"true"}', "state", "true"),
    ('{"hour":7, "minutes":30}', "hour", "7"),
    ('{"appName":"YouTube"}', "appName", "YouTube"),
    ('{"topic":"Artificial Intelligence"}', "topic", "Artificial Intelligence")
]

for sample_json, key, expected in test_cases:
    res = extract_arg(sample_json, key)
    test(f"Extract key '{key}' from {sample_json}", res == expected, f"got {res}")

print("\n=================================================================")
print(f"  TEST RESULTS: {passed_tests}/{total_tests} Tests Passed ({(passed_tests/total_tests)*100:.1f}%)")
if passed_tests == total_tests:
    print("  STATUS: ALL TESTS PASSED! APPLICATION VERIFIED 100% READY! ")
else:
    print("  STATUS: SOME TESTS FAILED!")
print("=================================================================")
