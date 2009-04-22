


# For LinkedList

result = "["
h = o.header 
i = 0
current = h.next 
while current != h and current != None and i < 5:
   i += 1

   result += str(current.element)
   result += ", "

   current = current.next

if current != h and current != None:
   result += "..."

result += "]"
return result

# For LinkedList, but returning a list
l = ArrayList()
h = o.header 
current = h.next 
while current != h and current != None:
   el = current.element
   if isinstance(el, TODObject):
      el = el.robj
   l.add(el)
   current = current.next

return l
