#! /usr/bin/python
# -*- coding: utf-8 -*-

import sys
from debugger.pytod.core.hunterTrace import hT

print "PyTOD wrapper v1"
if __name__ == '__main__':
    #print sys.argv
    execfile('%s'%(sys.argv[1]),locals(),globals())