#! /usr/bin/python
# -*- coding: utf-8 -*-

__author__ = "Milton Inostroza Aguilera"
__email__ = "minoztro@gmail.com"
__all__ = ['Dictionary']

import xdrlib
import inspect
import re

class Dictionary(dict):
    
    def __init__(self, hT):
        self.hT = hT
        dict.__init__(self)

    def __setitem__(self, aKey, aValue):
        dict.__setitem__(self,aKey,aValue)

    def __update__(self, aDictionary, aParentId, aArgument):
        for theKey, theValue in aDictionary.items():
            if theKey in aArgument:
                #variable local ya registrada ya que es un argumento
                self[theKey] = theValue
                return
            #se debe registrar argumento self?
            if not self.has_key(theKey):
                if not theKey == 'self':
                    self[theKey] = theValue
                    self.hT.itsPacker.reset()
                    self.hT.itsPacker.pack_int(self.hT.itsEvents['register'])
                    self.hT.itsPacker.pack_int(self.hT.itsObjects['local'])
                    self.hT.itsPacker.pack_int(theValue)
                    self.hT.itsPacker.pack_int(aParentId)
                    self.hT.itsPacker.pack_string(theKey)
                    if self.hT.FLAG_DEBUGG:
                        print self.hT.itsEvents['register'],
                        print self.hT.itsObjects['local'],
                        print theValue,
                        print aParentId,
                        print theKey
                        raw_input()
                    try:
                        self.hT.itsSocket.sendall(self.hT.itsPacker.get_buffer())
                    except:
                        print 'TOD está durmiendo :-('
                    

    def __updateAttr__(self, aDictionary, aParentId): 
        for theKey, theValue in aDictionary.items():
            #se debe registrar argumento self?
            if not self.has_key(theKey):
                if not theKey == 'self':
                    theId = self.hT.itsId.__get__()
                    self.hT.itsId.__next__()
                    self[theKey] = theId
                    self.hT.itsPacker.reset()
                    self.hT.itsPacker.pack_int(self.hT.itsEvents['register'])
                    self.hT.itsPacker.pack_int(self.hT.itsObjects['attribute'])
                    self.hT.itsPacker.pack_int(theId)
                    self.hT.itsPacker.pack_int(aParentId)
                    self.hT.itsPacker.pack_string(theKey)
                    if self.hT.FLAG_DEBUGG:
                        print self.hT.itsEvents['register'],
                        print self.hT.itsObjects['attribute'],
                        print theId,
                        print aParentId,
                        print theKey 
                        raw_input()                          
                    try:       
                        self.hT.itsSocket.sendall(self.hT.itsPacker.get_buffer())
                    except:
                        print 'TOD está durmiendo :-('

    def __updateStaticField__(self, aDictionary, aParentId): 
        for theKey, theValue in aDictionary.items():
            if not inspect.isfunction(theValue) and not theKey == 'self':
                if not re.search(self.hT.itsMethodPattern,theKey):
                    if not self.has_key(theKey):
                        theValue = self.hT.itsId.__get__()
                        self.hT.itsId.__next__()
                        self[theKey] = theValue
                        self.hT.itsPacker.reset()
                        self.hT.itsPacker.pack_int(
                            self.hT.itsEvents['register'])
                        self.hT.itsPacker.pack_int(
                            self.hT.itsObjects['classAttribute'])
                        self.hT.itsPacker.pack_int(theValue)
                        self.hT.itsPacker.pack_int(aParentId)
                        self.hT.itsPacker.pack_string(theKey)
                        if self.hT.FLAG_DEBUGG:
                            print self.hT.itsEvents['register'],
                            print self.hT.itsObjects['classAttribute'],
                            print theValue,
                            print aParentId,
                            print theKey 
                            raw_input()                          
                        try:       
                            self.hT.itsSocket.sendall(
                                self.hT.itsPacker.get_buffer())
                            pass
                        except:
                            print 'TOD está durmiendo :-('