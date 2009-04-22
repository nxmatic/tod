#! /usr/bin/python
# -*- coding: utf-8 -*-

__author__ = "Milton Inostroza Aguilera"
__email__ = "minoztro@gmail.com"
__all__ = ['events','objects', 'dataTypes','packXDRLib']


events = {
          'register':0,
          'call':1,
          'set':2,
          'return':3,
          'instantiation':4
          }

objects = {
           'class':0,
           'method':1,
           'attribute':2,
           'function':3,
           'local':4,
           'probe':5,
           'thread':6,
           'classAttribute':7,
           'object':8,
           'exception':9,
           'specialMethod':10
           }

dataTypes = {
             int.__name__:0,
             str.__name__:1,
             float.__name__:2,
             long.__name__:3,
             bool.__name__:4,
             tuple.__name__:5,
             list.__name__:6,
             dict.__name__:7
             #other type: 8
             }

packXDRLib = {
              0:'int',
              1:'string',
              2:'float',
              3:'double',
              4:'int', #4:'bool'
              }