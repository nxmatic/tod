import socket
from xdrlib import Packer
host = 'localhost'
port = 8000
s = socket.socket(socket.AF_INET,socket.SOCK_STREAM)
s.connect((host, port))
p = Packer()
p.pack_int(1)
s.send(p.get_buffer())
data = s.recv(1024)
s.close()
print 'recibido', repr(data)
