#! /usr/bin/python
# -*- coding: utf-8 -*-

__author__ = "Milton Inostroza Aguilera"
__email__ = "minoztro@gmail.com"
__all__ = ['hT']
th = True
import sys
import dis
import re
import types
import time
import thread
import xdrlib
import socket
import inspect
from constantsObjects import events, objects, dataTypes, packXDRLib
from generatorId import generatorId
from objectClass import Class
from objectMethod import Method
from objectFunction import Function
if th:
    from threading import settrace   


class hunterTrace(object):

    def __init__(self, aId, aProbeId, aThreadId, aPacker, aHost, aPort):
        self.itsClass = {}
        self.itsFunction = {}
        self.itsMethod = {}
        self.itsProbe = {}
        self.itsThread = {}
        self.itsSocket = None
        self.itsEvents = events
        self.itsObjects = objects
        self.itsDataTypes = dataTypes
        self.itsPackXDR = packXDRLib
        self.itsId = aId
        self.itsProbeId = aProbeId
        self.itsThreadId = aThreadId
        self.itsPacker = aPacker
        self.itsHost = aHost
        self.itsPort = aPort
        self.FLAG_DEBUGG = False
        self.FLAG_THROWN = False
        self.itsMethodPattern = "\A__.*(__)$"
        #trick for MetaDescriptor's depth
        self.itsCurrentDepth = 0
        #trick for Objects {strings, tuple, dict, list}
        self.itsRegisterObjects = []
        self.__socketConnect__()

    def __addClass__(self, aId, aLnotab, aCode):
        objectClass = Class(self,aId,aCode,aLnotab)
        self.itsClass.update({aCode:objectClass})
        return objectClass

    def __addFunction__(self, aId, aLnotab, aCode, aArgs):
        self.itsFunction.update({aCode:Function(self,aId,aCode,aLnotab,aArgs)})

    def __addMethod__(self, aId, aLnotab, aCode, idClass, aArgs):
        self.itsMethod.update({aCode:Method(self,aId,aCode,aLnotab,idClass,aArgs)})

    def __addProbe__(self, aProbeId, currentLasti, parentId):
        self.itsProbe.update({(currentLasti,parentId):aProbeId})

    def __addThread__(self, aThreadId, aThreadSysId):
        self.itsThread.update({aThreadSysId:aThreadId})
                
    def __behaviorExit__(self,
                         aFrame,
                         aArg,
                         aDepth,
                         aParentTimestampFrame,
                         aThreadId,
                         aHasThrown):
        theBackFrame = aFrame.f_back
        theBackFrameCode = theBackFrame.f_code
        theParentId = self.__getObjectId__(theBackFrameCode)
        behaviorId = self.__getObjectId__(aFrame.f_code)
        theCurrentLasti = aFrame.f_lasti
        theDepth = aDepth + 1
        if not self.itsProbe.has_key((theCurrentLasti,theParentId)):
            theProbeId = self.__registerProbe__(theCurrentLasti,
                                                theParentId,
                                                aFrame.f_lineno)
        else:
            theProbeId = self.itsProbe[(theCurrentLasti,theParentId)]
        theCurrentTimestamp = self.__convertTimestamp__(time.time())
        if aHasThrown:                  
            self.__registerObject__(aArg,theCurrentTimestamp)
            self.itsPacker.reset()
            self.itsPacker.pack_int(self.itsEvents['return'])
            self.itsPacker.pack_int(behaviorId)
            theDataType = self.__getDataType__(aArg)
            self.itsPacker.pack_int(theDataType)
            self.itsPacker.pack_int(id(aArg))
            self.itsPacker.pack_int(1) #True
            self.itsPacker.pack_int(theProbeId)
            self.itsPacker.pack_hyper(aParentTimestampFrame)        
            self.itsPacker.pack_int(theDepth)
            self.itsPacker.pack_hyper(theCurrentTimestamp)
            self.itsPacker.pack_int(aThreadId)
        else:
            theDataType = self.__getDataType__(aArg)
            if theDataType == 1:
                if not id(aArg) in self.itsObjects:
                    self.__registerObject__(aArg,theCurrentTimestamp)
            self.itsPacker.reset()
            self.itsPacker.pack_int(self.itsEvents['return'])
            self.itsPacker.pack_int(behaviorId)
            self.itsPacker.pack_int(theDataType)
            if theDataType == 1:
                thePackValue = id(aArg)
                self.itsPacker.pack_int(id(aArg))
            else:
                thePackValue = self.__packValue__(theDataType, aArg)            
            self.itsPacker.pack_int(0) #False
            self.itsPacker.pack_int(theProbeId)
            self.itsPacker.pack_hyper(aParentTimestampFrame)        
            self.itsPacker.pack_int(theDepth)
            self.itsPacker.pack_hyper(theCurrentTimestamp)
            self.itsPacker.pack_int(aThreadId)
        if self.FLAG_DEBUGG:
            print self.itsEvents['return'],
            print behaviorId,
            print theDataType,
            print thePackValue,
            print aHasThrown,
            print theProbeId,
            print aParentTimestampFrame,
            print theDepth,
            print theCurrentTimestamp,
            print aThreadId
            raw_input()
        try:
            self.itsSocket.sendall(self.itsPacker.get_buffer())
        except:
            print 'TOD está durmiendo :-( - Salida de behavior', aFrame.f_code.co_name
        
    def __createlnotab__(self, aCode):
        theLnotab = {}
        if hasattr(aCode, 'co_lnotab'):
            table = aCode.co_lnotab
            index = 0
            last_index = None
            for i in range(0, len(table), 2):
                index = index + ord(table[i])
                if last_index == None:
                    last_index = index
                else:
                    theLnotab.update({index:tuple([last_index,index-1])})                
                    last_index = index
            theLnotab.update({len(aCode.co_code)-1:tuple([last_index,len(aCode.co_code)-1])})                
        return theLnotab        

    def __convertTimestamp__(self,aTimestamp):
        #the timestamp is converted to long
        return long(aTimestamp*1000000000)

    def __depthFrame__(self, aFrame):
        theBackFrame = aFrame.f_back
        if theBackFrame.f_locals.has_key('__depth__'):
            theCurrentDepth = theBackFrame.f_locals['__depth__']
            aFrame.f_locals['__depth__'] = theCurrentDepth + 1
        else:
            aFrame.f_locals['__depth__'] = 1
        return aFrame.f_locals['__depth__']
    
    def __functionCall__(self, 
                         aCode, 
                         aFrame,
                         aDepth,
                         aCurrentTimestamp,
                         aParentTimestampFrame,
                         aThreadId):
        theObject = self.__getObject__(aCode)
        theFunctionId = theObject.__getId__()
        theArgsValue = theObject.__getArgsValues__(aFrame.f_locals)
        theBackFrame = aFrame.f_back
        theBackFrameLasti = theBackFrame.f_lasti
        theBackFrameCode = theBackFrame.f_code
        theParentId = self.__getObjectId__(theBackFrameCode)
        theCurrentLasti = aFrame.f_lasti
        if not self.itsProbe.has_key((theCurrentLasti,theParentId)):
            theProbeId = self.__registerProbe__(theCurrentLasti,
                                                theParentId,
                                                aFrame.f_lineno)
        else:
            theProbeId = self.itsProbe[(theCurrentLasti,theParentId)]
        #preguntamos si algún argumento es del tipo string
        #y lo registramos debidamente
        for theValue in theArgsValue:
            if type(theValue) == types.StringType:
                if not id(theValue) in hT.itsRegisterObjects:
                    self.__registerObject__(theValue, aCurrentTimestamp)             
        self.itsPacker.reset()
        self.itsPacker.pack_int(self.itsEvents['call'])
        self.itsPacker.pack_int(self.itsObjects['function'])
        self.itsPacker.pack_int(theFunctionId)
        self.itsPacker.pack_int(len(theArgsValue))
        thePrintArg = " "
        for theValue in theArgsValue:
            theDataType = self.__getDataType__(theValue)
            self.itsPacker.pack_int(theDataType)
            thePrintArg += str(theDataType)
            thePrintArg += " "
            if theDataType == 1:
                self.itsPacker.pack_hyper(id(theValue))
                thePrintArg += str(id(theValue))
                thePrintArg += " "
            else:
                thePrintArg += str(self.__packValue__(theDataType, theValue))
                thePrintArg += " "            
        self.itsPacker.pack_int(theProbeId)
        self.itsPacker.pack_hyper(aParentTimestampFrame)        
        self.itsPacker.pack_int(aDepth)
        self.itsPacker.pack_hyper(aCurrentTimestamp)
        self.itsPacker.pack_int(aThreadId)
        if self.FLAG_DEBUGG:
            print self.itsEvents['call'],
            print self.itsObjects['function'],
            print theFunctionId,
            print len(theArgsValue),
            print thePrintArg,
            print theProbeId,
            print aParentTimestampFrame,
            print aDepth,
            print aCurrentTimestamp,
            print aThreadId
            raw_input()
        try:
            self.itsSocket.sendall(self.itsPacker.get_buffer())
        except:
            print 'TOD está durmiendo :-( - Llamado a funcion', aCode.co_name        
            
    def __getArgs__(self, aCode):
        return aCode.co_varnames[:aCode.co_argcount]

    def __getClassKey__(self, aNameClass):
        for theKey, theValue in self.itsClass.iteritems():
            if theKey.co_name == aNameClass:
                return theKey
        return None
    
    def __getObjectId__(self, aCode):
        if self.__isClassKey__(aCode):
            return self.itsClass[aCode].__getId__()
        elif self.__isFunctionKey__(aCode):
            return self.itsFunction[aCode].__getId__()
        elif self.__isMethodKey__(aCode):
            return self.itsMethod[aCode].__getId__()
        return -1

    def __getObject__(self, aCode):
        if self.__isFunctionKey__(aCode):
            return self.itsFunction[aCode]
        elif self.__isMethodKey__(aCode):
            return self.itsMethod[aCode]
        return None

    def __getThreadId__(self, aThreadSysId):
        if not hT.itsThread.has_key(aThreadSysId):
            theThreadId = self.__registerThread__(aThreadSysId)
        else:
            theThreadId = self.itsThread[aThreadSysId]
        return theThreadId

    def __getpartcode__(self, aCode, aLimits):
        theLower = aLimits[0]
        theUpper = aLimits[1]
        theCode = aCode.co_code
        theStoreFast = {}    
        while theLower < theUpper:
            theOp = ord(theCode[theLower])
            theNameOp = dis.opname[theOp]
            theLower = theLower + 1
            if theOp >= dis.HAVE_ARGUMENT:
                theValue = ord(theCode[theLower]) + ord(theCode[theLower+1])*256
                theLower = theLower + 2
                if theOp in dis.haslocal and theNameOp == 'STORE_FAST':
                    theArgumentValue = aCode.co_varnames[theValue]
                    theStoreFast.update({theArgumentValue:theValue})
        return theStoreFast

    def __getDepthFrame__(self, aFrame):
        try:
            return aFrame.f_locals['__depth__']
        except:
            return -1
    
    def __getDataType__(self, aValue):
        theDataType = 8
        try:
            if self.itsDataTypes.has_key(aValue.__class__.__name__):
                theDataType = self.itsDataTypes[aValue.__class__.__name__]
        except:
            return theDataType
        finally:
            return theDataType

    def __getTimestampFrame__(self, aFrame):
        if aFrame.f_locals.has_key('__timestampFrame__'):
            return aFrame.f_locals['__timestampFrame__']
        return 0

    def __getTimestampParentFrame__(self, aFrame):
        theBackFrame = aFrame.f_back 
        if theBackFrame.f_locals.has_key('__timestampFrame__'):
            return theBackFrame.f_locals['__timestampFrame__']
        return 0

    def __inClass__(self, aClass):
        if self.itsClass.has_key(aClass):
            return True
        return False

    def __inFunction__(self, aFunction):
        if self.itsFunction.has_key(aFunction):
            return True
        return False

    def __inMethod__(self, aMethod):
        if self.itsMethod.has_key(aMethod):
            return True
        return False

    def __isClassKey__(self, aClassCode):
        for theKey in self.itsClass.iterkeys():
            if theKey == aClassCode:
                return self.itsClass[theKey]
        return None

    def __isFunctionKey__(self, aFunctionCode):
        for theKey in self.itsFunction.iterkeys():
            if theKey == aFunctionCode:
                return self.itsFunction[theKey]
        return None

    def __isMethodKey__(self, aMethodCode):
        for theKey in self.itsMethod.iterkeys():
            if theKey == aMethodCode:
                return self.itsMethod[theKey]
        return None

    def __instantiation__(self, 
                          aCode, 
                          aFrame, 
                          aInstantiationId, 
                          aDepth, 
                          aCurrentTimestamp, 
                          aParentTimestampFrame, 
                          aThreadId):
        theBehavior = self.__getObject__(aCode)
        theBehaviorId = theBehavior.__getId__()
        theClassId = theBehavior.__getTarget__()
        theArgsValue = theBehavior.__getArgsValues__(aFrame.f_locals)
        theBackFrame = aFrame.f_back
        theFrameLasti = theBackFrame.f_lasti
        theBackFrameCode = theBackFrame.f_code
        theParentId = self.__getObjectId__(theBackFrameCode)
        theCurrentLasti = aFrame.f_lasti        
        if not self.itsProbe.has_key((theCurrentLasti,theParentId)):
            theProbeId = self.__registerProbe__(theCurrentLasti,
                                                theParentId,
                                                aFrame.f_lineno)
        else:
            theProbeId = self.itsProbe[(theCurrentLasti,theParentId)]
        #preguntamos si algún argumento es del tipo string
        #y lo registramos debidamente
        for theValue in theArgsValue:
            if type(theValue) == types.StringType:
                if not id(theValue) in hT.itsRegisterObjects:
                    self.__registerObject__(theValue, aCurrentTimestamp)
        self.itsPacker.reset()       
        self.itsPacker.pack_int(self.itsEvents['instantiation'])
        self.itsPacker.pack_int(theBehaviorId)
        self.itsPacker.pack_int(aInstantiationId)
        self.itsPacker.pack_int(len(theArgsValue))
        thePrintArg = " "
        for theValue in theArgsValue:
            theDataType = self.__getDataType__(theValue)               
            self.itsPacker.pack_int(theDataType)
            thePrintArg += str(theDataType)
            thePrintArg += " "
            if theDataType == 1:
                self.itsPacker.pack_hyper(id(theValue))
                thePrintArg += str(id(theValue))
                thePrintArg += " "
            else:
                thePrintArg += str(self.__packValue__(theDataType, theValue))
                thePrintArg += " "
        self.itsPacker.pack_int(theProbeId)
        self.itsPacker.pack_hyper(aParentTimestampFrame)
        self.itsPacker.pack_int(aDepth)    
        self.itsPacker.pack_hyper(aCurrentTimestamp)
        self.itsPacker.pack_int(aThreadId)
        if self.FLAG_DEBUGG:
            print self.itsEvents['instantiation'],
            print aInstantiationId,
            print len(theArgsValue), 
            print thePrintArg,        
            print theProbeId,
            print aParentTimestampFrame,
            print aDepth,
            print aCurrentTimestamp,
            print aThreadId
            raw_input()
        try:
            self.itsSocket.sendall(self.itsPacker.get_buffer())
        except:
            print 'TOD está durmiendo :-( - Instanciación', aCode.co_name
    
    def __localWrite__(self,
                       aCode,
                       aBytecodeLocal,
                       aLocals,
                       aObject,
                       aCurrentLasti,
                       aCurrentLineno,
                       aDepth,
                       aParentTimestampFrame, 
                       aThreadId):
        theLocalVariables = aObject.__getLocals__()
        theBehaviorId = self.__getObjectId__(aCode)
        theDepth = aDepth + 1
        for theValue in aBytecodeLocal.iterkeys():
            if not theLocalVariables.has_key(theValue) or \
               not aLocals.has_key(theValue):
                return
            if not self.itsProbe.has_key((aCurrentLasti,theBehaviorId)):
                theProbeId = self.__registerProbe__(aCurrentLasti,
                                                    theBehaviorId,
                                                    aCurrentLineno)
            else:
                theProbeId = self.itsProbe[(aCurrentLasti,theBehaviorId)]
            theCurrentTimestamp = self.__convertTimestamp__(time.time())
            if type(aLocals[theValue]) == types.StringType:
                if not id(aLocals[theValue]) in hT.itsRegisterObjects:
                    hT.__registerObject__(aLocals[theValue], theCurrentTimestamp)
                    self.itsPacker.reset()
                    self.itsPacker.pack_int(self.itsEvents['set'])
                    self.itsPacker.pack_int(self.itsObjects['local'])
                    self.itsPacker.pack_int(theLocalVariables[theValue])
                    self.itsPacker.pack_int(theBehaviorId)
                    theDataType = self.__getDataType__(aLocals[theValue])
                    self.itsPacker.pack_int(theDataType)
                    #thePackValue = self.__packValue__(theDataType, aLocals[theValue])
                    self.itsPacker.pack_hyper(id(aLocals[theValue]))
                    self.itsPacker.pack_int(theProbeId)
                    self.itsPacker.pack_hyper(aParentTimestampFrame)
                    self.itsPacker.pack_int(theDepth) 
                    self.itsPacker.pack_hyper(theCurrentTimestamp)
                    self.itsPacker.pack_int(aThreadId)
                    if self.FLAG_DEBUGG:            
                        print self.itsEvents['set'],
                        print self.itsObjects['local'],
                        print theLocalVariables[theValue],
                        print theBehaviorId,
                        print theDataType,
                        print thePackValue,
                        print theProbeId,
                        print aParentTimestampFrame,
                        print theDepth,
                        print theCurrentTimestamp,
                        print aThreadId
                        raw_input()
                    try:
                        self.itsSocket.sendall(self.itsPacker.get_buffer())
                    except:
                        print 'TOD está durmiendo :-( - Local write', aCode.co_name                   
            else:    
                self.itsPacker.reset()
                self.itsPacker.pack_int(self.itsEvents['set'])
                self.itsPacker.pack_int(self.itsObjects['local'])
                self.itsPacker.pack_int(theLocalVariables[theValue])
                self.itsPacker.pack_int(theBehaviorId)
                theDataType = self.__getDataType__(aLocals[theValue])
                self.itsPacker.pack_int(theDataType)
                thePackValue = self.__packValue__(theDataType, aLocals[theValue])
                self.itsPacker.pack_int(theProbeId)
                self.itsPacker.pack_hyper(aParentTimestampFrame)
                self.itsPacker.pack_int(theDepth) 
                self.itsPacker.pack_hyper(theCurrentTimestamp)
                self.itsPacker.pack_int(aThreadId)
                if self.FLAG_DEBUGG:            
                    print self.itsEvents['set'],
                    print self.itsObjects['local'],
                    print theLocalVariables[theValue],
                    print theBehaviorId,
                    print theDataType,
                    print thePackValue,
                    print theProbeId,
                    print aParentTimestampFrame,
                    print theDepth,
                    print theCurrentTimestamp,
                    print aThreadId
                    raw_input()
                try:
                    self.itsSocket.sendall(self.itsPacker.get_buffer())
                except:
                    print 'TOD está durmiendo :-( - Local write', aCode.co_name            
    
    def __markTimestampFrame__(self, aFrame):
        if not aFrame.f_locals.has_key('__timestampFrame__'): 
            aFrame.f_locals['__timestampFrame__'] = self.__convertTimestamp__(
                                                                  time.time())
        return

    def __methodCall__(self,
                       aCode,
                       aFrame, 
                       aTargetId,
                       aDepth,
                       aCurrentTimestamp,
                       aParentTimestampFrame,
                       aThreadId):
        theObject = self.__getObject__(aCode)
        theMethodId = theObject.__getId__()
        #classId = theObject.__getTarget__()
        theArgsValue = theObject.__getArgsValues__(aFrame.f_locals)
        theBackFrame = aFrame.f_back
        theBackFrameLasti = theBackFrame.f_lasti
        theBackFrameCode = theBackFrame.f_code
        theParentId = self.__getObjectId__(theBackFrameCode)
        theCurrentLasti = aFrame.f_lasti        
        if not self.itsProbe.has_key((theCurrentLasti,theParentId)):
            theProbeId = self.__registerProbe__(theCurrentLasti,
                                                theParentId,
                                                aFrame.f_lineno)
        else:
            theProbeId = self.itsProbe[(theCurrentLasti,theParentId)]
        #preguntamos si algún argumento es del tipo string
        #y lo registramos debidamente
        for theValue in theArgsValue:
            if type(theValue) == types.StringType:
                if not id(theValue) in hT.itsRegisterObjects:
                    self.__registerObject__(theValue, aCurrentTimestamp)            
        self.itsPacker.reset()
        self.itsPacker.pack_int(self.itsEvents['call'])
        self.itsPacker.pack_int(self.itsObjects['method'])
        self.itsPacker.pack_int(theMethodId)
        self.itsPacker.pack_int(aTargetId)
        self.itsPacker.pack_int(len(theArgsValue))
        thePrintArg = " "
        for theValue in theArgsValue:
            theDataType = self.__getDataType__(theValue)
            self.itsPacker.pack_int(theDataType)
            thePrintArg += str(theDataType)
            thePrintArg += " "
            if theDataType == 1:
                self.itsPacker.pack_hyper(id(theValue))
                thePrintArg += str(id(theValue))
                thePrintArg += " "
            else:
                thePrintArg += str(self.__packValue__(theDataType, theValue))
                thePrintArg += " "
        self.itsPacker.pack_int(theProbeId)
        self.itsPacker.pack_hyper(aParentTimestampFrame)
        self.itsPacker.pack_int(aDepth)    
        self.itsPacker.pack_hyper(aCurrentTimestamp)
        self.itsPacker.pack_int(aThreadId)
        if self.FLAG_DEBUGG:
            print self.itsEvents['call'],
            print self.itsObjects['method'],
            print theMethodId,
            print aTargetId,
            print len(theArgsValue),
            print thePrintArg,
            print theProbeId,
            print aParentTimestampFrame,
            print aDepth,
            print aCurrentTimestamp,
            print aThreadId
            raw_input()
        try:
            self.itsSocket.sendall(self.itsPacker.get_buffer())
        except:
            print 'TOD está durmiendo :-( - Call metodo', aCode.co_name
    
    
    def __packValue__(self, aDataType, aValue):
        if self.itsPackXDR.has_key(aDataType):
            theMethodName = self.itsPackXDR[aDataType]
            #bool no funciona bien con XDRLib entre java y python
            if aDataType == 4:
                if aValue:
                    aValue = 1
                else:
                    aValue = 0
            #trick for string objects (it's send the Id)
            if aDataType == 1:
                 self.itsPacker.pack_hyper(id(aValue))
                 return id(aValue)
            getattr(self.itsPacker,'pack_%s'%theMethodName)(aValue)
            return aValue            
        else:
            #en estos momentos envíamos el tipo de dato
            #TODO: debieramos envíar el id del objeto
            self.itsPacker.pack_int(aDataType)
            return aDataType

    def __printHunter__(self):
        #cerrar socket
        #TODO: encontrar una manera mejor de hacer esto
        self.itsSocket.close()
        print
        print 'clases'
        for theKey, theValue in hT.itsClass.iteritems():
            print theValue.__dict__
            print
        print '======='
        
        print 'metodos'
        for theKey, theValue in hT.itsMethod.iteritems():
            print v.__dict__
            print
        print '======='
        
        print 'funcion'
        for theKey, theValue in hT.itsFunction.iteritems():
            print theValue.__dict__
            print
        print '======='
        
    def __socketConnect__(self):
        self.itsSocket = socket.socket(socket.AF_INET,socket.SOCK_STREAM)
        try:
            self.itsSocket.connect((self.itsHost, self.itsPort))
        except:
            print "TOD, esta durmiendo :("
    
    def __register__(self, aObject, aLocals):
        aObject.__registerLocals__(aLocals)

    def __registerClass__(self, aCode, aLocals, aParentTimestamp, aDepth, aFrameLineNo):
        theClassId = self.itsId.__get__()
        theClassName = aCode.co_name
        #HINT: ver como recuperar las herencias de esta clase 
        theClassBases = None
        self.itsPacker.reset()
        self.itsPacker.pack_int(self.itsEvents['register'])
        self.itsPacker.pack_int(self.itsObjects['class'])
        self.itsPacker.pack_int(theClassId)
        self.itsPacker.pack_string(theClassName)
        self.itsPacker.pack_int(0)
        if self.FLAG_DEBUGG:
            print self.itsEvents['register'],
            print self.itsObjects['class'],
            print theClassId,
            print theClassName,
            print theClassBases
            raw_input()
        try:
            self.itsSocket.sendall(self.itsPacker.get_buffer())
        except:
            print 'TOD está durmiendo :-( - Registrando Clase', aCode.co_name
        theObjectClass = self.__addClass__(
                                           theClassId,
                                           self.__createlnotab__(aCode),
                                           aCode)
        self.itsId.__next__()
        theObjectClass.__addMethod__(aCode,aLocals)
        theObjectClass.__register_set_StaticField__(aLocals,
                                                    aFrameLineNo,
                                                    aParentTimestamp,
                                                    aDepth,
                                                    aCode.co_filename)

    def __registerException__(self,
                              aFrame,
                              aArg,
                              aDepth,
                              aParentTimestampFrame,
                              aThreadId):
        #theBackFrame = aFrame.f_back
        #theBackFrameCode = theBackFrame.f_code
        #print aFrame.f_code.co_name, theBackFrameCode.co_name
        #raw_input()
        #theParentId = self.__getObjectId__(theBackFrameCode)
        theParentId = self.__getObjectId__(aFrame.f_code)
        #behaviorId = self.__getObjectId__(aFrame.f_code)
        #theParentId = self.__getObjectId__(aFrame.f_code)
        theCurrentLasti = aFrame.f_lasti
        theDepth = aDepth + 1
        if not self.itsProbe.has_key((theCurrentLasti,theParentId)):
            theProbeId = self.__registerProbe__(theCurrentLasti,
                                                theParentId,
                                                aFrame.f_lineno)
        else:
            theProbeId = self.itsProbe[(theCurrentLasti,theParentId)]
        theCurrentTimestamp = self.__convertTimestamp__(time.time())       
        if not id(aArg) in self.itsObjects:
            self.__registerObject__(aArg,theCurrentTimestamp)            
        self.itsPacker.reset()
        self.itsPacker.pack_int(self.itsEvents['register'])
        self.itsPacker.pack_int(self.itsObjects['exception'])       
        theDataType = self.__getDataType__(aArg)
        self.itsPacker.pack_int(theDataType)
        self.itsPacker.pack_hyper(id(aArg))
        self.itsPacker.pack_int(theProbeId)
        self.itsPacker.pack_hyper(aParentTimestampFrame)        
        self.itsPacker.pack_int(theDepth)
        self.itsPacker.pack_hyper(theCurrentTimestamp)
        self.itsPacker.pack_int(aThreadId)
        if self.FLAG_DEBUGG:
            print self.itsEvents['register'],
            print self.itsObjects['exception'],
            print theDataType,
            print id(aArg),
            print theProbeId,
            print aParentTimestampFrame,
            print theDepth,
            print theCurrentTimestamp,
            print aThreadId
            raw_input()
        try:
            self.itsSocket.sendall(self.itsPacker.get_buffer())
        except:
            print 'TOD está durmiendo :-( - Salida de behavior', aFrame.f_code.co_name

    def __registerFunction__(self, aCode):
        theFunctionId = self.itsId.__get__()
        aArgs = self.__getArgs__(aCode)
        theLineNumbers = 0
        self.itsPacker.reset()
        self.itsPacker.pack_int(self.itsEvents['register'])
        self.itsPacker.pack_int(self.itsObjects['function'])
        self.itsPacker.pack_int(theFunctionId)
        self.itsPacker.pack_string(aCode.co_name)
        self.itsPacker.pack_int(len(aArgs))
        thePrintArg = " " 
        for theValue in range(len(aArgs)):
            if not aArgs[theValue] == 'self':
                thePrintArg += str(aArgs[theValue])
                thePrintArg += " "
                self.itsPacker.pack_string(aArgs[theValue])
                thePrintArg += str(theValue)
                thePrintArg += " "
                self.itsPacker.pack_int(theValue)
        self.itsPacker.pack_string(aCode.co_filename)
        #agregamos setup del método para que el plugin
        #funcione correctamente
        #de seguro que esto se puede hacer mejor
        for theTuple in dis.findlinestarts(aCode):
            theLineNumbers += 1
        self.itsPacker.pack_int(len(aCode.co_code))        
        self.itsPacker.pack_int(theLineNumbers)
        thePrintLines = " "
        for theStartPc, theLineNumber in dis.findlinestarts(aCode):
            thePrintArg += str(theStartPc)
            thePrintArg += " "            
            self.itsPacker.pack_int(theStartPc)
            thePrintArg += str(theLineNumber)
            thePrintArg += " "            
            self.itsPacker.pack_int(theLineNumber)
        if self.FLAG_DEBUGG:
        #if True:
            print self.itsEvents['register'],
            print self.itsObjects['function'],
            print theFunctionId,
            print aCode.co_name,
            print len(aArgs)-1,
            print thePrintArg,
            print aCode.co_filename
            print len(aCode.co_code)
            print theLineNumbers
            print thePrintLines
            raw_input()
        try:
            self.itsSocket.sendall(self.itsPacker.get_buffer())
        except:
            print 'TOD está durmiendo :-( - Registrando Funcion', aCode.co_name       
        self.__addFunction__(
                             theFunctionId,
                             self.__createlnotab__(aCode),
                             aCode,
                             aArgs)
        self.itsId.__next__()


    def __registerMethod__(self, aCode, aMethodId, aClassId, aArgs):
        theLineNumbers = 0
        self.itsPacker.reset()
        self.itsPacker.pack_int(self.itsEvents['register'])
        self.itsPacker.pack_int(self.itsObjects['method'])
        self.itsPacker.pack_int(aMethodId)
        self.itsPacker.pack_int(aClassId)
        self.itsPacker.pack_string(aCode.co_name)
        #argumento viene con self, se le debe restar uno a la cantidad de
        #elementos
        self.itsPacker.pack_int(len(aArgs)-1)
        thePrintArg = " "
        for theValue in range(len(aArgs)):
            if not aArgs[theValue] == 'self':
                thePrintArg += str(aArgs[theValue])
                thePrintArg += " "
                self.itsPacker.pack_string(aArgs[theValue])
                thePrintArg += str(theValue)
                thePrintArg += " "
                self.itsPacker.pack_int(theValue)
        self.itsPacker.pack_string(aCode.co_filename)
        #agregamos setup del método para que el plugin
        #funcione correctamente
        #de seguro que esto se puede hacer mejor
        for theTuple in dis.findlinestarts(aCode):
            theLineNumbers += 1
        self.itsPacker.pack_int(len(aCode.co_code))        
        self.itsPacker.pack_int(theLineNumbers)
        thePrintLines = " "
        for theStartPc, theLineNumber in dis.findlinestarts(aCode):
            thePrintArg += str(theStartPc)
            thePrintArg += " "            
            self.itsPacker.pack_int(theStartPc)
            thePrintArg += str(theLineNumber)
            thePrintArg += " "            
            self.itsPacker.pack_int(theLineNumber)
        if self.FLAG_DEBUGG:
            print self.itsEvents['register'],
            print self.itsObjects['method'],
            print aMethodId,
            print aClassId,
            print aCode.co_name,
            print len(aArgs)-1,
            print thePrintArg,
            print aCode.co_filename
            print len(aCode.co_code)
            print theLineNumbers
            print thePrintLines
            raw_input()
        try:
            self.itsSocket.sendall(self.itsPacker.get_buffer())
        except:
            print 'TOD está durmiendo :-( Registrando Metodo', aCode.co_name
        self.__addMethod__(
                           aMethodId,
                           self.__createlnotab__(aCode),
                           aCode,
                           aClassId,
                           aArgs)
        
        
    def __registerSpecialMethod__(self, aName, aMethodId, aClassId, aFileName):
        self.itsPacker.reset()
        self.itsPacker.pack_int(self.itsEvents['register'])
        self.itsPacker.pack_int(self.itsObjects['specialMethod'])
        self.itsPacker.pack_int(aMethodId)
        self.itsPacker.pack_int(aClassId)
        self.itsPacker.pack_string(aName)
        self.itsPacker.pack_string(aFileName)
        #agregamos setup del método para que el plugin
        #funcione correctamente
        #de seguro que esto se puede hacer mejor
        if self.FLAG_DEBUGG:
            print self.itsEvents['register'],
            print self.itsObjects['specialMethod'],
            print aMethodId,
            print aClassId,
            print aName,
            print aFileName
            raw_input()
        try:
            self.itsSocket.sendall(self.itsPacker.get_buffer())
        except:
            print 'TOD está durmiendo :-( Registrando Metodo', aName
        """
        self.__addMethod__(
                           aMethodId,
                           self.__createlnotab__(aCode),
                           aCode,
                           aClassId,
                           aArgs)
        """
        
    def __registerObject__(self, aValue, aCurrentTimestamp):
        self.itsRegisterObjects.append(id(aValue))
        self.itsPacker.reset()
        self.itsPacker.pack_int(self.itsEvents['register'])
        self.itsPacker.pack_int(self.itsObjects['object'])
        #enviar el tipo de dato por mientras solo es string
        self.itsPacker.pack_int(1) #string
        self.itsPacker.pack_hyper(id(aValue))
        self.itsPacker.pack_string(aValue)
        self.itsPacker.pack_hyper(aCurrentTimestamp)
        if self.FLAG_DEBUGG:
            print self.itsEvents['register'],
            print self.itsObjects['object'],
            print 1,
            print id(aValue),
            print aValue,
            print aCurrentTimestamp
            raw_input()
        try:
            self.itsSocket.sendall(self.itsPacker.get_buffer())
        except:
            print 'TOD está durmiendo :-( - Registrando Probe'
    
    def __registerProbe__(self, aCurrentLasti, aBehaviorId, aCurrentLineno):
        theProbeId = self.itsProbeId.__get__()
        self.__addProbe__(theProbeId,aCurrentLasti,aBehaviorId)
        self.itsPacker.reset()
        self.itsPacker.pack_int(self.itsEvents['register'])
        self.itsPacker.pack_int(self.itsObjects['probe'])
        self.itsPacker.pack_int(theProbeId)
        self.itsPacker.pack_int(aBehaviorId)
        self.itsPacker.pack_int(aCurrentLasti)
        self.itsPacker.pack_int(aCurrentLineno)
        if self.FLAG_DEBUGG:
            print self.itsEvents['register'],
            print self.itsObjects['probe'],
            print theProbeId,
            print aBehaviorId,
            print aCurrentLasti,            
            print aCurrentLineno
            raw_input()
        try:
            self.itsSocket.sendall(self.itsPacker.get_buffer())
        except:
            print 'TOD está durmiendo :-( - Registrando Probe'
        self.itsProbeId.__next__()
        return theProbeId
    
    def __registerThread__(self, aThreadSysId):
        theThreadId = self.itsThreadId.__get__()
        self.__addThread__(theThreadId,aThreadSysId)
        self.itsPacker.reset()
        self.itsPacker.pack_int(self.itsEvents['register'])
        self.itsPacker.pack_int(self.itsObjects['thread'])
        self.itsPacker.pack_int(theThreadId)
        self.itsPacker.pack_int(aThreadSysId)
        if self.FLAG_DEBUGG:
            print self.itsEvents['register'],
            print self.itsObjects['thread'],
            print theThreadId,
            print aThreadSysId
            raw_input()
        try:
            self.itsSocket.sendall(self.itsPacker.get_buffer())
        except:
            print 'TOD está durmiendo :-( - Registrando Thread'
        self.itsThreadId.__next__()
        return theThreadId

    def __trace__(self, aFrame, aEvent, aArg):
        if aFrame.f_back == None:
            sys.settrace(None)
            return
        theCode = aFrame.f_code
        if theCode.co_name == '<module>':
            return
        theLocals = aFrame.f_locals
        theGlobals = aFrame.f_globals
        theDepth = self.itsCurrentDepth = self.__depthFrame__(aFrame)
        self.__markTimestampFrame__(aFrame)
        theThreadId = self.__getThreadId__(thread.get_ident())
        if aEvent == "call":
            if re.search(self.itsMethodPattern,theCode.co_name):
                if not theCode.co_name == '__init__':
                    return
            theParentTimestampFrame = self.__getTimestampParentFrame__(aFrame)
            if theCode.co_name == '__init__':
                #FIXME: cambio experimental, revizar!!!!!!
                """
                id = self.itsId.__get__()
                if not hasattr(theLocals['self'],'__dict__'):
                    return
                theLocals['self'].__dict__.update({'__pyTOD__':id})
                self.itsId.__next__()
                """
                #aca se sacan las bases de la clase la cual
                #se ha instanciado
                #TODO: encontrar una mejor forma de hacerlo
                #ineficiente!!..quizas interviniendo la llamada
                #de la super clase?
                if 'self' in theLocals:
                    print type(theLocals['self']).__bases__
            #si self esta en theLocals estamos en un metodo
            if theLocals.has_key('self'):
                if not self.__inMethod__(theCode):
                    theKey = type(theLocals['self']).__name__
                    theKey = hT.__getClassKey__(theKey)
                    if theKey == None:
                        return
                    if not hT.itsClass.has_key(theKey):
                        return
                    theClassId = hT.itsClass[theKey].__getId__()
                    if not hT.itsClass[theKey].method.has_key(theCode.co_name):
                        return
                    theMethodId = hT.itsClass[theKey].method[theCode.co_name]
                    theArgs = self.__getArgs__(theCode)
                    self.__registerMethod__(theCode,theMethodId,theClassId,theArgs)
                theCurrentTimestamp = aFrame.f_locals['__timestampFrame__']
                if theCode.co_name == '__init__':
                    Id = self.itsId.__get__()
                    if not hasattr(theLocals['self'],'__dict__'):
                        return
                    theLocals['self'].__dict__.update({'__pyTOD__':Id})
                    self.itsId.__next__()
                    self.__instantiation__(theCode,
                                           aFrame,
                                           theLocals['self'].__pyTOD__,
                                           theDepth,
                                           theCurrentTimestamp,
                                           theParentTimestampFrame,
                                           theThreadId)

                else:
                    self.__methodCall__(theCode,
                                        aFrame,
                                        theLocals['self'].__pyTOD__,
                                        theDepth,
                                        theCurrentTimestamp,
                                        theParentTimestampFrame,
                                        theThreadId)
            else:
                #verificamos si es una funcion
                if theGlobals.has_key(theCode.co_name):
                    if inspect.isfunction(theGlobals[theCode.co_name]):
                        if not self.__inFunction__(theCode):
                            self.__registerFunction__(theCode)
                    theCurrentTimestamp = aFrame.f_locals['__timestampFrame__']
                    self.__functionCall__(theCode,
                                          aFrame,
                                          theDepth,
                                          theCurrentTimestamp,
                                          theParentTimestampFrame,
                                          theThreadId)   
            return self.__trace__
        elif aEvent == "line":
            if re.search(self.itsMethodPattern,theCode.co_name):
                if not theCode.co_name == '__init__':
                    return
            theParentTimestampFrame = self.__getTimestampFrame__(aFrame)
            theObject = self.__getObject__(theCode)
            if theObject == None:
                return
            theLnotab = theObject.__getLnotab__()
            if theLnotab.has_key(aFrame.f_lasti):
                theBytecodeLocals = self.__getpartcode__(theCode,theLnotab[aFrame.f_lasti])
                self.__register__(theObject,theBytecodeLocals)
                self.__localWrite__(theCode,
                                    theBytecodeLocals,
                                    theLocals,
                                    theObject,
                                    aFrame.f_lasti,
                                    aFrame.f_lineno,
                                    theDepth,
                                    theParentTimestampFrame,
                                    theThreadId)
            return self.__trace__
        elif aEvent == "return":
            if re.search(self.itsMethodPattern,theCode.co_name):
                if not theCode.co_name == '__init__':
                    return
            theParentTimestampFrame = self.__getTimestampFrame__(aFrame)
            if theLocals.has_key('__init__'):
                if not self.__inClass__(theCode):
                    #registramos la definicion de la clase
                    theLocals.update(
                            {'__setattr__':Descriptor.__dict__['__setattr__']})
                    theLocals.update({'__metaclass__':MetaDescriptor})
                    self.__registerClass__(
                                           theCode,
                                           theLocals,
                                           theParentTimestampFrame,
                                           theDepth,
                                           aFrame.f_lineno)
            else:
                theObject = self.__getObject__(theCode)
                if theObject == None:
                    return
                if self.FLAG_THROWN == True:
                    self.FLAG_THROWN = False
                    return
                theLnotab = theObject.__getLnotab__()
                if theLnotab.has_key(aFrame.f_lasti):
                    theBytecodeLocals = self.__getpartcode__(
                                                    theCode,
                                                    theLnotab[aFrame.f_lasti])
                    self. __register__(theObject,theBytecodeLocals)
                    self.__localWrite__(theCode,
                                        theBytecodeLocals,
                                        theLocals,
                                        theObject,
                                        aFrame.f_lasti,
                                        aFrame.f_lineno,
                                        theDepth,
                                        theParentTimestampFrame,
                                        theThreadId)
                self.__behaviorExit__(aFrame,
                                     aArg,
                                     theDepth,
                                     theParentTimestampFrame,
                                     theThreadId,
                                     False)
        elif aEvent == "exception":
            theParentTimestampFrame = self.__getTimestampFrame__(aFrame)
            if type(aArg[1]) is tuple:
                theArgument = aArg[1][1]
            else:
                theArgument = aArg[1]
            self.__registerException__(aFrame,
                                        theArgument,
                                        theDepth,
                                        theParentTimestampFrame,
                                        theThreadId)
            for theTuple in dis.findlinestarts(theCode):
                if aFrame.f_lineno in theTuple:
                    theIndex = theTuple[0]
            theOp = ord(theCode.co_code[theIndex-3])
            theInstruction = dis.opname[theOp]
            if theInstruction == 'SETUP_EXCEPT':
                return self.__trace__
            self.__behaviorExit__(aFrame,
                                    theArgument,
                                    theDepth,
                                    theParentTimestampFrame,
                                    theThreadId,
                                    True)
            self.FLAG_THROWN = True           

