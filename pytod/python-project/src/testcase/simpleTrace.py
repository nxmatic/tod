#! /usr/bin/python
# -*- coding: utf-8 -*-

import sys
import dis

"""
    def trace(frame, event, arg):
        print event
        code = frame.f_code
        if event == 'exception':
            fBack = frame.f_back
            if frame.f_code.co_name == '<module>':
                print 'para la cuestioncita'
                #sys.settrace(None)
            print fBack, fBack.f_code.co_name, code.co_name
            raw_input()
            for theIndex in dis.findlinestarts(code):
                if frame.f_lineno in theIndex:
                    theValue = theIndex[0]
            print frame.f_lineno, frame.f_lasti
            print dis.opname[ord(code.co_code[theValue-3])]
            dis.disassemble(frame.f_code,frame.f_lasti)
            #print event, code.co_name
            #print arg[0],arg[1],arg[2]
            raw_input()
            return None
        if event == 'call':
            #print code.co_name
            #raw_input()
            return trace
        elif event == 'line':
            #print code.co_name
            return trace
        elif event == 'return':
            print code.co_name
            #print arg
            #raw_input()
            pass
    
    class Dict(dict):
        
        def __setitem__(self, aKey, aValue):
            print 'estas asignanto',aValue,'a la variable',aKey
            dict.__setitem__(self,aKey,aValue)
    
    def trace(aFrame, aEvent, aArg):
        dis.disassemble(aFrame.f_code)
        if aEvent == 'call':
            print aFrame.f_code.co_name, aEvent
            #print aFrame.f_locals
            #raw_input()
            if aFrame.f_code.co_name == '__init__':
                aFrame.f_locals['self'].__dict__ = Dict()
                type(aFrame.f_locals['self'].__dict__)
                raw_input()
            return trace
        elif aEvent == 'line':
            print aFrame.f_code.co_name, aEvent
            #print aFrame.f_locals
            if aFrame.f_locals.has_key('self'):
                print aFrame.f_locals['self'].__dict__
            #raw_input()
            return trace
        elif aEvent == 'return':
            print aFrame.f_code.co_name, aEvent
            print aFrame.f_locals
            #print aFrame.f_globals
            #raw_input()
        
    #sys.settrace(trace)
    
    def m0():
        m1()
    
    def m1():
        m2()
    
    def m2():
        try:
            m3()
        except:
            print 'hola'
            
    def m2():
        m3()
    
    def m3():
        y = 1/0
        
    m0()
    
    class algo(object):
        a = 0
    
        def __init__(self):
            self.b = 52
            self.z =10
            print self.a
        
        def imprimir(self):
            print self.b, self.z
            
        def __setattr__(self, aName, aValue):
            print aName, aValue
            object.__setattr__(self, aName, aValue)
       
    b = algo()
    algo.imprimir()
    b.e=52
    print b.b
    
    
    class MyDict(dict):
        def __setitem__(self, aKey, aValue):
            print 'asignando'
            dict.__setitem__(self,aKey,aValue)
            
        def __update__(self, aDictionary):
            print 'a'
    
    dicc = MyDict()
    dicc.update({'hola': lambda self: 'hola metamundo!'})
    Saludo = type('Saludo', (), dicc)
    s = Saludo()
    s.hola()

    
    def __depthFrame__(aFrame):
        theBackFrame = aFrame.f_back
        if theBackFrame.f_locals.has_key('__depth__'):
            theCurrentDepth = theBackFrame.f_locals['__depth__']
            aFrame.f_locals['__depth__'] = theCurrentDepth + 1
        else:
            aFrame.f_locals['__depth__'] = 1
            print 'frame inicial',aFrame.f_code.co_name,
        return aFrame.f_locals['__depth__']

    def trace(aFrame, aEvent, aArg):
        theCode = aFrame.f_code
        if theCode.co_name == '<module>':
                return
        theDepth = __depthFrame__(aFrame)
        if aEvent == 'call':
            print theCode.co_name, theDepth
        elif aEvent == 'line':
            pass
        elif aEvent == 'return':
            pass
        elif aEvent == 'exception':
            pass
"""
def trace(aFrame, aEvent, aArg):
    theCode = aFrame.f_code
    if aEvent == 'call':
        print len(dis.findlinestarts(theCode))
        for theTuple in dis.findlinestarts(theCode):
            print theTuple
        print dis.dis(theCode)
        print len(theCode.co_code)
        raw_input()
    elif aEvent == 'line':
        pass
    elif aEvent == 'return':
        pass
    elif aEvent == 'exception':
        pass
    
sys.settrace(trace)


class Test(object):
    def __init__(self):
        self.a = 10
        self.metodo()
    
    def metodo(self):
        self.a = self.a +1

if __name__ == '__main__':
    test = Test()
    test.metodo()
    
