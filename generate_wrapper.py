import subprocess
import os

gradle_bat = r"e:\wafa-ai\gradle-dist\gradle-8.11.1\bin\gradle.bat"
java_home = r"C:\Program Files\Microsoft\jdk-17.0.20.8-hotspot"

env = os.environ.copy()
env["JAVA_HOME"] = java_home

print("Generating standard Gradle Wrapper files...")
res = subprocess.run([gradle_bat, "wrapper", "--gradle-version", "8.11.1"], cwd=r"e:\wafa-ai", env=env, capture_output=True, text=True)
print("STDOUT:\n", res.stdout)
print("STDERR:\n", res.stderr)
print("Exit code:", res.returncode)
