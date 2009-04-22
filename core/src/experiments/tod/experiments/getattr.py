#! /usr/bin/python

class MyClass:
	i = 1
	def __getattr__(self, name):
		print "Getting: "+name
		return 2

c = MyClass()
print c.i
print c.j

from tod.experiments import JythonTest
JythonTest.foo()