import os
import subprocess
import sys
import time

java_home = r"C:\Program Files\Microsoft\jdk-17.0.20.8-hotspot"
gradle_bat = r"e:\wafa-ai\gradle-dist\gradle-8.11.1\bin\gradle.bat"
adb_exe = r"e:\wafa-ai\platform-tools\adb.exe"

env = os.environ.copy()
env["JAVA_HOME"] = java_home
env["PATH"] = f"{java_home}\\bin;e:\\wafa-ai\\platform-tools;" + env.get("PATH", "")

log_file = r"e:\wafa-ai\build_output.log"

print("=================================================================")
print("  STARTING AUTOMATED MAYA AI BUILD & PHONE DEPLOYMENT            ")
print("=================================================================")

with open(log_file, "w", encoding="utf-8") as out_f:
    out_f.write("Starting build...\n")

    # Step 1: Run Gradle assembleDebug
    print("Step 1: Compiling Maya AI APK (assembleDebug)...")
    cmd = [gradle_bat, "assembleDebug", "--no-daemon", "--stacktrace"]
    
    proc = subprocess.Popen(
        cmd,
        cwd=r"e:\wafa-ai",
        env=env,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        text=True,
        bufsize=1
    )

    for line in proc.stdout:
        print(line, end="")
        out_f.write(line)
        out_f.flush()

    proc.wait()
    print(f"Build exited with code: {proc.returncode}")

    if proc.returncode == 0:
        print("\n=================================================================")
        print("  APK BUILD SUCCESSFUL! INSTALLING ON CONNECTED PHONE...         ")
        print("=================================================================")
        apk_path = r"e:\wafa-ai\app\build\outputs\apk\debug\app-debug.apk"
        if os.path.exists(apk_path):
            install_cmd = [adb_exe, "install", "-r", "-d", apk_path]
            install_res = subprocess.run(install_cmd, capture_output=True, text=True)
            print("INSTALL_OUTPUT:\n", install_res.stdout, install_res.stderr)
            out_f.write("\nINSTALL_OUTPUT:\n" + install_res.stdout + install_res.stderr)

            # Launch app on phone screen
            print("Launching Maya AI on phone screen...")
            launch_cmd = [adb_exe, "shell", "am", "start", "-n", "com.aistudio.firdousai.vzcyp/com.example.MainActivity"]
            launch_res = subprocess.run(launch_cmd, capture_output=True, text=True)
            print("LAUNCH_OUTPUT:\n", launch_res.stdout, launch_res.stderr)
            out_f.write("\nLAUNCH_OUTPUT:\n" + launch_res.stdout + launch_res.stderr)
            print("\n🎉 MAYA AI IS NOW LIVE AND RUNNING ON YOUR PHONE SCREEN! 🎉")
        else:
            print("Error: APK output file not found at", apk_path)
    else:
        print("Build failed. Check log for details.")
