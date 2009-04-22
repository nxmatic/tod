import socket
from xdrlib import Unpacker
host = 'localhost'
port = 8000
s = socket.socket(socket.AF_INET,socket.SOCK_STREAM)
s.bind((host,port))
s.listen(5)
conn, addr = s.accept()
print 'conectado por', addr
while 1:
    data = conn.recv(1024)
    un = Unpacker(data)
    if not data: break
    data = un.unpack_int()
    conn.send(str(data))
conn.close()
