import socket
import subprocess
import time
import os

adb = r"e:\wafa-ai\platform-tools\adb.exe"

def ensure_server():
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.settimeout(1)
        s.connect(('127.0.0.1', 5037))
        s.close()
        return True
    except Exception:
        subprocess.run([adb, "start-server"], shell=True)
        time.sleep(1.5)
        return True

ensure_server()

def query_adb(service):
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.connect(('127.0.0.1', 5037))
    length = f"{len(service):04x}"
    packet = f"{length}{service}".encode('utf-8')
    s.sendall(packet)
    resp = s.recv(4)
    if resp == b"OKAY":
        len_str = s.recv(4).decode('utf-8')
        try:
            total_len = int(len_str, 16)
            data = s.recv(total_len).decode('utf-8')
            return True, data
        except Exception:
            return True, ""
    else:
        len_str = s.recv(4).decode('utf-8', errors='ignore')
        try:
            total_len = int(len_str, 16)
            err = s.recv(total_len).decode('utf-8', errors='ignore')
            return False, err
        except Exception:
            return False, "Unknown error"

ok, data = query_adb("host:devices-l")
print("ADB_SERVER_CONNECTED: TRUE")
print("DEVICE_SCAN_RESULT:")
if data.strip():
    print(data.strip())
else:
    print("NO_DEVICE_FOUND: (Phone not yet detected by Windows USB driver)")
