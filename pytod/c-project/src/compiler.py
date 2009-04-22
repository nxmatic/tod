#! /usr/bin/python
# -*- coding: utf-8 -*-

from distutils.core import setup, Extension

if __name__ == __name__:
    import sys
    if len(sys.argv) > 1:
        nombre = sys.argv.pop(1)
        modulo = Extension(nombre,sources = ['%s.c'%nombre])
        setup (
            name=nombre,
            version = '1.0', 
            description='descripcion de %s'%nombre,
            ext_modules=[modulo]
        )

