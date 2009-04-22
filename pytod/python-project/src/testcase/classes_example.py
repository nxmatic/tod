#! /usr/bin/python
# -*- coding: utf-8 -*-
import sys
#sys.path.append('/media/WD Passport/eclipse/workspace/python-project/src')
from debugger.pytod.core.hunterTrace import hT
import time
first = time.time()

class clase1(object):
    h = 2
    def __init__(self, y):
        self.__x = []
        self.__x.append(1)
        self.c = True
        self.z = 1
        self.metodo(self.z, 1, 2, 3)
        return
    
    def metodo(self, h, i, j, k):
        self.casa = 1 + h
        h = 0
        print self.z
        k = i + j
        self.x = k
        try:
            o = self.foo()
        except:
            o = 1
        #self.foo()
        return k
    
    def foo(self):
        y = 1/0

class Corriente(object):
    "Clase Corriente. Nota: Llamar con from pinchpython import *"
    __num_corrientes = -1
    __corrientes_definidas = []
    __tipos_corrientes = ('Caliente','Fr￯﾿ﾽa','Indeterminado')
    def __init__(self):
        self.__class__.__num_corrientes += 1
        self.__MCP = 0
        self.__T0 = 0
        self.__Tf = 0
        self.__h = 0
        self.__tipo = self.__tipos_corrientes[2]
        #self.__class__.__corrientes_definidas.append(self)
        #self.__id_num = self.__corrientes_definidas.index(self)
        # N￯﾿ﾽmero de unidades de intercambio de calor necesarias
        # para esta corriente
        self.__N_sobre_pinch = 0.0
        self.__N_bajo_pinch = 0.0
        self.__phi = 0
        self.__marcada = False
        self.__dividida = False
        self.__vectorAncestros = []
        self.__vectorAncestros.append(2)
"""
class clase2(object):
    def __init__(self):
        y = 1
   
        
class clase3(object):
    def __init__(self):
        y = 1
    def metodo(self):
        v=6
"""
if __name__ == '__main__':
    a = clase1(1)
    a.metodo(10,20,30,40)
    c = Corriente()
    Corriente.__num_corrientes = 36
    finish = time.time()
    print finish-first
    #b = clase2()
    #c = clase3()
    #hT.__printHunter__()