hT = hunterTrace(
                 generatorId(),
                 generatorId(),
                 generatorId(),
                 xdrlib.Packer(),
                 '127.0.0.1',
                 8058)

class MetaDescriptor(type):
    def __setattr__(self, aName, aValue):
        import sys
        theFrame = sys._getframe()
        theCode = theFrame.f_back.f_code
        theCurrentLasti = theFrame.f_back.f_lasti
        theCurrentDepth = hT.itsCurrentDepth
        theCurrentTimestamp = hT.__convertTimestamp__(time.time())
        theParentTimestamp = hT.__getTimestampParentFrame__(theFrame)
        theThreadId = hT.__getThreadId__(thread.get_ident())
        theKey = self.__name__
        theKey = hT.__getClassKey__(theKey)
        if theKey == None:
            return
        theObject = hT.itsClass[theKey]
        theObjectId = theObject.__getId__()
        theBehaviorId = hT.__getObjectId__(theCode)
        sys.settrace(None)
        theObject.__addStaticField__({aName:-1})
        Id = theObject.staticField[aName]
        if not hT.itsProbe.has_key((theCurrentLasti,theBehaviorId)):
            theProbeId = hT.__registerProbe__(theCurrentLasti,
                                              theBehaviorId,
                                              theFrame.f_lineno)
        else:
            theProbeId = hT.itsProbe[(theCurrentLasti,theBehaviorId)]          
        #preguntar si el valor está registrado y si además es string
        if type(aValue) == types.StringType:
            if not id(aValue) in hT.itsRegisterObjects:
                hT.__registerObject__(aValue, theCurrentTimestamp)
                hT.itsPacker.reset()
                hT.itsPacker.pack_int(hT.itsEvents['set'])
                hT.itsPacker.pack_int(hT.itsObjects['classAttribute'])
                hT.itsPacker.pack_int(Id)
                theDataType = hT.__getDataType__(aValue)
                hT.itsPacker.pack_int(theDataType)
                hT.itsPacker.pack_hyper(id(aValue))
                hT.itsPacker.pack_int(theProbeId)
                hT.itsPacker.pack_hyper(theParentTimestamp)        
                hT.itsPacker.pack_int(theCurrentDepth)
                hT.itsPacker.pack_hyper(theCurrentTimestamp)
                hT.itsPacker.pack_int(theThreadId)
                super(MetaDescriptor, self).__setattr__(aName, aValue)
                if hT.FLAG_DEBUGG:
                    print hT.itsEvents['set'],
                    print hT.itsObjects['classAttribute'],
                    print Id,
                    print theDataType,
                    print id(aValue),
                    print theProbeId,
                    print theParentTimestamp,
                    print theCurrentDepth,
                    print theCurrentTimestamp,
                    print theThreadId
                    raw_input()
                try:
                    hT.itsSocket.sendall(hT.itsPacker.get_buffer())
                    pass
                except:
                    print 'TOD está durmiendo :-(', theCode.co_name    
                sys.settrace(hT.__trace__)
        else:
            hT.itsPacker.reset()
            hT.itsPacker.pack_int(hT.itsEvents['set'])
            hT.itsPacker.pack_int(hT.itsObjects['classAttribute'])
            hT.itsPacker.pack_int(Id)
            theDataType = hT.__getDataType__(aValue)
            hT.itsPacker.pack_int(theDataType)
            thePackValue = hT.__packValue__(theDataType, aValue)
            hT.itsPacker.pack_int(theProbeId)
            hT.itsPacker.pack_hyper(theParentTimestamp)        
            hT.itsPacker.pack_int(theCurrentDepth)
            hT.itsPacker.pack_hyper(theCurrentTimestamp)
            hT.itsPacker.pack_int(theThreadId)
            super(MetaDescriptor, self).__setattr__(aName, aValue)
            if hT.FLAG_DEBUGG:
                print hT.itsEvents['set'],
                print hT.itsObjects['classAttribute'],
                print Id,
                print theDataType,
                print thePackValue,
                print theProbeId,
                print aParentTimestamp,
                print theCurrentDepth,
                print theCurrentTimestamp,
                print theThreadId
                raw_input()
            try:
                hT.itsSocket.sendall(hT.itsPacker.get_buffer())
            except:
                print 'TOD está durmiendo :-(', theCode.co_name    
        

