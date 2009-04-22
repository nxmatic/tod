#! /usr/bin/python2.5
# -*- coding: utf-8 -*-

import os
import re
import time
from debugger.pytod.core.hunterTrace import *
from threading import *


class testit(Thread):
   def __init__ (self,ip):
      Thread.__init__(self)
      self.ip = ip
      self.status = -1
   def run(self):
      pingaling = os.popen("ping -q -c2 "+self.ip,"r")
      while 1:
        line = pingaling.readline()
        if not line: break
        igot = re.findall(testit.lifeline,line)
        if igot:
           self.status = int(igot[0])

testit.lifeline = re.compile(r"(\d) received")
report = ("No response","Partial Response","Alive")



print time.ctime()

pinglist = []

for host in range(60,70):
   ip = "192.168.200."+str(host)
   current = testit(ip)
   pinglist.append(current)
   current.start()

for pingle in pinglist:
   pingle.join()
   print "Status from ",pingle.ip,"is",report[pingle.status]

print time.ctime()
print 'es el fin'

