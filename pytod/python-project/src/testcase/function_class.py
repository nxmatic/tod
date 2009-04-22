#! /usr/bin/python
# -*- coding: utf-8 -*-

from debugger.pytod.core.hunterTrace import *


def prueba():
    x = 10
    a = 0
    x = 15
    y = x = a = 1

def prueba2(e=3):
    x = 10
    a = 0
    x = 15
    y = x = a = 1

class clasePrueba(object):

    def __init__(self):
        x = 1
        self.w = 20
        self. h = 50
        x = self.w

    def impresion(self):
        print 'hola'

    def asignacion(self):
        j = 4

#prueba()
#prueba2()
objeto = clasePrueba()
objeto.asignacion()