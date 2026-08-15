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

def open_url_on_phone(url):
    ensure_server()
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.connect(('127.0.0.1', 5037))
    s.sendall(b"0012host:transport-any")
    resp = s.recv(4)
    if resp == b"OKAY":
        cmd = f"shell:am start -a android.intent.action.VIEW -d \"{url}\""
        packet = f"{len(cmd):04x}{cmd}".encode('utf-8')
        s.sendall(packet)
        resp2 = s.recv(4)
        print("AM_START_STATUS:", resp2)
        out = s.recv(1024)
        print("OUTPUT:", out.decode('utf-8', errors='ignore'))
    s.close()

if __name__ == "__main__":
    import sys
    target = sys.argv[1] if len(sys.argv) > 1 else "http://10.76.134.211:8080"
    open_url_on_phone(target)
