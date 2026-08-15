import shutil
import os

gradle_home = r"e:\wafa-ai\gradle-dist\gradle-8.11.1"
wrapper_jar_src = os.path.join(gradle_home, "lib", "plugins", "gradle-plugins-8.11.1.jar") # or wrapper lib

# Search for gradle-wrapper.jar in gradle-dist
wrapper_jar = None
for root, dirs, files in os.walk(gradle_home):
    for f in files:
        if f.startswith("gradle-wrapper") and f.endswith(".jar"):
            wrapper_jar = os.path.join(root, f)
            break

os.makedirs(r"e:\wafa-ai\gradle\wrapper", exist_ok=True)
if wrapper_jar:
    shutil.copyfile(wrapper_jar, r"e:\wafa-ai\gradle\wrapper\gradle-wrapper.jar")
    print("Copied gradle-wrapper.jar to gradle/wrapper/")
else:
    print("Wrapper jar not found directly in bin dist, using gradle wrapper")

# Copy gradlew scripts
bin_gradle = os.path.join(gradle_home, "bin", "gradle")
bin_gradle_bat = os.path.join(gradle_home, "bin", "gradle.bat")
if os.path.exists(bin_gradle):
    shutil.copyfile(bin_gradle, r"e:\wafa-ai\gradlew")
if os.path.exists(bin_gradle_bat):
    shutil.copyfile(bin_gradle_bat, r"e:\wafa-ai\gradlew.bat")
print("Copied gradlew and gradlew.bat")
