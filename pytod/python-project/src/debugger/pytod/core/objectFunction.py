#! /usr/bin/python
# -*- coding: utf-8 -*-

__author__ = "Milton Inostroza Aguilera"
__email__ = "minoztro@gmail.com"
__all__ = ['Function']

from Dictionary import Dictionary

class Function(object):

    def __init__(self, aHt, aId, aCode, aLnotab, aArgs):
        self.hT = aHt
        self.locals = Dictionary(self.hT)
        self.argument = ()
        self.lnotab = aLnotab
        self.code = aCode
        self.name = aCode.co_name
        self.Id = aId
        self.__updateArgument__(aArgs)

    def __getId__(self):
        return self.Id

    def __getLnotab__(self):
        return self.lnotab

    def __getLocals__(self):
        return self.locals

    def __getArgs__(self):
        return self.argument
    
    def __getArgsValues__(self, aLocals):
        argValues = ()
        for name in self.argument:
            if aLocals.has_key(name):
                argValues = argValues + (aLocals[name],)
        #TODO: analizar caso para cuando sean tuple, list, dict
        return argValues

    def __updateArgument__(self, aArgs):
        #no se registra self como argumento valido
        for theArg in aArgs:
            if not theArg == 'self':
                self.argument = self.argument + (theArg,)
        theParentId = self.Id
        if self.hT.FLAG_DEBUGG:
            for theIndex in range(len(aArgs)):            
                    print self.hT.itsEvents['register'],
                    print self.hT.itsObjects['local'],          
                    print theIndex,
                    print theParentId,
                    print aArgs[theIndex]
                    raw_input()             
            
    def __registerLocals__(self, aLocal):
        self.locals.__update__(aLocal,self.Id,self.argument)