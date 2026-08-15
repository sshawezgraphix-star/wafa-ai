import os
import sys
import socket

adb = r"e:\wafa-ai\platform-tools\adb.exe"

def install_apk_on_device(apk_path):
    if not os.path.exists(apk_path):
        print(f"Error: APK file not found at {apk_path}")
        return False

    print(f"Installing {apk_path} on connected phone (CPH2579 - Android 15)...")
    # Query adb install
    cmd = f'"{adb}" install -r -d "{apk_path}"'
    res = os.system(cmd)
    if res == 0:
        print("SUCCESS! Maya AI installed on your phone! 🚀")
        # Launch app on phone
        print("Launching Maya AI on phone screen...")
        os.system(f'"{adb}" shell am start -n com.aistudio.firdousai.vzcyp/com.example.MainActivity')
        return True
    else:
        print("Installation command returned code:", res)
        return False

if __name__ == "__main__":
    if len(sys.argv) > 1:
        install_apk_on_device(sys.argv[1])
    else:
        default_apk = r"e:\wafa-ai\app\build\outputs\apk\debug\app-debug.apk"
        install_apk_on_device(default_apk)
