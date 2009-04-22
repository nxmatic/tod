import sys
from debugger.pytod.core.hunterTrace import hT
import time


class Sort(object):
    
    def __init__(self, aArreglo=None):
        if aArreglo is None:
            aArreglo = []
        self.aArreglo = aArreglo
    
    def burble(self):
        for i in range(len(self.aArreglo)):
            pass
            for j in range(i, len(self.aArreglo)):
                if self.aArreglo[i] > self.aArreglo[j]:
                    theAux = self.aArreglo[i]
                    self.aArreglo[i] = self.aArreglo[j]
                    self.aArreglo[j] = theAux
            pass
    
    def show(self):
        print self.aArreglo

if __name__ == '__main__':
    first = time.time()
    theArreglo = list(range(5000))
    theSort = Sort(theArreglo)
    theSort.burble()
    #theSort.show() 
    second = time.time()
    print second-first