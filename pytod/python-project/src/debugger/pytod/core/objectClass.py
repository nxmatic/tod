#! /usr/bin/python
# -*- coding: utf-8 -*-

__author__ = "Milton Inostroza Aguilera"
__email__ = "minoztro@gmail.com"
__all__ = ['Class']

import inspect
import dis
import time
import re
import thread
from Dictionary import Dictionary

class Class(object):

    def __init__(self, aHt, aClassId, aCode, aLnotab):
        self.hT = aHt
        self.staticField = Dictionary(self.hT)
        self.attributes = Dictionary(self.hT)
        self.method = Dictionary(self.hT)
        self.lnotab = aLnotab
        self.code = aCode
        self.name = aCode.co_name
        self.Id = aClassId
        self.SpecialBehaviorId = -1

    def __getId__(self):
        return self.Id

    def __getLnotab__(self):
        return self.lnotab

    def __addMethod__(self, aCode, aLocals):
        for theKey,theValue in aLocals.iteritems():
            if inspect.isfunction(theValue):
                if not (theKey == '__module__'):
                    theId = self.hT.itsId.__get__()
                    self.method.update({theKey:theId})
                    self.hT.itsId.__next__()
    
    def __addSpecialMethod__(self, aFileName):
        if self.method.has_key("%sStaticMethod"%self.name):
            return
        theId = self.hT.itsId.__get__()
        self.method.update({"%sStaticMethod"%self.name:theId})
        self.hT.itsId.__next__()
        self.hT.__registerSpecialMethod__("%sStaticMethod"%self.name,
                                          theId,
                                          self.Id,
                                          aFileName)
        self.SpecialBehaviorId = theId
    
    def __setStaticField__(self,
                           aId,
                           aValue,
                           aFrameLineNo,
                           aCurrentLasti,
                           aParentTimestamp,
                           aDepth):
        theThreadId = self.hT.__getThreadId__(thread.get_ident())
        theCurrentTimestamp = self.hT.__convertTimestamp__(time.time())
        if not self.hT.itsProbe.has_key((aCurrentLasti, self.SpecialBehaviorId)):
            theProbeId = self.hT.__registerProbe__(aCurrentLasti,
                                              self.SpecialBehaviorId,
                                              aFrameLineNo)
        else:
            theProbeId = hT.itsProbe[(aCurrentLasti,aTheSpecialBehaviorId)]
        self.hT.itsPacker.reset()
        self.hT.itsPacker.pack_int(self.hT.itsEvents['set'])
        self.hT.itsPacker.pack_int(self.hT.itsObjects['classAttribute'])
        self.hT.itsPacker.pack_int(aId)
        theDataType = self.hT.__getDataType__(aValue)
        self.hT.itsPacker.pack_int(theDataType)
        thePackValue = self.hT.__packValue__(theDataType, aValue)
        self.hT.itsPacker.pack_int(theProbeId)
        self.hT.itsPacker.pack_hyper(aParentTimestamp)        
        self.hT.itsPacker.pack_int(aDepth)
        self.hT.itsPacker.pack_hyper(theCurrentTimestamp)
        self.hT.itsPacker.pack_int(theThreadId)
        if self.hT.FLAG_DEBUGG:
            print self.hT.itsEvents['set'],
            print self.hT.itsObjects['classAttribute'],
            print Id,
            print theDataType,
            print thePackValue,
            print theProbeId,
            print aParentTimestamp,
            print aCurrentDepth,
            print theCurrentTimestamp,
            print theThreadId
            raw_input()
        try:
            self.hT.itsSocket.sendall(self.hT.itsPacker.get_buffer())
            pass
        except:
            print 'TOD está durmiendo :-(', 'set static field'   
    
    def __register_set_StaticField__(self, 
                                     aLocals, 
                                     aFrameLineNo,
                                     aParentTimestamp,
                                     aDepth,
                                     aFileName):
        theLower = 0
        theUpper = len(self.code.co_code)
        theCode = self.code.co_code   
        while theLower < theUpper:
            theOp = ord(theCode[theLower])
            theNameOp = dis.opname[theOp]      
            theLower = theLower + 1
            if theOp >= dis.HAVE_ARGUMENT:
                theValue = ord(theCode[theLower])
                theValue += ord(theCode[theLower+1])*256
                if theNameOp == 'STORE_NAME':
                    #print self.code.co_names[theValue]
                    #registro el atributo estático
                    theStaticFieldName = self.code.co_names[theValue]                    
                    self.staticField.__updateStaticField__(
                                                    {theStaticFieldName:aLocals[theStaticFieldName]}, 
                                                    self.Id)
                    #creamos un metodo artificial para almacenar
                    #la definición de los atributos de clase
                    self.__addSpecialMethod__(aFileName)
                    #set para el atributo estático
                    if not re.search(self.hT.itsMethodPattern,theStaticFieldName):
                        if not inspect.isfunction(aLocals[theStaticFieldName]):
                            self.__setStaticField__(
                                                self.staticField[theStaticFieldName], 
                                                aLocals[theStaticFieldName],
                                                aFrameLineNo,
                                                theLower,
                                                aParentTimestamp,
                                                aDepth)
                theLower = theLower + 2
    
    def __addStaticField__(self, aLocals):
        self.staticField.__updateStaticField__(aLocals, self.Id)
    
    def __addAttribute__(self, aName, aObjectId):
        self.attributes.__updateAttr__({aName:-1}, aObjectId)       