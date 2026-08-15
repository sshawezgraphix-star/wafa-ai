import socket
import time

s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
s.connect(('127.0.0.1', 5037))
s.sendall(b"0012host:transport-any")
resp = s.recv(4)
print("TRANSPORT_RESP:", resp)
if resp == b"OKAY":
    cmd = "shell:am start -a android.intent.action.VIEW -d http://10.76.134.211:8080"
    packet = f"{len(cmd):04x}{cmd}".encode('utf-8')
    s.sendall(packet)
    resp2 = s.recv(4)
    print("AM_RESP:", resp2)
    s.settimeout(2)
    try:
        out = s.recv(1024)
        print("AM_OUTPUT:", out.decode('utf-8', errors='ignore'))
    except Exception:
        pass
s.close()
