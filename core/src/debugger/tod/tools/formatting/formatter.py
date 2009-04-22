from tod.tools.formatting import IPyObjectFormatter, IPyFormatterFactory;

class TODObject:
	"A reconstituted TOD object"
	robj = None
	
	def __init__(self, robj):
		self.robj = robj

	def __str__(self):
		return self.robj.format()
	
	def __coerce__(self, other):
		return None
	
	def __radd__(self, other):
		return other+str(self)
	
	def __eq__(self, other):
		if isinstance(other, TODObject):
			return self.robj == other.robj
		else:
			return self is other
	
	def __ne__(self, other):
		return not (self == other) 
	
	def __getattr__(self, name):
		#print "get: "+name
		return self.robj.get(name)
		
	def __mod__(self, other):
		print "mod"
		return self	  
	
class PyObjectFormatter(IPyObjectFormatter):
	
	"A function that takes a TODObject and returns a representation of that object (string or otherwise)"
	func = None
	
	def __init__(self, func):
		self.func = func
	
	def format(self, robj):
		o = TODObject(robj)
		return self.func(o)
	
class PyFormatterFactory(IPyFormatterFactory):
	def createFormatter(self, func):
		return PyObjectFormatter(func)
	
	def createTODObject(self, o):
		return TODObject(o)
	
factory = PyFormatterFactory()
