import socket
import subprocess
import time

adb = r"e:\wafa-ai\platform-tools\adb.exe"

def ensure_server():
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.settimeout(1)
        s.connect(('127.0.0.1', 5037))
        s.close()
    except Exception:
        subprocess.run([adb, "start-server"], shell=True)
        time.sleep(1.5)

ensure_server()

s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
s.connect(('127.0.0.1', 5037))
s.sendall(b"0012host:transport-any")
resp = s.recv(4)
if resp == b"OKAY":
    cmd = "shell:getprop ro.product.model; getprop ro.build.version.release; dumpsys battery | grep level"
    packet = f"{len(cmd):04x}{cmd}".encode('utf-8')
    s.sendall(packet)
    resp2 = s.recv(4)
    if resp2 == b"OKAY":
        out = b""
        s.settimeout(3)
        try:
            while True:
                chunk = s.recv(1024)
                if not chunk: break
                out += chunk
        except Exception:
            pass
        print("CONNECTED_PHONE_DETAILS:\n" + out.decode('utf-8', errors='ignore'))
s.close()