class Descriptor(object):

    def __setattr__(self, aName, aValue):
        import sys
        theFrame = sys._getframe()
        theCode = theFrame.f_back.f_code
        theCurrentLasti = theFrame.f_back.f_lasti
        theCurrentDepth = hT.__getDepthFrame__(theFrame.f_back) + 1
        theCurrentTimestamp = hT.__convertTimestamp__(time.time()) 
        theParentTimestamp = hT.__getTimestampParentFrame__(theFrame)
        theThreadId = hT.__getThreadId__(thread.get_ident())
        theKey = type(self).__name__
        theKey = hT.__getClassKey__(theKey)
        if theKey == None:
            return
        theObject = hT.itsClass[theKey] 
        theObjectId = theObject.__getId__()
        theBehaviorId = hT.__getObjectId__(theCode)
        sys.settrace(None)
        theObject.__addAttribute__(aName, theObjectId)
        Id = theObject.attributes[aName]
        if not hT.itsProbe.has_key((theCurrentLasti,theBehaviorId)):
            theProbeId = hT.__registerProbe__(theCurrentLasti,
                                              theBehaviorId,
                                              theFrame.f_lineno)
        else:
            theProbeId = hT.itsProbe[(theCurrentLasti,theBehaviorId)]          
        if type(aValue) == types.StringType:
            if not id(aValue) in hT.itsRegisterObjects:
                hT.__registerObject__(aValue, theCurrentTimestamp)
                hT.itsPacker.pack_int(hT.itsEvents['set'])
                hT.itsPacker.pack_int(hT.itsObjects['attribute'])
                hT.itsPacker.pack_int(Id)
                try:
                    hT.itsPacker.pack_int(self.__pyTOD__)
                except:
                    object.__setattr__(self, aName, aValue)
                    return
                theDataType = hT.__getDataType__(aValue)
                hT.itsPacker.pack_int(theDataType)
                hT.itsPacker.pack_hyper(id(aValue))
                hT.itsPacker.pack_int(theProbeId)
                hT.itsPacker.pack_hyper(theParentTimestamp)        
                hT.itsPacker.pack_int(theCurrentDepth)
                hT.itsPacker.pack_hyper(theCurrentTimestamp)
                hT.itsPacker.pack_int(theThreadId)
                object.__setattr__(self, aName, aValue)
                if hT.FLAG_DEBUGG:
                    print hT.itsEvents['set'],
                    print hT.itsObjects['attribute'],
                    print Id,
                    print theBehaviorId,
                    print theDataType,
                    print id(aValue),
                    print theProbeId,
                    print theParentTimestamp,
                    print theCurrentDepth,
                    print theCurrentTimestamp,
                    print theThreadId
                    raw_input()
                try:
                    hT.itsSocket.sendall(hT.itsPacker.get_buffer())
                except:
                    print 'TOD está durmiendo :-(', theCode.co_name   
                sys.settrace(hT.__trace__)
        else:
            hT.itsPacker.reset()
            hT.itsPacker.pack_int(hT.itsEvents['set'])
            hT.itsPacker.pack_int(hT.itsObjects['attribute'])
            hT.itsPacker.pack_int(Id)
            try:
                hT.itsPacker.pack_int(self.__pyTOD__)
            except:
                object.__setattr__(self, aName, aValue)
                return
            theDataType = hT.__getDataType__(aValue)
            hT.itsPacker.pack_int(theDataType)
            thePackValue = hT.__packValue__(theDataType, aValue)
            hT.itsPacker.pack_int(theProbeId)
            hT.itsPacker.pack_hyper(theParentTimestamp)        
            hT.itsPacker.pack_int(theCurrentDepth)
            hT.itsPacker.pack_hyper(theCurrentTimestamp)
            hT.itsPacker.pack_int(theThreadId)
            object.__setattr__(self, aName, aValue)
            if hT.FLAG_DEBUGG:
                print hT.itsEvents['set'],
                print hT.itsObjects['attribute'],
                print Id,
                print theBehaviorId,
                print theDataType,
                print thePackValue,
                print theProbeId,
                print theParentTimestamp,
                print theCurrentDepth,
                print theCurrentTimestamp,
                print theThreadId
                raw_input()
            try:
                hT.itsSocket.sendall(hT.itsPacker.get_buffer())
            except:
                print 'TOD está durmiendo :-(', theCode.co_name   
            sys.settrace(hT.__trace__)


if th:
    settrace(hT.__trace__)  
sys.settrace(hT.__trace__)

