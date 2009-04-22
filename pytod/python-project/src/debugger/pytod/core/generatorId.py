#! /usr/bin/python
# -*- coding: utf-8 -*-

__author__ = "Milton Inostroza Aguilera"
__email__ = "minoztro@gmail.com"
__all__ = ['generatorId']

class generatorId(object):
    
    def __init__(self):
        self.Id = 101

    def __get__(self):
        return self.Id

    def __next__(self):
        self.Id = self.Id + 1
        return 